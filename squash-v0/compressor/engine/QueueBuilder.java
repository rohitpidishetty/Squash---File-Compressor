package compressor.engine;

import compressor.std_target_output.SquashFileWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class QueueBuilder {

  QueueBuilder() {}

  private Map<Byte, Integer> frequency = null;
  private PriorityQueue<TreeNodeTuple> pQ = null;
  private SquashFileWriter sqfw;

  public QueueBuilder(ArrayList<File> files) {
    frequency = new HashMap<>();
    pQ = new PriorityQueue<>(Comparator.comparingInt(a -> a.Frequency));

    try {
      for (File file : files) {
        InputStream inputReader = new FileInputStream(file);

        for (byte b : inputReader.readAllBytes()) {
          frequency.put(b, frequency.getOrDefault(b, 0) + 1);
        }

        inputReader.close();
      }
      for (Map.Entry<Byte, Integer> freqMap : frequency.entrySet()) {
        pQ.add(new TreeNodeTuple(freqMap.getKey(), freqMap.getValue()));
      }
    } catch (Exception e) {}
  }

  public PriorityQueue<TreeNodeTuple> getQueue() {
    return this.pQ;
  }
}
