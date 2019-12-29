package nl.leidenuniv;

import com.rabbitmq.client.DeliverCallback;
import org.bson.Document;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberWorker extends AbstractWorker {

    private final DeliverCallback deliverCallback;

    public NumberWorker() throws IOException, TimeoutException {
        super("NumberWorkerMQ", "nl.leidenuniv.NumberWorker", 5672);
        deliverCallback = createDeliverCallback();
    }

    @Override
    public void handleTasks() {
        System.out.println("Started service --> " + ServiceName);
        System.out.println("Waiting for tasks out of the queue");

        //autoAck off, thus we have to make the basicAck by ourselves, see above.
        try {
            channel.basicConsume(QueueName, false, deliverCallback, consumerTag -> { });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DeliverCallback createDeliverCallback() {
        return (consumerTag, delivery) -> {
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(message);
                String fileID = (String) jsonObject.get("FileID");
                String fileName = (String) jsonObject.get("FileName");
                System.out.println("Received file name " + fileName);
                byte[] file = getFileFromMyMongo(fileID);
                int amountOfNumbers = findNumbers(file);
                db.getCollection("numbers").insertOne(new Document()
                        .append("file_id", fileID)
                        .append("amount", amountOfNumbers)
                );
                System.out.println(amountOfNumbers + " numbers are in the file " + fileName);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    /**
     * This method gets a file and reads all Integer out of it.
     * It will return the amount of Integer, but it would also be possible to return the numbers.
     */
    int findNumbers(byte[] data) {
        int numberCounter = 0;
        Pattern p = Pattern.compile("-?\\d+");
        String[] split = new String(data, StandardCharsets.UTF_8).split("\n");
        for (String line : split) {
            Matcher m = p.matcher(line);
            while (m.find()) {
                String s = m.group();
                numberCounter++;
            }
        }
        return numberCounter;
    }
}
