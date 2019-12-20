import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class numberworker {

    //Static variables
    private final String QueueName = "NumberWorkerMQ";
    private final String ServiceName = "NumberWorker";

    //MongoDB Attributes
    private MongoDatabase db;
    private MongoClient mongoClient;
    GridFSBucket gridFSBucket;

    //RabbitMQ Attributes
    ConnectionFactory factory;
    Connection connection;
    Channel channel;
    //String QueueName;

    numberworker() throws IOException, TimeoutException {
        //MongoDB inits
        mongoClient = MongoClients.create(new ConnectionString("mongodb://0.0.0.0:27017"));
        db = mongoClient.getDatabase("TextDocumentsDB");
        // Create a gridFSBucket with a custom bucket name "files"
        gridFSBucket = GridFSBuckets.create(db, "files");

        //RabbitMQ inits
        factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QueueName, false, false, false, null);
        channel.basicQos(1); // accept only one unack-ed message at a time (see below)
    }

    public void handleTasks() {
        System.out.println("Started service --> " + ServiceName);
        System.out.println("Waiting for tasks out of the queue");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            JSONObject jsonObject = new JSONObject(message);
            String fileID = (String) jsonObject.get("FileID");
            String fileName = (String) jsonObject.get("FileName");
            System.out.println("Received file name " + fileName);
            File file = getFileFromMyMongo(fileID);
            int amountOfNumbers = findNumbers(file);
            System.out.println(amountOfNumbers + " numbers are in the file " + fileName);
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        //autoAck off, thus we have to make the basicAck by ourselves, see above.
        try {
            channel.basicConsume(QueueName, false, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method gets a file and reads all Integer out of it.
     * It will return the amount of Integer, but it would also be possible to return the numbers.
     *
     * @param textFile : java.io.File
     */
    int findNumbers(File textFile) {
        List numbers = new ArrayList();
        int numberCounter = 0;
        Scanner input = null;
        Pattern p = Pattern.compile("-?\\d+");
        try {
            FileReader fr = new FileReader(textFile);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                while (m.find()) {
                    String s = m.group();
                    numbers.add(Integer.parseInt(s));
                    numberCounter++;
                }
            }
            fr.close();    //closes the stream and release the resources
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numberCounter;
    }

    private File getFileFromMyMongo(String fileId) throws IOException {
        GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(fileId);
        int fileLength = (int) downloadStream.getGridFSFile().getLength();
        byte[] bytesToWriteTo = new byte[fileLength];
        downloadStream.read(bytesToWriteTo);
        downloadStream.close();

        File tempFile = File.createTempFile("Temporary", ".txt", null);
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(bytesToWriteTo);

        return tempFile;
    }
}
