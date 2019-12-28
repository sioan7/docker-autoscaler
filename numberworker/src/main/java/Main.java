import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        try {
            NumberWorker numberWorker = new NumberWorker();
            numberWorker.handleTasks();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
