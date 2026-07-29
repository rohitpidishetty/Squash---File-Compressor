package compressor.engine;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import lz77.encoder.LZ77Encoder;

public class FileSquasher {

  private static final int DATA_CHUNK = 65536;

  public static void compress(
    File targetFilePath,
    boolean debugMode,
    enums.Files fileDescriptor,
    DataOutputStream dos
  ) {
    try {
      if (fileDescriptor == enums.Files.FILE) {
        // System.out.println(0); // File -> 0, writeBit(0)
        dos.write(0);
      } else {
        // System.out.println(1); // Folder -> 1, writeBit(1)
        dos.write(1);
      }
      // System.out.println(
      //   Arrays.toString(targetFilePath.getAbsolutePath().getBytes())
      // ); // writeBytes()
      dos.write(targetFilePath.getAbsolutePath().getBytes());

      byte chunk[] = new byte[DATA_CHUNK]; // 64 Kilo-Bytes

      // total number of chunks
      dos.writeInt((int) Math.ceil(targetFilePath.length() / DATA_CHUNK));

      try (FileInputStream fis = new FileInputStream(targetFilePath)) {
        int bytesRead = 0;
        while ((bytesRead = fis.read(chunk)) != -1) LZ77Encoder.encodeStream(
          chunk,
          debugMode,
          dos
        );
        System.out.println("[INFO] " + targetFilePath + " squashed.");
      } catch (Exception e) {
        System.out.println("[ERROR] " + e.getMessage());
      }
    } catch (Exception e) {
      System.out.println("[ERROR] " + e.getMessage());
      System.exit(1);
    }
  }
}

/*
0 -> File | Folder (Indicator)

[67, 58, 92, 85, 115, 101, 114, 115, 92, 114, 111, 104, 105, 116, 92, 79, 110, 101, 68, 114, 105, 118, 101, 92, 68, 101, 115, 107, 116, 111, 112, 92, 66, 97, 115, 101, 92, 84, 105, 101, 114, 50, 92, 82, 111, 104, 105, 116, 95, 83, 68, 69, 46, 112, 100, 102] -> (FilePath)

1

5 -> (Offset Leaves Length)

0
0
1
1
0
1
7
0
1
15
1
3
1
0
[-57, -21, 24] -> Offset Compression
2 -> Offset Padding

3 -> (Length Leaves Length)

0
0
1
2
1
1
1
0
[-41, -102, -128] -> Length Compression
7 -> Length Padding

8 -> (Code Leaves Length)

0
0
0
1
56
1
45
0
1
2
0
1
54
1
5
0
1
3
0
1
1
1
4
[-53, 93, -98, 34, -32] -> Code Compression
5 -> Length Padding

[60, 49, 48, 44, 49, 44, 69, 79, 83, 62] -> <o,l,c> (Last triplet in chunk)
 */
