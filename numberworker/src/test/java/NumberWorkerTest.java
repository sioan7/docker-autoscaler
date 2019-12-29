//import com.mongodb.client.gridfs.GridFSDownloadStream;
//import com.mongodb.client.gridfs.model.GridFSUploadOptions;
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;
//import com.rabbitmq.client.DeliverCallback;
//import nl.leidenuniv.NumberWorker;
//import org.bson.Document;
//import org.bson.types.ObjectId;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.util.concurrent.TimeoutException;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//public class NumberWorkerTest {
//
//    private NumberWorker numberworker;
//    private File test1;
//
//    @Before
//    public void setUpTests() throws IOException, TimeoutException {
//        numberworker = new NumberWorker();
//        test1 = new File("src/test/resources/test1.txt");
//    }
//
//    @Test
//    public void testFindNumbers() {
//        assertTrue("File is not accesible!", test1.canRead());
//        assertEquals("The number of number occurences is 6.", 6, numberworker.findNumbers(test1));
//    }
//
//    /**
//     * MongoDB Container needs to run
//     */
//    @Test
//    public void testMongoDBConnection() {
//        ObjectId fileId = null;
//        try {
//            InputStream streamToUploadFrom = new FileInputStream(test1);
//            // Create some custom options
//            GridFSUploadOptions options = new GridFSUploadOptions()
//                    .chunkSizeBytes(358400)
//                    .metadata(new Document("type", "text"));
//
//            fileId = numberworker.gridFSBucket.uploadFromStream("text1", streamToUploadFrom, options);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Stored fileID " + fileId);
//
//        GridFSDownloadStream downloadStream = numberworker.gridFSBucket.openDownloadStream(fileId);
//        int fileLength = (int) downloadStream.getGridFSFile().getLength();
//        byte[] bytesToWriteTo = new byte[fileLength];
//        downloadStream.read(bytesToWriteTo);
//        downloadStream.close();
//
//        System.out.println(new String(bytesToWriteTo, StandardCharsets.UTF_8));
//
//        numberworker.gridFSBucket.delete(fileId);
//    }
//
//    @Test
//    public void testRabbitMQConnection() throws Exception {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
//        factory.setPort(5672);
//        String QueueName = "NumberWorkerMQ";
//        try (Connection connection = factory.newConnection();
//             Channel channel = connection.createChannel()) {
//            channel.queueDeclare(QueueName, false, false, false, null);
//            channel.basicQos(1);
//            String message = "Hello World!";
//            channel.basicPublish("", QueueName, null, message.getBytes(StandardCharsets.UTF_8));
//            System.out.println(" [x] Sent '" + message + "'");
//        }
//
//        Connection connection = factory.newConnection();
//        Channel channel = connection.createChannel();
//
//        channel.queueDeclare(QueueName, false, false, false, null);
//        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
//
//        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//            String message = new String(delivery.getBody(), "UTF-8");
//            System.out.println(" [x] Received '" + message + "'");
//            try {
//                System.out.println("Do something...");
//                //something strange is not working here
//                assertEquals("It is expected to return Hello World!", "HelWorld!", message);
//            } finally {
//                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
//            }
//        };
//        boolean autoAck = false;
//        String result = channel.basicConsume(QueueName, autoAck, deliverCallback, consumerTag -> {
//        });
//        Thread.sleep(2000);
//        System.out.println(result);
//    }
//}
