import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        try {
            SortWorker sortWorker = new SortWorker();
            sortWorker.handleTasks();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
