import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class numberworker {

    MongoDatabase db;
    MongoClient mongoClient;
    GridFSBucket gridFSFilesBucket;

    public numberworker() {
        mongoClient = MongoClients.create();
        db = mongoClient.getDatabase("TextDocumentsDB");
        // Create a gridFSBucket with a custom bucket name "files"
        gridFSFilesBucket = GridFSBuckets.create(db, "files");
    }

    /**
     * This method gets a file and reads all Integer out of it.
     * It will return the amount of Integer, but it would also be possible to return the numbers.
     *
     * @param textFile : java.io.File
     */
    public int findNumbers(File textFile) {
        List numbers = new ArrayList();
        int numberCounter = 0;
        Scanner input = null;
        try {
            input = new Scanner(textFile);
        } catch (Exception ex) {
            System.out.println("Can not open file: " + textFile.getName());
        }
        while (input.hasNextInt()) {
            int number = input.nextInt();
            numbers.add(number);
            numberCounter++;
        }
        return numberCounter;
    }
}
