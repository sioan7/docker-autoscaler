import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class SortWorkerTest {

    private SortWorker sortworker;
    private File test1;
    private String testresultPath;

    @Before
    public void setUpTests() throws IOException, TimeoutException {
        sortworker = new SortWorker();
        test1 = new File("src/test/resources/test1.txt");
        testresultPath = "src/test/resources/test1result.txt";
    }

    @Test
    public void sortInputFile() {
    	byte[] output = sortworker.sortInputFile(test1);
    	String outputString = new String(output);
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get(testresultPath), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        String testresultString = contentBuilder.toString();
        assertEquals("It should look different", testresultString, outputString);
    }

    /**
     * MongoDB Container needs to run
     */
    @Test
    public void testMongoDBConnection() {
        ObjectId fileId = null;
        try {
            InputStream streamToUploadFrom = new FileInputStream(test1);
            // Create some custom options
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(358400)
                    .metadata(new Document("type", "text"));

            fileId = sortworker.gridFSBucket.uploadFromStream("text1", streamToUploadFrom, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Stored fileID " + fileId);

        GridFSDownloadStream downloadStream = sortworker.gridFSBucket.openDownloadStream(fileId);
        int fileLength = (int) downloadStream.getGridFSFile().getLength();
        byte[] bytesToWriteTo = new byte[fileLength];
        downloadStream.read(bytesToWriteTo);
        downloadStream.close();

        System.out.println(new String(bytesToWriteTo, StandardCharsets.UTF_8));

        sortworker.gridFSBucket.delete(fileId);
    }

    @Test
    public void testRabbitMQConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        String QueueName = "SortWorkerMQ";
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(QueueName, false, false, false, null);
            channel.basicQos(1);
            String message = "Hello World!";
            channel.basicPublish("", QueueName, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "'");
        }

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QueueName, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
            try {
                System.out.println("Do something...");
                //something strange is not working here
                assertEquals("It is expected to return Hello World!", "HelWorld!", message);
            } finally {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        boolean autoAck = false;
        String result = channel.basicConsume(QueueName, autoAck, deliverCallback, consumerTag -> {
        });
        Thread.sleep(2000);
        System.out.println(result);
    }
}
