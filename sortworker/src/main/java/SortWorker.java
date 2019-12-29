import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.rabbitmq.client.DeliverCallback;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class SortWorker extends AbstractWorker {

    public SortWorker() throws IOException, TimeoutException {
        super("SortWorkerMQ", "SortWorker", 5672);
    }

    @Override
    public void handleTasks() {
        System.out.println("Started service --> " + ServiceName);
        System.out.println("Waiting for tasks out of the queue");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(message);
            String fileId = (String) jsonObject.get("FileID");
            Integer chunk = (Integer) jsonObject.get("Chunk");
            System.out.println("Received chunk " + chunk + " from " + fileId);
            byte[] data = (byte[]) db.getCollection("fs.chunks")
                    .find(and(eq("files_id", fileId), eq("n", chunk)))
                    .first()
                    .get("data");
            storeSortedChunk(sortChunk(data), fileId, chunk);
            int totalChunks = db.getCollection("fs.chunks")
                    .find(eq("files_id", fileId))
                    .into(new ArrayList<>()).size();
            if (chunk == totalChunks - 1) {
                sendMessageToReducer(fileId);
            }
            System.out.println("Chunk is sorted and stored in the MongoDB");
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

    private void sendMessageToReducer(String fileId) throws IOException {
    	String queueNamePublish = "ReduceWorkerMQ";
    	channel = connection.createChannel();
        channel.queueDeclare(queueNamePublish, false, false, false, null);
        channel.basicQos(1); // accept only one unack-ed message at a time (see below)
        String message = "{\n" + 
        		"  \"FileID\": \"" + fileId + "\",\n" +
        		"}";
        channel.basicPublish("", queueNamePublish, null, message.getBytes(StandardCharsets.UTF_8));
    }

    private void storeSortedChunk(byte[] sortOutput, String fileId, Integer chunk) {
        db.getCollection("numbers").insertOne(new Document()
                .append("fileId", fileId)
                .append("chunk", chunk)
                .append("data", sortOutput)
        );

//    	GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(255 * 1024).metadata(new Document("type", "text"));
//    	GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(fileNameSorted, options);
//    	uploadStream.write(sortOutput);
//    	uploadStream.close();
//    	ObjectId fileId = uploadStream.getFileId();
    	
        System.out.println("Stored fileID " + fileId);
    }

    public byte[] sortChunk(byte[] data) {
		String outputString = "";
        Pattern p = Pattern.compile("-?\\d+");
        String[] split = new String(data, StandardCharsets.UTF_8).split("\n");
        for (String line : split) {
            List<Integer> numbers = new ArrayList<>();
            Matcher m = p.matcher(line);
            while (m.find()) {
                String s = m.group();
                numbers.add(Integer.parseInt(s));
            }
            if(numbers.isEmpty())
                continue;
            Collections.sort(numbers);
            for(int number : numbers)
            {
                outputString = outputString + number + " ";
            }
            outputString = outputString + "\n";
        }
        return outputString.getBytes(StandardCharsets.UTF_8);
    }
}
