package lz77.encoder;

import RLE.encoder.RLE;
import huffman.buffer.Buffer;
import huffman.encoder.HuffmanEncoder;
import huffman.tree.Builder;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class LZ77Encoder {

  private static final int WINDOW_SIZE = 32768;
  private static final int LOOKAHEAD_SIZE = 258;
  private static final byte THRESHOLD = 8;

  private static class Triplet {

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

  public static void encodeStream(
    byte[] buffer,
    boolean debug,
    DataOutputStream dos
  ) throws Exception {
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
          bestDistance = distance;
        }

        windowStart++;
      }
      int nextPosition = pointer + bestLength;
      Byte next = null;
      if (nextPosition < n) next = buffer[nextPosition];
      Triplet triplet = new Triplet(bestDistance, bestLength, next);
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

    Builder offsetTreeRef = new Builder(offsetTree);
    Buffer offsetTreeRoot = offsetTreeRef.getRoot();
    toByteSequence(
      offsetTreeRoot,
      offsetTreeRef.generateEmbeddings(),
      len,
      triplets,
      enums.Triplet.OFFSET,
      dos
    );

    Builder lengthTreeRef = new Builder(lengthTree);
    Buffer lengthTreeRoot = lengthTreeRef.getRoot();
    toByteSequence(
      lengthTreeRoot,
      lengthTreeRef.generateEmbeddings(),
      len,
      triplets,
      enums.Triplet.LENGTH,
      dos
    );

    Builder codeTreeRef = new Builder(codeTree);
    Buffer codeTreeRoot = codeTreeRef.getRoot();
    toByteSequence(
      codeTreeRoot,
      codeTreeRef.generateEmbeddings(),
      len,
      triplets,
      enums.Triplet.CODE,
      dos
    );

    // System.out.println(Arrays.toString(triplets.get(i).toString().getBytes())); // writeBytes()

    // Flush memory
    offsetTree.clear();
    lengthTree.clear();
    codeTree.clear();
    offsetTreeRef = null;
    lengthTreeRef = null;
    codeTreeRef = null;
  }

  private static void serializeTree(Buffer tree, DataOutputStream dos)
    throws Exception {
    if (tree.leftTuple == null && tree.rightTuple == null) {
      // System.out.println(1); // writeBit()
      dos.writeByte(1);
      // System.out.println(tree.Key); // writeInt()
      // dos.writeInt(tree.Key);
      return;
    }
    // System.out.println(0); // writeBit()
    dos.writeByte(0);
    serializeTree(tree.leftTuple, dos);
    serializeTree(tree.rightTuple, dos);
  }

  private static void toByteSequence(
    Buffer tree,
    Map<Integer, String> embeddings,
    int len,
    List<Triplet> triplets,
    enums.Triplet type,
    DataOutputStream dos
  ) throws Exception {
    // Size of leaf nodes
    // System.out.println(embeddings.keySet().size()); // writeInt()
    dos.writeInt(embeddings.keySet().size());
    serializeTree(tree, dos);
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

    int i = 0;
    byte b = (byte) 0;
    int bitsMagnitude = 0;

    for (; i < len - 1; i++) {
      String em = null;

      switch (type) {
        case enums.Triplet.OFFSET:
          em = embeddings.get(triplets.get(i).offset);
          break;
        case enums.Triplet.LENGTH:
          em = embeddings.get(triplets.get(i).length);
          System.out.println(em + " > len " + triplets.get(i).length);
          break;
        case enums.Triplet.CODE:
          em = embeddings.get((int) triplets.get(i).nextCode);
          // System.out.println(em + " code -> " + triplets.get(i).nextCode);
          break;
        default:
          break;
      }

      int em_mag = em.length();
      for (int j = 0; j < em_mag; j++) {
        b = (byte) ((byte) (b << 1) | ((byte) (em.charAt(j) == '0' ? 0 : 1)));
        bitsMagnitude++;
        if (bitsMagnitude == 8) {
          byteStream.write(b);
          bitsMagnitude = 0; // Reset
          b = (byte) 0;
        }
      }
    }

    int padding = bitsMagnitude == 0 ? 0 : THRESHOLD - bitsMagnitude;
    if (bitsMagnitude > 0) {
      b <<= padding;
      byteStream.write(b);
    }
    // System.out.println(Arrays.toString(byteStream.toByteArray())); // writeByteArray()
    byte[] encodedData = byteStream.toByteArray();

    dos.writeInt(encodedData.length);
    dos.write(encodedData);

    encodedData = null;

    // System.out.println(padding); // writeByte()
    dos.write(padding);
    // System.out.println("+------------------+");
  }
}
