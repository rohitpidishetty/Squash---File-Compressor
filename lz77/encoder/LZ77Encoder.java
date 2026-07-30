package lz77.encoder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class LZ77Encoder {

  private static final int WINDOW_SIZE = 65535;
  private static final int LOOKAHEAD_SIZE = 512;
  private static final int MIN_MATCH = 4;
  private static final int HASH_BITS = 16;
  private static final int HASH_SIZE = 1 << HASH_BITS;
  private static final int HASH_MASK = HASH_SIZE - 1;

  private static final int MAX_CHAIN_SEARCH = 128;

  private static final int MODE_RAW = 0;
  private static final int MODE_COMPRESSED = 1;

  private LZ77Encoder() {}

  public static void encodeStream(
    byte[] input,
    int inputLength,
    boolean debug,
    DataOutputStream output
  ) throws IOException {
    if (input == null) throw new IllegalArgumentException(
      "Input cannot be null"
    );

    if (
      inputLength < 0 || inputLength > input.length
    ) throw new IllegalArgumentException(
      "Invalid input length: " + inputLength
    );

    byte[] compressed = compress(input, inputLength, debug);

    /*
     * Store raw data whenever compression is not beneficial.
     *
     * Chunk structure:
     *
     * byte mode
     * int originalLength
     * int storedLength
     * byte[] data
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

  private static byte[] compress(byte[] input, int inputLength, boolean debug)
    throws IOException {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(inputLength);

    BitOutputStream bitOutput = new BitOutputStream(byteOutput);

    /*
     * head[hash] stores the newest position with that hash.
     *
     * previous[position] links to the previous position having the same
     * hash value.
     */
    int[] head = new int[HASH_SIZE];
    int[] previous = new int[inputLength];

    Arrays.fill(head, -1);
    Arrays.fill(previous, -1);

    int position = 0;
    int literalCount = 0;
    int matchCount = 0;

    while (position < inputLength) {
      Match match = findBestMatch(input, inputLength, position, head, previous);

      if (match.length >= MIN_MATCH) {
        /*
         * Flag 1 means match.
         */
        bitOutput.writeBit(1);

        /*
         * Distance range:
         * 1 through 65,535
         *
         * Stored directly using 16 bits.
         */
        bitOutput.writeBits(match.distance, 16);

        /*
         * Length range:
         * MIN_MATCH through LOOKAHEAD_SIZE
         *
         * Store length - MIN_MATCH in nine bits.
         */
        bitOutput.writeBits(match.length - MIN_MATCH, 9);

        if (debug) {
          System.out.printf(
            "<distance=%d,length=%d>%n",
            match.distance,
            match.length
          );
        }

        int matchEnd = Math.min(position + match.length, inputLength);

        for (int index = position; index < matchEnd; index++) {
          insertPosition(input, inputLength, index, head, previous);
        }

        position += match.length;
        matchCount++;
      } else {
        /*
         * Flag 0 means literal.
         */
        bitOutput.writeBit(0);

        /*
         * Convert the signed Java byte to an unsigned 0-255 value.
         */
        bitOutput.writeBits(input[position] & 0xFF, 8);

        if (debug) System.out.printf("<literal=%d>%n", input[position] & 0xFF);

        insertPosition(input, inputLength, position, head, previous);

        position++;
        literalCount++;
      }
    }

    bitOutput.finish();

    if (debug) {
      System.out.printf(
        "[TOKENS] literals=%d, matches=%d%n",
        literalCount,
        matchCount
      );
    }

    return byteOutput.toByteArray();
  }

  private static Match findBestMatch(
    byte[] input,
    int inputLength,
    int position,
    int[] head,
    int[] previous
  ) {
    if (position + MIN_MATCH > inputLength) {
      return Match.NONE;
    }

    int hash = hash(input, position);
    int candidate = head[hash];

    int bestLength = 0;
    int bestDistance = 0;
    int attempts = 0;

    int maximumLength = Math.min(LOOKAHEAD_SIZE, inputLength - position);

    while (candidate >= 0 && attempts < MAX_CHAIN_SEARCH) {
      int distance = position - candidate;

      if (distance > WINDOW_SIZE) {
        break;
      }

      /*
       * Quick rejection before performing a longer comparison.
       */
      if (
        bestLength < maximumLength &&
        input[candidate + bestLength] == input[position + bestLength]
      ) {
        int length = matchLength(
          input,
          inputLength,
          candidate,
          position,
          maximumLength
        );

        if (length > bestLength) {
          bestLength = length;
          bestDistance = distance;

          if (bestLength == maximumLength) {
            break;
          }
        }
      }

      candidate = previous[candidate];
      attempts++;
    }

    if (bestLength < MIN_MATCH) {
      return Match.NONE;
    }

    return new Match(bestDistance, bestLength);
  }

  private static int matchLength(
    byte[] input,
    int inputLength,
    int candidate,
    int position,
    int maximumLength
  ) {
    int length = 0;
    int distance = position - candidate;

    while (length < maximumLength) {
      int sourceIndex = candidate + (length % distance);

      if (
        sourceIndex >= inputLength ||
        input[sourceIndex] != input[position + length]
      ) break;

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

  private static final class Match {

    private static final Match NONE = new Match(0, 0);

    private final int distance;
    private final int length;

    private Match(int distance, int length) {
      this.distance = distance;
      this.length = length;
    }
  }
}
