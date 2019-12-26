import com.rabbitmq.client.DeliverCallback;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
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
            //TODO implement stuff
//            System.out.println(sortedNumbers + " numbers are in the file " + fileName);
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

    public void sortInputFile(File textFile){
        //TODO: implement stuff
    }
}
