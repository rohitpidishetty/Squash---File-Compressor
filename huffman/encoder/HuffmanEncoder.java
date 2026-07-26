package huffman.encoder;

import huffman.buffer.Buffer;
import huffman.buffer.CodeFrequency;
import huffman.buffer.LengthFrequency;
import huffman.buffer.OffsetFrequency;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class HuffmanEncoder {

  private Map<Integer, OffsetFrequency> offsetFreqLookUp;
  private Map<Integer, LengthFrequency> lengthFreqLookUp;
  private Map<Byte, CodeFrequency> codeFreqLookUp;

  public HuffmanEncoder() {
    this.offsetFreqLookUp = new HashMap<>();
    this.lengthFreqLookUp = new HashMap<>();
    this.codeFreqLookUp = new HashMap<>();
    this.offsetTree = new PriorityQueue<>((a, b) -> a.Frequency - b.Frequency);
    this.lengthTree = new PriorityQueue<>((a, b) -> a.Frequency - b.Frequency);
    this.codeTree = new PriorityQueue<>((a, b) -> a.Frequency - b.Frequency);
  }

  private PriorityQueue<Buffer> offsetTree;
  private PriorityQueue<Buffer> lengthTree;
  private PriorityQueue<Buffer> codeTree;

  public void calculateFrequency(int value, enums.Triplet type) {
    switch (type) {
      case enums.Triplet.OFFSET:
        if (!offsetFreqLookUp.containsKey(value)) {
          Buffer newOffsetRef = new OffsetFrequency(value);
          offsetFreqLookUp.put(value, (OffsetFrequency) newOffsetRef);
          offsetTree.add(newOffsetRef);
        }
        offsetFreqLookUp.get(value).incrementFrequency();
        break;
      case enums.Triplet.LENGTH:
        if (!lengthFreqLookUp.containsKey(value)) {
          Buffer newLengthRef = new LengthFrequency(value);
          lengthFreqLookUp.put(value, (LengthFrequency) newLengthRef);
          lengthTree.add(newLengthRef);
        }
        lengthFreqLookUp.get(value).incrementFrequency();
        break;
      case enums.Triplet.CODE:
        Byte B = (byte) value;
        if (!codeFreqLookUp.containsKey(B)) {
          Buffer newCodeRef = new CodeFrequency(B);
          codeFreqLookUp.put(B, (CodeFrequency) newCodeRef);
          codeTree.add(newCodeRef);
        }
        codeFreqLookUp.get(B).incrementFrequency();
        break;
      default:
        break;
    }
  }

  public PriorityQueue<Buffer> getOffsetTree() {
    return this.offsetTree;
  }

  public PriorityQueue<Buffer> getLengthTree() {
    return this.lengthTree;
  }

  public PriorityQueue<Buffer> getCodeTree() {
    return this.codeTree;
  }
}
