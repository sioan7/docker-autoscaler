package nl.leidenuniv;

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
import org.bson.types.ObjectId;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractWorker implements IWorker {
//    public static final String MONGO_DB_HOST = "localhost";
//    public static final String RABBIT_MQ_HOST = "localhost";
    public static final String MONGO_DB_HOST = "mymongo";
    public static final String RABBIT_MQ_HOST = "myrabbit";

    String QueueName;
    String ServiceName;

    //MongoDB Attributes
    MongoDatabase db;
    MongoClient mongoClient;

    GridFSBucket gridFSBucket;

    //RabbitMQ Attributes
    ConnectionFactory factory;
    Connection connection;
    Channel channel;


    public AbstractWorker(String queueName, String serviceName, int MQPort) throws IOException, TimeoutException {
        QueueName = queueName;
        ServiceName = serviceName;

        //MongoDB inits
        mongoClient = MongoClients.create(new ConnectionString("mongodb://" + MONGO_DB_HOST));
        db = mongoClient.getDatabase("TextDocumentsDB");
        // Create a gridFSBucket with a custom bucket name "files"
        gridFSBucket = GridFSBuckets.create(db);

        //RabbitMQ inits
        factory = new ConnectionFactory();
        factory.setHost(RABBIT_MQ_HOST);
//        factory.setPort(MQPort);
        while (connection == null) {
            try {
                connection = factory.newConnection();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        channel = connection.createChannel();
        channel.queueDeclare(QueueName, false, false, false, null);
        channel.basicQos(1); // accept only one unack-ed message at a time (see below)
    }


    public byte[] getFileFromMyMongo(String fileId) throws IOException {
        GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(new ObjectId(fileId));
        int fileLength = (int) downloadStream.getGridFSFile().getLength();
        byte[] bytesToWriteTo = new byte[fileLength];
        downloadStream.read(bytesToWriteTo);
        downloadStream.close();

//        File tempFile = File.createTempFile("Temporary", ".txt", null);
//        FileOutputStream fos = new FileOutputStream(tempFile);
//        fos.write(bytesToWriteTo);

        return bytesToWriteTo;
    }
}
