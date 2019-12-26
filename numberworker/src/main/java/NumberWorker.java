import com.rabbitmq.client.DeliverCallback;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberWorker extends AbstractWorker {

    public NumberWorker() throws IOException, TimeoutException {
        super("NumberWorkerMQ", "NumberWorker", 5672);
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
            int amountOfNumbers = findNumbers(file);
            System.out.println(amountOfNumbers + " numbers are in the file " + fileName);
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
    /**
     * This method gets a file and reads all Integer out of it.
     * It will return the amount of Integer, but it would also be possible to return the numbers.
     *
     * @param textFile : java.io.File
     */
    int findNumbers(File textFile) {
        List numbers = new ArrayList();
        int numberCounter = 0;
        Pattern p = Pattern.compile("-?\\d+");
        try {
            FileReader fr = new FileReader(textFile);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                while (m.find()) {
                    String s = m.group();
                    numbers.add(Integer.parseInt(s));
                    numberCounter++;
                }
            }
            fr.close();    //closes the stream and release the resources
        } catch (IOException e) {
            e.printStackTrace();
        }

        return numberCounter;
    }
}
