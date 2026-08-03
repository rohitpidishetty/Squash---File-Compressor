package lz77.encoder;

import huffman.encoder.CanonicalHuffmanEncoder;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LZ77Encoder {

  private static final int WINDOW_SIZE = 65_535;
  private static final int LOOKAHEAD_SIZE = 512;
  private static final int MIN_MATCH = 4;
  private static final int HASH_BITS = 16;
  private static final int HASH_SIZE = 1 << HASH_BITS;
  private static final int HASH_MASK = HASH_SIZE - 1;
  private static final int MAX_CHAIN_SEARCH = 128;
  private static final int FIRST_LENGTH_SYMBOL = 256;
  private static final int LENGTH_SYMBOL_COUNT = LOOKAHEAD_SIZE - MIN_MATCH + 1;
  private static final int EOS_SYMBOL =
    FIRST_LENGTH_SYMBOL + LENGTH_SYMBOL_COUNT;
  private static final int LITERAL_LENGTH_ALPHABET_SIZE = EOS_SYMBOL + 1;
  private static final int DISTANCE_ALPHABET_SIZE = 16;
  private static final int MODE_RAW = 0;
  private static final int MODE_COMPRESSED = 1;

  public static void encodeStream(
    byte[] input,
    int inputLength,
    boolean debug,
    DataOutputStream output
  ) throws IOException {
    if (input == null) {
      throw new IllegalArgumentException("Input cannot be null");
    }

    if (inputLength < 0 || inputLength > input.length) {
      throw new IllegalArgumentException(
        "Invalid input length: " + inputLength
      );
    }

    byte[] compressed = compress(input, inputLength, debug);

    /*
     * Per-chunk format:
     *
     * byte mode
     * int originalLength
     * int storedLength
     * byte[] storedData
     */
    if (compressed.length >= inputLength) {
      output.writeByte(MODE_RAW);
      output.writeInt(inputLength);
      output.writeInt(inputLength);
      output.write(input, 0, inputLength);

      if (debug) {
        System.out.printf(
          "[RAW] original=%d, stored=%d%n",
          inputLength,
          inputLength
        );
      }
    } else {
      output.writeByte(MODE_COMPRESSED);
      output.writeInt(inputLength);
      output.writeInt(compressed.length);
      output.write(compressed);

      if (debug) {
        double ratio =
          inputLength == 0 ? 0.0 : (compressed.length * 100.0) / inputLength;
        System.out.printf(
          "[COMPRESSED] original=%d, stored=%d, ratio=%.2f%%%n",
          inputLength,
          compressed.length,
          ratio
        );
      }
    }
  }

  // +----------------------------------------+
  //            Helper Functions
  // +----------------------------------------+

  private static byte[] compress(byte[] input, int inputLength, boolean debug)
    throws IOException {
    List<Token> tokens = createTokens(input, inputLength, debug);
    int[] literalLengthFrequencies = new int[LITERAL_LENGTH_ALPHABET_SIZE];
    int[] distanceFrequencies = new int[DISTANCE_ALPHABET_SIZE];
    calculateFrequencies(tokens, literalLengthFrequencies, distanceFrequencies);
    CanonicalHuffmanEncoder.Codebook literalLengthCodebook =
      CanonicalHuffmanEncoder.build(literalLengthFrequencies);
    CanonicalHuffmanEncoder.Codebook distanceCodebook =
      CanonicalHuffmanEncoder.build(distanceFrequencies);
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(inputLength);
    DataOutputStream metadataOutput = new DataOutputStream(byteOutput);

    /*
     * Store canonical Huffman code lengths.
     *
     * The decoder can rebuild the exact canonical codes from these lengths.
     */
    literalLengthCodebook.writeHeader(metadataOutput);
    distanceCodebook.writeHeader(metadataOutput);

    BitOutputStream bitOutput = new BitOutputStream(byteOutput);

    for (Token token : tokens) {
      if (token.isLiteral()) {
        int literal = token.literal & 0xFF;
        literalLengthCodebook.writeSymbol(literal, bitOutput);
      } else {
        int lengthSymbol = lengthToSymbol(token.length);
        literalLengthCodebook.writeSymbol(lengthSymbol, bitOutput);
        DistanceEncoding distanceEncoding = encodeDistance(token.distance);
        distanceCodebook.writeSymbol(distanceEncoding.category, bitOutput);
        if (distanceEncoding.extraBitCount > 0) {
          bitOutput.writeBits(
            distanceEncoding.extraValue,
            distanceEncoding.extraBitCount
          );
        }
      }
    }

    literalLengthCodebook.writeSymbol(EOS_SYMBOL, bitOutput);
    bitOutput.finish();
    metadataOutput.flush();
    return byteOutput.toByteArray();
  }

  private static List<Token> createTokens(
    byte[] input,
    int inputLength,
    boolean debug
  ) {
    List<Token> tokens = new ArrayList<>();
    int[] head = new int[HASH_SIZE];
    int[] previous = new int[inputLength];
    Arrays.fill(head, -1);
    Arrays.fill(previous, -1);
    int position = 0;
    while (position < inputLength) {
      Match match = findBestMatch(input, inputLength, position, head, previous);
      if (match.length >= MIN_MATCH) {
        tokens.add(Token.match(match.distance, match.length));
        if (debug) {
          System.out.printf(
            "<distance=%d,length=%d>%n",
            match.distance,
            match.length
          );
        }
        int end = Math.min(position + match.length, inputLength);
        for (int index = position; index < end; index++) {
          insertPosition(input, inputLength, index, head, previous);
        }
        position += match.length;
      } else {
        tokens.add(Token.literal(input[position]));
        if (debug) System.out.printf("<literal=%d>%n", input[position] & 0xFF);
        insertPosition(input, inputLength, position, head, previous);
        position++;
      }
    }

    return tokens;
  }

  private static void calculateFrequencies(
    List<Token> tokens,
    int[] literalLengthFrequencies,
    int[] distanceFrequencies
  ) {
    for (Token token : tokens) {
      if (token.isLiteral()) literalLengthFrequencies[token.literal & 0xFF]++;
      else {
        int lengthSymbol = lengthToSymbol(token.length);
        literalLengthFrequencies[lengthSymbol]++;
        DistanceEncoding distanceEncoding = encodeDistance(token.distance);
        distanceFrequencies[distanceEncoding.category]++;
      }
    }
    literalLengthFrequencies[EOS_SYMBOL]++;
    boolean hasDistance = false;
    for (int frequency : distanceFrequencies) {
      if (frequency > 0) {
        hasDistance = true;
        break;
      }
    }
    if (!hasDistance) distanceFrequencies[0] = 1;
  }

  private static int lengthToSymbol(int length) {
    if (
      length < MIN_MATCH || length > LOOKAHEAD_SIZE
    ) throw new IllegalArgumentException("Invalid match length: " + length);

    return FIRST_LENGTH_SYMBOL + (length - MIN_MATCH);
  }

  private static DistanceEncoding encodeDistance(int distance) {
    if (distance < 1 || distance > WINDOW_SIZE) {
      throw new IllegalArgumentException("Invalid match distance: " + distance);
    }
    int category = 31 - Integer.numberOfLeadingZeros(distance);
    int base = 1 << category;
    int extraValue = distance - base;
    return new DistanceEncoding(category, extraValue, category);
  }

  private static Match findBestMatch(
    byte[] input,
    int inputLength,
    int position,
    int[] head,
    int[] previous
  ) {
    if (position + MIN_MATCH > inputLength) return Match.NONE;
    int hash = hash(input, position);
    int candidate = head[hash];
    int bestLength = 0;
    int bestDistance = 0;
    int attempts = 0;
    int maximumLength = Math.min(LOOKAHEAD_SIZE, inputLength - position);
    while (candidate >= 0 && attempts < MAX_CHAIN_SEARCH) {
      int distance = position - candidate;
      if (distance > WINDOW_SIZE) break;
      if (
        bestLength < maximumLength &&
        input[candidate + bestLength] == input[position + bestLength]
      ) {
        int length = matchLength(input, candidate, position, maximumLength);
        if (length > bestLength) {
          bestLength = length;
          bestDistance = distance;
          if (bestLength == maximumLength) break;
        }
      }
      candidate = previous[candidate];
      attempts++;
    }
    if (bestLength < MIN_MATCH) return Match.NONE;
    return new Match(bestDistance, bestLength);
  }

  private static int matchLength(
    byte[] input,
    int candidate,
    int position,
    int maximumLength
  ) {
    int length = 0;
    int distance = position - candidate;
    while (length < maximumLength) {
      int sourceIndex = candidate + (length % distance);
      if (input[sourceIndex] != input[position + length]) break;
      length++;
    }
    return length;
  }

  private static void insertPosition(
    byte[] input,
    int inputLength,
    int position,
    int[] head,
    int[] previous
  ) {
    if (position + MIN_MATCH > inputLength) return;
    int hash = hash(input, position);
    previous[position] = head[hash];
    head[hash] = position;
  }

  private static int hash(byte[] input, int position) {
    int value =
      ((input[position] & 0xFF) * 0x1E35A7BD) ^
      ((input[position + 1] & 0xFF) * 0x9E3779B1) ^
      ((input[position + 2] & 0xFF) * 0x85EBCA77) ^
      ((input[position + 3] & 0xFF) * 0xC2B2AE3D);
    value ^= value >>> 16;
    return value & HASH_MASK;
  }

  private static final class Token {

    private final Byte literal;
    private final int distance;
    private final int length;

    private Token(Byte literal, int distance, int length) {
      this.literal = literal;
      this.distance = distance;
      this.length = length;
    }

    private static Token literal(byte value) {
      return new Token(value, 0, 0);
    }

    private static Token match(int distance, int length) {
      return new Token(null, distance, length);
    }

    private boolean isLiteral() {
      return literal != null;
    }
  }

  private static final class Match {

    private static final Match NONE = new Match(0, 0);

    private final int distance;
    private final int length;

    private Match(int distance, int length) {
      this.distance = distance;
      this.length = length;
    }
  }

  private static final class DistanceEncoding {

    private final int category;
    private final int extraValue;
    private final int extraBitCount;

    private DistanceEncoding(int category, int extraValue, int extraBitCount) {
      this.category = category;
      this.extraValue = extraValue;
      this.extraBitCount = extraBitCount;
    }
  }
}
