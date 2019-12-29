package nl.leidenuniv;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        try {
            ReduceWorker reduceWorker = new ReduceWorker();
            reduceWorker.handleTasks();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
