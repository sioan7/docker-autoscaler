package nl.leidenuniv;

import java.io.File;
import java.io.IOException;

public interface IWorker {

    void handleTasks();

    File getFileFromMyMongo(String fileId) throws IOException;
}
