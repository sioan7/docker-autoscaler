import java.io.File;
import java.io.IOException;

public interface IWorker {

    void handleTasks();

    byte[] getFileFromMyMongo(String fileId) throws IOException;
}
