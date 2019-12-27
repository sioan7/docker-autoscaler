import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.rabbitmq.client.DeliverCallback;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.*;
import java.util.concurrent.TimeoutException;

public class SortWorker extends AbstractWorker {

    public SortWorker() throws IOException, TimeoutException {
        super("SortWorkerMQ", "SortWorker", 5672);
    }

    @Override
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
            ObjectId newFileID = saveOutputFile(sortInputFile(file));
            sendMessageToReducer();
            System.out.println("File is sorted and stored in the MongoDB");
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

    private void sendMessageToReducer() {

    }

    private ObjectId saveOutputFile(File sortOutputFile) {
        ObjectId fileId = null;
        try {
            InputStream streamToUploadFrom = new FileInputStream(sortOutputFile);
            // Create some custom options
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(358400)
                    .metadata(new Document("type", "text"));

            fileId = gridFSBucket.uploadFromStream(sortOutputFile.getName(), streamToUploadFrom, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Stored fileID " + fileId);
        return fileId;
    }

    public File sortInputFile(File textFile){
        //TODO: implement stuff
        return new File();
    }
}
