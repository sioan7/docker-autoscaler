import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class numberworker {

    private MongoDatabase db;
    private MongoClient mongoClient;
    GridFSBucket gridFSBucket;

    numberworker() {
        mongoClient = MongoClients.create(new ConnectionString("mongodb://0.0.0.0:27017"));
        db = mongoClient.getDatabase("TextDocumentsDB");
        // Create a gridFSBucket with a custom bucket name "files"
        gridFSBucket = GridFSBuckets.create(db, "files");
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
        Scanner input = null;
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
