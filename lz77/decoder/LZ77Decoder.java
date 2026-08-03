package lz77.decoder;

import huffman.decoder.CanonicalHuffmanDecoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

public final class LZ77Decoder {

  private static final int WINDOW_SIZE = 65_535;
  private static final int MIN_MATCH = 4;
  private static final int LOOKAHEAD_SIZE = 512;

  private static final int FIRST_LENGTH_SYMBOL = 256;

  private static final int LENGTH_SYMBOL_COUNT = LOOKAHEAD_SIZE - MIN_MATCH + 1;

  private static final int EOS_SYMBOL =
    FIRST_LENGTH_SYMBOL + LENGTH_SYMBOL_COUNT;

  public static byte[] decode(byte[] compressed, int originalLength)
    throws IOException {
    if (compressed == null) throw new IllegalArgumentException(
      "Compressed data cannot be null"
    );

    ByteArrayInputStream byteInput = new ByteArrayInputStream(compressed);
    DataInputStream metadata = new DataInputStream(byteInput);
    CanonicalHuffmanDecoder literalLengthDecoder =
      CanonicalHuffmanDecoder.readHeader(metadata);
    CanonicalHuffmanDecoder distanceDecoder =
      CanonicalHuffmanDecoder.readHeader(metadata);
    BitInputStream bits = new BitInputStream(byteInput);
    ByteArrayOutputStream output = new ByteArrayOutputStream(originalLength);

    while (true) {
      int symbol = literalLengthDecoder.readSymbol(bits);
      if (symbol < FIRST_LENGTH_SYMBOL) {
        output.write(symbol);
        continue;
      }
      if (symbol == EOS_SYMBOL) break;
      int length = symbolToLength(symbol);
      int distanceCategory = distanceDecoder.readSymbol(bits);
      int distance = decodeDistance(distanceCategory, bits);
      copyMatch(output, distance, length);
    }

    byte[] result = output.toByteArray();
    if (result.length != originalLength) {
      throw new IOException(
        "Decoded length mismatch. Expected " +
          originalLength +
          " got " +
          result.length
      );
    }

    return result;
  }

  private static int symbolToLength(int symbol) throws IOException {
    int length = MIN_MATCH + (symbol - FIRST_LENGTH_SYMBOL);
    if (length < MIN_MATCH || length > LOOKAHEAD_SIZE) throw new IOException(
      "Invalid length symbol: " + symbol
    );
    return length;
  }

  private static int decodeDistance(int category, BitInputStream bits)
    throws IOException {
    if (category < 0 || category > 15) throw new IOException(
      "Invalid distance category"
    );
    int base = 1 << category;
    int extra = 0;
    if (category > 0) extra = bits.readBits(category);
    return base + extra;
  }

  private static void copyMatch(
    ByteArrayOutputStream output,
    int distance,
    int length
  ) throws IOException {
    byte[] buffer = output.toByteArray();
    int start = buffer.length - distance;
    if (start < 0) throw new IOException("Invalid distance: " + distance);
    for (int i = 0; i < length; i++) {
      byte[] current = output.toByteArray();
      int source = current.length - distance;
      output.write(current[source]);
    }
  }

  private LZ77Decoder() {}
}
