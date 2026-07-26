package compressor.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class TreeBuilder {

  private TreeNodeTuple root = null;

  public TreeBuilder(PriorityQueue<TreeNodeTuple> queue) {
    while (queue.size() > 1) {
      TreeNodeTuple t1 = queue.poll();
      TreeNodeTuple t2 = queue.poll();
      TreeNodeTuple sub_root = new TreeNodeTuple(
        (byte) 0,
        (t1.Frequency + t2.Frequency)
      );
      sub_root.leftTuple = t1;
      sub_root.rightTuple = t2;
      queue.offer(sub_root);
    }
    this.root = queue.poll();
  }

  private void buildCodeMap(
    TreeNodeTuple node,
    String code,
    Map<Byte, String> map
  ) {
    if (node == null) return;
    if (node.isLeaf()) {
      map.put(node.Byte, code.length() > 0 ? code : "0");
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
