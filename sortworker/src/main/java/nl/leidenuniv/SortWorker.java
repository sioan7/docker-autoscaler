package nl.leidenuniv;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONObject;

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

    public final Channel reduceChannel;
    private final String REDUCE_WORKER_QUEUE = "ReduceWorkerMQ";

    public SortWorker() throws IOException, TimeoutException {
        super("SortWorkerMQ", "nl.leidenuniv.SortWorker", 5672);
        reduceChannel = connection.createChannel();
        reduceChannel.queueDeclare(REDUCE_WORKER_QUEUE, false, false, false, null);
        reduceChannel.basicQos(1); // accept only one unack-ed message at a time (see below)
    }

    @Override
    public void handleTasks() {
        System.out.println("Started service --> " + ServiceName);
        System.out.println("Waiting for tasks out of the queue");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(message);
                String fileId = (String) jsonObject.get("FileID");
                String filename = (String) jsonObject.get("FileName");
                Integer chunk = (Integer) jsonObject.get("Chunk");
                System.out.println("Received chunk " + chunk + " from " + fileId);
                byte[] data = ((Binary) db.getCollection("fs.chunks")
                        .find(and(eq("files_id", new ObjectId(fileId)), eq("n", chunk)))
                        .first()
                        .get("data")).getData();
                storeSortedChunk(sortChunk(data), fileId, chunk);
                int totalChunks = db.getCollection("fs.chunks")
                        .find(eq("files_id", new ObjectId(fileId)))
                        .into(new ArrayList<>()).size();
                int processedChunks = db.getCollection("sorted.chunks")
                        .find(eq("file_id", fileId))
                        .into(new ArrayList<>()).size();
                if (processedChunks == totalChunks) {
                    sendMessageToReducer(fileId, filename);
                    gridFSBucket.delete(new ObjectId(fileId));
                }
                System.out.println("Chunk is sorted and stored in MongoDB");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        //autoAck off, thus we have to make the basicAck by ourselves, see above.
        try {
            channel.basicConsume(QueueName, false, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToReducer(String fileId, String filename) throws IOException {
        String message = "{\n" +
                "  \"FileID\": \"" + fileId + "\",\n" +
                "  \"FileName\": \"" + filename + "\"\n" +
                "}";
        reduceChannel.basicPublish("", REDUCE_WORKER_QUEUE, null, message.getBytes(StandardCharsets.UTF_8));
    }

    private void storeSortedChunk(byte[] sortOutput, String fileId, Integer chunk) {
        db.getCollection("sorted.chunks").insertOne(new Document()
                .append("file_id", fileId)
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
