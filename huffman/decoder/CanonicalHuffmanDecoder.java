package huffman.decoder;

import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import lz77.decoder.BitInputStream;

public final class CanonicalHuffmanDecoder {

  private final Map<String, Integer> lookup;

  private CanonicalHuffmanDecoder(Map<String, Integer> lookup) {
    this.lookup = lookup;
  }

  public static CanonicalHuffmanDecoder readHeader(DataInputStream input)
    throws IOException {
    int count = input.readUnsignedShort();

    List<Entry> entries = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      int symbol = input.readUnsignedShort();

      int length = input.readUnsignedShort();

      entries.add(new Entry(symbol, length));
    }

    entries.sort(
      Comparator.comparingInt((Entry e) -> e.length).thenComparingInt(e ->
        e.symbol
      )
    );

    Map<String, Integer> table = new HashMap<>();

    BigInteger code = BigInteger.ZERO;

    int previousLength = 0;

    for (Entry entry : entries) {
      int shift = entry.length - previousLength;

      code = code.shiftLeft(shift);

      table.put(key(code, entry.length), entry.symbol);

      code = code.add(BigInteger.ONE);

      previousLength = entry.length;
    }

    return new CanonicalHuffmanDecoder(table);
  }

  public int readSymbol(BitInputStream input) throws IOException {
    StringBuilder bits = new StringBuilder();

    while (true) {
      int bit = input.readBit();

      if (bit == -1) throw new IOException("Unexpected EOF");

      bits.append(bit);

      Integer symbol = lookup.get(bits.toString());

      if (symbol != null) return symbol;
    }
  }

  private static String key(BigInteger code, int length) {
    StringBuilder b = new StringBuilder();

    for (int i = length - 1; i >= 0; i--) b.append(code.testBit(i) ? '1' : '0');

    return b.toString();
  }

  private static class Entry {

    int symbol;
    int length;

    Entry(int symbol, int length) {
      this.symbol = symbol;
      this.length = length;
    }
  }
}
