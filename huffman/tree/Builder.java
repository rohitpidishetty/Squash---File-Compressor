package huffman.tree;

import huffman.buffer.Buffer;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Builder {

  private Buffer root = null;

  public Builder(PriorityQueue<Buffer> queue) {
    while (queue.size() > 1) {
      Buffer t1 = queue.poll();
      Buffer t2 = queue.poll();
      Buffer sub_root = new Buffer(0, (t1.Frequency + t2.Frequency));
      sub_root.leftTuple = t1;
      sub_root.rightTuple = t2;
      queue.offer(sub_root);
    }
    this.root = queue.poll();
  }

  public Buffer getRoot() {
    return this.root;
  }

  private void buildCodeMap(
    Buffer node,
    String code,
    Map<Integer, String> map
  ) {
    if (node == null) return;
    if (node.isLeaf()) {
      map.put(node.Key, code.length() > 0 ? code : "0");
      return;
    }
    buildCodeMap(node.leftTuple, code + "0", map);
    buildCodeMap(node.rightTuple, code + "1", map);
  }

  public Map<Integer, String> generateEmbeddings() {
    Map<Integer, String> embeddings = new HashMap<>();
    buildCodeMap(root, "", embeddings);
    return embeddings;
  }
}
