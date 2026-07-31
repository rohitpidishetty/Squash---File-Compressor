package huffman.encoder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import lz77.encoder.BitOutputStream;

public final class CanonicalHuffmanEncoder {

  private CanonicalHuffmanEncoder() {}

  public static Codebook build(int[] frequencies) {
    if (
      frequencies == null || frequencies.length == 0
    ) throw new IllegalArgumentException("Frequency array cannot be empty");

    PriorityQueue<Node> queue = new PriorityQueue<>(
      Comparator.comparingLong((Node node) -> node.frequency).thenComparingInt(
        node -> node.minimumSymbol
      )
    );

    for (int symbol = 0; symbol < frequencies.length; symbol++) {
      if (frequencies[symbol] > 0) queue.add(
        Node.leaf(symbol, frequencies[symbol])
      );
    }
    if (queue.isEmpty()) throw new IllegalArgumentException(
      "At least one symbol must have a frequency"
    );
    int[] codeLengths = new int[frequencies.length];

    if (queue.size() == 1) {
      Node onlyNode = queue.remove();
      codeLengths[onlyNode.symbol] = 1;
    } else {
      while (queue.size() > 1) {
        Node first = queue.remove();
        Node second = queue.remove();
        queue.add(Node.parent(first, second));
      }
      assignLengths(queue.remove(), 0, codeLengths);
    }
    return createCanonicalCodebook(codeLengths);
  }

  public static void assignLengths(Node node, int depth, int[] codeLengths) {
    if (node.isLeaf()) {
      codeLengths[node.symbol] = Math.max(1, depth);
      return;
    }

    assignLengths(node.left, depth + 1, codeLengths);
    assignLengths(node.right, depth + 1, codeLengths);
  }

  public static Codebook createCanonicalCodebook(int[] codeLengths) {
    List<SymbolLength> symbols = new ArrayList<>();
    for (int symbol = 0; symbol < codeLengths.length; symbol++) {
      int length = codeLengths[symbol];
      if (length > 0) symbols.add(new SymbolLength(symbol, length));
    }

    symbols.sort(
      Comparator.comparingInt((SymbolLength entry) ->
        entry.length
      ).thenComparingInt(entry -> entry.symbol)
    );

    BigInteger[] codes = new BigInteger[codeLengths.length];
    BigInteger currentCode = BigInteger.ZERO;
    int previousLength = 0;

    for (SymbolLength entry : symbols) {
      int shift = entry.length - previousLength;
      currentCode = currentCode.shiftLeft(shift);
      codes[entry.symbol] = currentCode;
      currentCode = currentCode.add(BigInteger.ONE);
      previousLength = entry.length;
    }
    return new Codebook(codes, codeLengths, symbols);
  }

  public static final class Codebook {

    private final BigInteger[] codes;
    private final int[] lengths;
    private final List<SymbolLength> symbols;

    private Codebook(
      BigInteger[] codes,
      int[] lengths,
      List<SymbolLength> symbols
    ) {
      this.codes = codes;
      this.lengths = lengths;
      this.symbols = symbols;
    }

    public void writeSymbol(int symbol, BitOutputStream output)
      throws IOException {
      if (symbol < 0 || symbol >= codes.length || codes[symbol] == null) {
        throw new IllegalArgumentException(
          "Symbol is not in Huffman codebook: " + symbol
        );
      }
      output.writeBits(codes[symbol], lengths[symbol]);
    }

    public void writeHeader(DataOutputStream output) throws IOException {
      output.writeShort(symbols.size());

      for (SymbolLength entry : symbols) {
        output.writeShort(entry.symbol);
        output.writeShort(entry.length);
      }
    }
  }

  private static final class Node {

    private final int symbol;
    private final long frequency;
    private final int minimumSymbol;

    private final Node left;
    private final Node right;

    private Node(
      int symbol,
      long frequency,
      int minimumSymbol,
      Node left,
      Node right
    ) {
      this.symbol = symbol;
      this.frequency = frequency;
      this.minimumSymbol = minimumSymbol;
      this.left = left;
      this.right = right;
    }

    private static Node leaf(int symbol, long frequency) {
      return new Node(symbol, frequency, symbol, null, null);
    }

    private static Node parent(Node left, Node right) {
      return new Node(
        -1,
        left.frequency + right.frequency,
        Math.min(left.minimumSymbol, right.minimumSymbol),
        left,
        right
      );
    }

    private boolean isLeaf() {
      return left == null && right == null;
    }
  }

  private static final class SymbolLength {

    private final int symbol;
    private final int length;

    private SymbolLength(int symbol, int length) {
      this.symbol = symbol;
      this.length = length;
    }
  }
}
