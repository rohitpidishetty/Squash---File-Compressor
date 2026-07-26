package lz77.encoder;

import huffman.buffer.Buffer;
import huffman.encoder.HuffmanEncoder;
import huffman.tree.Builder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class LZ77Encoder {

  private static final int WINDOW_SIZE = 32768;
  private static final int LOOKAHEAD_SIZE = 258;

  private class Triplet {

    int offset, length;
    Byte nextCode;

    public Triplet(int offset, int length, Byte nextCode) {
      this.offset = offset;
      this.length = length;
      this.nextCode = nextCode;
    }

    @Override
    public String toString() {
      return String.format(
        "<%d,%d,%s>",
        offset,
        length,
        nextCode == null ? "EOS" : String.valueOf(nextCode)
      );
    }
    // EOS: End Of Stream
  }

  public static void encodeStream(byte[] buffer, boolean debug) {
    List<Triplet> triplets = new ArrayList<>();
    int n = buffer.length;
    int pointer = 0;
    while (pointer < n) {
      int bestDistance = 0;
      int bestLength = 0;

      int windowStart = Math.max(0, pointer - WINDOW_SIZE);
      int lookAheadBufferLimit = Math.min(LOOKAHEAD_SIZE, n - pointer);
      while (windowStart < pointer) {
        int length = 0;
        int distance = pointer - windowStart;
        while (length < lookAheadBufferLimit) {
          if (
            buffer[windowStart + (length % distance)] !=
            buffer[pointer + length]
          ) break;
          length++;
        }
        if (length > bestLength) {
          bestLength = length;

          if (bestLength == lookAheadBufferLimit) break;
          bestDistance = distance;
        }

        windowStart++;
      }
      int nextPosition = pointer + bestLength;
      Byte next = null;
      if (nextPosition < n) next = buffer[nextPosition];
      Triplet triplet = new LZ77Encoder().new Triplet(
        bestDistance,
        bestLength,
        next
      );
      triplets.add(triplet);
      if (debug) System.out.println(triplet);
      pointer += bestLength;
      if (next != null) pointer++;
    }
    if (debug) System.out.println("+==============+");
    int len = triplets.size();
    int i = 0;
    HuffmanEncoder huffEnc = new HuffmanEncoder();

    for (; i < len - 1; i++) {
      huffEnc.calculateFrequency(triplets.get(i).offset, enums.Triplet.OFFSET);
      huffEnc.calculateFrequency(triplets.get(i).length, enums.Triplet.LENGTH);
      huffEnc.calculateFrequency(triplets.get(i).nextCode, enums.Triplet.CODE);
    }
    PriorityQueue<Buffer> offsetTree = huffEnc.getOffsetTree();
    PriorityQueue<Buffer> lengthTree = huffEnc.getLengthTree();
    PriorityQueue<Buffer> codeTree = huffEnc.getCodeTree();

    // while (offsetTree.isEmpty() == false) {
    //   System.out.println(
    //     offsetTree.peek().Key + " " + offsetTree.peek().Frequency
    //   );
    //   offsetTree.poll();
    // }

    Map<Integer, String> offsetEmbedding = new Builder(
      offsetTree
    ).generateEmbeddings();
    new Builder(lengthTree).generateEmbeddings();
    new Builder(codeTree).generateEmbeddings();

    i = 0;
    for (; i < len - 1; i++) {
      System.out.println(
        triplets.get(i).offset +
          " " +
          offsetEmbedding.get(triplets.get(i).offset)
      );
    }

    System.out.println(triplets.get(i));
  }
}
