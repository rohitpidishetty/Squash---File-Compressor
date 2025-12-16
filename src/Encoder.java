// package squash.archive;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Encoder {

  private CharFrequency treeState;

  protected void generateTree(PriorityQueue<CharFrequency> freqQ) {
    while (!freqQ.isEmpty()) {
      CharFrequency left = freqQ.poll(), right = freqQ.poll();
      CharFrequency node = new CharFrequency(
        null,
        left.freq + right.freq,
        NodeType.INTERMEDIATE
      );
      node.leftNode = left;
      node.rightNode = right;
      freqQ.offer(node);
      if (freqQ.size() == 1) break;
    }
    this.treeState = freqQ.poll();
  }

  private Map<Character, String> encodings = new HashMap<>();

  protected void dfs(CharFrequency root, StringBuilder binaryRepresentation) {
    if (root == null) return;
    if (root.type == NodeType.LEAF) {
      encodings.put(root.ch, binaryRepresentation.toString());
      return;
    }
    binaryRepresentation.append('0');
    dfs(root.leftNode, binaryRepresentation);
    binaryRepresentation.deleteCharAt(binaryRepresentation.length() - 1);

    binaryRepresentation.append('1');
    dfs(root.rightNode, binaryRepresentation);
    binaryRepresentation.deleteCharAt(binaryRepresentation.length() - 1);
  }

  protected Map<Character, String> generateEncodings() {
    dfs(this.treeState, new StringBuilder());
    return encodings;
  }
}
