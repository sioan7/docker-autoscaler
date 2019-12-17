import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import java.util.List;

public class reduceworker {
    MongoDatabase db;
    MongoClient mongoClient;
    GridFSBucket gridFSFilesBucket;

    public reduceworker(){
        mongoClient = MongoClients.create();
        db = mongoClient.getDatabase("TextDocumentsDB");
        // Create a gridFSBucket with a custom bucket name "files"
        gridFSFilesBucket = GridFSBuckets.create(db, "files");
    }

    public String reduceLists(List<List> listOfNumberLists){

        return "";
    }
}
