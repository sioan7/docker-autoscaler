import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ReduceWorker extends AbstractWorker {

    public ReduceWorker() throws IOException, TimeoutException {
        super("ReduceWorkerMQ", "ReduceWorker");
    }

    @Override
    public void handleTasks() {

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
