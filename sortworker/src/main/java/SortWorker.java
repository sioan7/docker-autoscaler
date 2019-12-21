import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class SortWorker extends AbstractWorker {

    public SortWorker() throws IOException, TimeoutException {
        super("SortWorkerMQ", "SortWorker");
    }

    @Override
    public void handleTasks() {

    }

    public void sortInputFile(File textFile){

    }
}
