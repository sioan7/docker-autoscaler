import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class numberworkerTest {

    private numberworker numberworker;
    private File test1;

    @Before
    public void setUpTests() {
        numberworker = new numberworker();
        test1 = new File("src/test/resources/test1.txt");
    }

    @Test
    public void testFindNumbers() {
        assertTrue("File is not accesible!", test1.canRead());
        assertEquals("The number of number occurences is 6.", 6, numberworker.findNumbers(test1));
    }

    /**
     * MongoDB Container needs to run
     */
    @Test
    public void testMongoDBConnection() {
        ObjectId fileId = null;
        try {
            InputStream streamToUploadFrom = new FileInputStream(test1);
            // Create some custom options
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(358400)
                    .metadata(new Document("type", "text"));

            fileId = numberworker.gridFSBucket.uploadFromStream("text1", streamToUploadFrom, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Stored fileID " + fileId);

        GridFSDownloadStream downloadStream = numberworker.gridFSBucket.openDownloadStream(fileId);
        int fileLength = (int) downloadStream.getGridFSFile().getLength();
        byte[] bytesToWriteTo = new byte[fileLength];
        downloadStream.read(bytesToWriteTo);
        downloadStream.close();

        System.out.println(new String(bytesToWriteTo, StandardCharsets.UTF_8));

        numberworker.gridFSBucket.delete(fileId);
    }
}
