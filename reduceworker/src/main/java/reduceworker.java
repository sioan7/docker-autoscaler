import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import java.util.ArrayList;
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
}
