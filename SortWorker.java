import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.rabbitmq.client.DeliverCallback;

import jdk.nashorn.internal.runtime.JSONFunctions;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.awt.List;
import java.io.*;
import java.util.Arrays;
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
            String fileNameSorted = fileName + "_sorted";
            System.out.println("Received file name " + fileName);
            File file = getFileFromMyMongo(fileID);
            ObjectId newFileID = saveOutputFile(sortInputFile(file), fileNameSorted);
            sendMessageToReducer(newFileID, fileNameSorted);
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

    private void sendMessageToReducer(ObjectId objectId, String fileName) {
    	String queueNamePublish = "ReduceWorkerMQ";
    	channel = connection.createChannel();
        channel.queueDeclare(queueNamePublish, false, false, false, null);
        channel.basicQos(1); // accept only one unack-ed message at a time (see below)
        String message = "{\n" + 
        		"  \"FileID\": \"" + objectId + "\",\n" + 
        		"  \"FileName\": \"" + fileName + ""\"\n" + 
        		"}"
        //JSONObject jsonObject = new JSONObject(message);
        channel.basicPublish("", queueNamePublish, null, message.getBytes(StandardCharsets.UTF_8));
    }

    private ObjectId saveOutputFile(byte[] sortOutput, String fileNameSorted) {
    	GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(358400).metadata(new Document("type", "text"));
    	GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(fileNameSorted, options);
    	uploadStream.write(sortOutput);
    	uploadStream.close();
    	ObjectId fileId = uploadStream.getFileId();
    	
        System.out.println("Stored fileID " + fileId);
        return fileId;
    }

    public byte[] sortInputFile(File textFile){
		String outputString = "";
        Pattern p = Pattern.compile("-?\\d+");
        try {
            FileReader fr = new FileReader(textFile);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            String line;
            while ((line = br.readLine()) != null) {
                List numbers = new ArrayList();
                Matcher m = p.matcher(line);
                while (m.find()) {
                    String s = m.group();
                    numbers.add(Integer.parseInt(s));
                }
                Collections.sort(numbers);
                outputString = outputString + numbers.toString() + "\n";
            }
            fr.close();    //closes the stream and release the resources
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputString.getBytes(StandardCharsets.UTF_8);
    }
}
