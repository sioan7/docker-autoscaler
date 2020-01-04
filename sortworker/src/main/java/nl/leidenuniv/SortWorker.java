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
import java.util.Arrays;
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
        super("SortWorkerMQ", "SortWorker", 5672);
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
                int totalChunks = fetchTotalNumberOfChunks(fileId);
                String[] data = assembleLinesForCurrentChunk(fileId, chunk, totalChunks);
                storeSortedChunk(sortChunk(data), fileId, chunk);
                if (getNumberOfProcessedChunks(fileId) == totalChunks) {
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

    private void storeSortedChunk(List<Integer> numbers, String fileId, Integer chunk) {
        db.getCollection("sorted.chunks").insertOne(new Document()
                .append("file_id", fileId)
                .append("chunk", chunk)
                .append("data", numbers)
        );
    	
        System.out.println("Stored fileID " + fileId);
    }

    private List<Integer> sortChunk(String[] data) {
		String outputString = "";
        Pattern p = Pattern.compile("-?\\d+");
        List<Integer> numbers = new ArrayList<>();
        for (String line : data) {
            Matcher m = p.matcher(line);
            while (m.find()) {
                String s = m.group();
                numbers.add(Integer.parseInt(s));
            }
            if(numbers.isEmpty())
                continue;
            Collections.sort(numbers);
        }
        return numbers;
    }

    private String[] fetchChunkData(String fileId, Integer chunk) {
        byte[] data = ((Binary) db.getCollection("fs.chunks")
                .find(and(eq("files_id", new ObjectId(fileId)), eq("n", chunk)))
                .first()
                .get("data")).getData();
        return new String(data, StandardCharsets.UTF_8).split("\n");
    }

    private String[] assembleLinesForCurrentChunk(String fileId, int chunk, int totalChunks) {
        String[] lines = fetchChunkData(fileId, chunk);
        if (chunk != totalChunks - 1) {
            String[] nextChunkLines = fetchChunkData(fileId, chunk + 1);
            lines[lines.length - 1] = lines[lines.length - 1] + nextChunkLines[0];
        }
        int firstIndex = chunk == 0 ? 0 : 1;
        return Arrays.copyOfRange(lines, firstIndex, lines.length);
    }

    private int fetchTotalNumberOfChunks(String fileId) {
        return db.getCollection("fs.chunks")
                .find(eq("files_id", new ObjectId(fileId)))
                .into(new ArrayList<>()).size();
    }

    private int getNumberOfProcessedChunks(String fileId) {
        return db.getCollection("sorted.chunks")
                .find(eq("file_id", fileId))
                .into(new ArrayList<>()).size();
    }
}
