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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class ReduceWorkerTest {

    private ReduceWorker reduceworker;
    private File test1;
    private File test2;

    @Before
    public void setUpTests() throws IOException, TimeoutException {
        reduceworker = new ReduceWorker();
        test1 = new File("src/test/resources/test1result.txt");
        test2 = new File("src/test/resources/SortedNumbers.txt");
    }

    @Test
    public void testReduceLists2(){
        List<List<Integer>> input = reduceworker.getInputForReduceLists(test1);
        /**StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get("src/test/resources/test1result.txt"), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        String testresultString = contentBuilder.toString();*/
        String result = reduceworker.reduceLists(input);
        System.out.println(result);
        assertEquals("The result should look different", "-5 1 2 3 22 34 ", result);
    }

    @Test
    public void testReduceLists(){
        List<List<Integer>> input = new ArrayList<>();
        try {
            FileReader fr = new FileReader(test2);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            String line;
            while ((line = br.readLine()) != null) {
                Scanner scanner = new Scanner(line);
                List<Integer> list = new ArrayList<Integer>();
                while (scanner.hasNextInt()) {
                    list.add(scanner.nextInt());
                }
                input.add(list);
            }
            fr.close();    //closes the stream and release the resources
        } catch (IOException e) {
            e.printStackTrace();
        }
        String result = reduceworker.reduceLists(input);
        System.out.println(result);
        assertEquals("The result should look like ", "1 5 8 15 30 42 56 56 84 108 150 320 965 1420 2000 2001 ", result);
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

            fileId = reduceworker.gridFSBucket.uploadFromStream("text1", streamToUploadFrom, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Stored fileID " + fileId);

        GridFSDownloadStream downloadStream = reduceworker.gridFSBucket.openDownloadStream(fileId);
        int fileLength = (int) downloadStream.getGridFSFile().getLength();
        byte[] bytesToWriteTo = new byte[fileLength];
        downloadStream.read(bytesToWriteTo);
        downloadStream.close();

        System.out.println(new String(bytesToWriteTo, StandardCharsets.UTF_8));

        reduceworker.gridFSBucket.delete(fileId);
    }

    @Test
    public void testRabbitMQConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        String QueueName = "ReduceWorkerMQ";
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
