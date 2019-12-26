import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ReduceWorkerTest {

    private ReduceWorker reduceworker;
    private File test1;

    @Before
    public void setUpTests() throws IOException, TimeoutException {
        reduceworker = new ReduceWorker();
        test1 = new File("src/test/resources/SortedNumbers.txt");
    }

    @Test
    public void testReduceLists(){
        List<List<Integer>> input = new ArrayList<>();
        List<Integer> row = new ArrayList<>();
    }
}
