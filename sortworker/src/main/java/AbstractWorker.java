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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractWorker implements IWorker {
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
        mongoClient = MongoClients.create(new ConnectionString("mongodb://localhost"));
        db = mongoClient.getDatabase("TextDocumentsDB");
        // Create a gridFSBucket with a custom bucket name "files"
        gridFSBucket = GridFSBuckets.create(db);

        //RabbitMQ inits
        factory = new ConnectionFactory();
        factory.setHost("localhost");
//        factory.setPort(MQPort);
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QueueName, false, false, false, null);
        channel.basicQos(1); // accept only one unack-ed message at a time (see below)
    }


    public File getFileFromMyMongo(String fileId) throws IOException {
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
