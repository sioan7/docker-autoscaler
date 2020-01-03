package nl.leidenuniv;

import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.rabbitmq.client.DeliverCallback;
import org.bson.Document;
import org.bson.types.Binary;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class ReduceWorker extends AbstractWorker {

    public ReduceWorker() throws IOException, TimeoutException {
        super("ReduceWorkerMQ", "nl.leidenuniv.ReduceWorker", 5672);
    }

    @Override
    public void handleTasks() {
        System.out.println("Started service --> " + ServiceName);
        System.out.println("Waiting for tasks out of the queue");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(message);
                String fileID = (String) jsonObject.get("FileID");
                String fileName = (String) jsonObject.get("FileName");
//                System.out.println("Received file name " + fileName);
//                File file = getFileFromMyMongo(fileID);
                String sortedNumbers = reduceLists(getInputForReduceLists(fileID));
                saveDataInGridFS(fileID, fileName, sortedNumbers);
                System.out.println("Reduced sorted lists " + fileID);
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

    public String reduceLists(List<List<Integer>> listOfNumberLists) {
        List<Integer> returnList = new ArrayList<>();
        while (!listOfNumberLists.isEmpty()) {
            int smallestValue = Integer.MAX_VALUE;
            int indexOfListOfNumbersLists = -1;
            for (int i = 0; i < listOfNumberLists.size(); i++) {
                if (listOfNumberLists.get(i).get(0) < smallestValue) {
                    smallestValue = listOfNumberLists.get(i).get(0);
                    indexOfListOfNumbersLists = i;
                }
            }
            returnList.add(smallestValue);
            listOfNumberLists.get(indexOfListOfNumbersLists).remove(0);
            if (listOfNumberLists.get(indexOfListOfNumbersLists).isEmpty())
                listOfNumberLists.remove(indexOfListOfNumbersLists);
        }
        return returnList.stream().map(Object::toString).collect(Collectors.joining(" "));
    }

    public List<List<Integer>> getInputForReduceLists(String fileId){
        List<List<Integer>> output = new ArrayList<>();
        db.getCollection("sorted.chunks")
                .find(eq("file_id", fileId))
                .forEach((Consumer<? super Document>) it -> {
                    output.add(it.getList("data", Integer.class));
                });
        return output;
    }

    public void saveDataInGridFS(String fileId, String filename, String data) {
        GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(255 * 1024);
        gridFSBucket.uploadFromStream(filename + "-" + fileId, new ByteArrayInputStream(data.getBytes()), options);
    }
}
