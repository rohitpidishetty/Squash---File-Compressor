package compressor.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class TreeBuilder {

  private TreeNodeTuple root = null;
  private Map<?, ?> embeddings;
  private PriorityQueue<TreeNodeTuple> transientQueue;

  public TreeBuilder(PriorityQueue<TreeNodeTuple> queue) {
    this.transientQueue = new PriorityQueue<>(queue);
    while (queue.size() > 1) {
      TreeNodeTuple t1 = queue.poll();
      TreeNodeTuple t2 = queue.poll();
      int cumulativeFreq = t1.Frequency + t2.Frequency;
      TreeNodeTuple pool = new TreeNodeTuple((byte) 0, cumulativeFreq);
      pool.leftTuple = t1;
      pool.rightTuple = t2;
      queue.offer(pool);
    }
    this.root = queue.poll();
  }

  public void scanTree(
    TreeNodeTuple root,
    byte value,
    StringBuilder embedding
  ) {
    if (root == null) return;
    if (value == root.Byte) return;
    // search left
    embedding.append('0');
    scanTree(root.leftTuple, value, embedding);

    // search right
    embedding.append('1');
    scanTree(root.rightTuple, value, embedding);
    embedding.deleteCharAt(embedding.length() - 1);
  }

  private void buildCodeMap(
    TreeNodeTuple node,
    String code,
    Map<Byte, String> map
  ) {
    if (node == null) return;
    if (node.isLeaf()) {
      map.put(node.Byte, code);
      return;
    }
    buildCodeMap(node.leftTuple, code + "0", map);
    buildCodeMap(node.rightTuple, code + "1", map);
  }

  public Map<Byte, String> generateEmbeddings() {
    Map<Byte, String> embeddings = new HashMap<>();
    buildCodeMap(root, "", embeddings);
    return embeddings;
  }
}
