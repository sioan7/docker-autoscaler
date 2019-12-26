import com.rabbitmq.client.DeliverCallback;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class ReduceWorker extends AbstractWorker {

    public ReduceWorker() throws IOException, TimeoutException {
        super("ReduceWorkerMQ", "ReduceWorker", 5672);
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
            String sortedNumbers = reduceLists(getInputForReduceLists(file));
            System.out.println(sortedNumbers + " numbers are in the file " + fileName);
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
        return returnList.toString();
    }

    private List<List<Integer>> getInputForReduceLists(File file){
        List<List<Integer>> output = new ArrayList<>();
        try {
            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            String line;
            while ((line = br.readLine()) != null) {
                Scanner scanner = new Scanner(line);
                List<Integer> list = new ArrayList<Integer>();
                while (scanner.hasNextInt()) {
                    list.add(scanner.nextInt());
                }
                output.add(list);
            }
            fr.close();    //closes the stream and release the resources
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }
}
