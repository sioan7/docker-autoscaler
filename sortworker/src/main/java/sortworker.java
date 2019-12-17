import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import java.io.File;

public class sortworker {
    MongoDatabase db;
    MongoClient mongoClient;
    GridFSBucket gridFSFilesBucket;

    public sortworker(){
        mongoClient = MongoClients.create();
        db = mongoClient.getDatabase("TextDocumentsDB");
        // Create a gridFSBucket with a custom bucket name "files"
        gridFSFilesBucket = GridFSBuckets.create(db, "files");
    }

    public void sortInputFile(File textFile){

    }
}
