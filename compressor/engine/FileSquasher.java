package compressor.engine;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import lz77.encoder.LZ77Encoder;

public class FileSquasher {

  public static void read(File targetFilePath, boolean debugMode) {
    LZ77Encoder.encodeStream(
      new byte[] {
        1,
        2,
        2,
        1,
        2,
        3,
        4,
        54,
        5,
        2,
        1,
        3,
        45,
        56,
        56,
        3,
        2,
        4,
        5,
      },
      debugMode
    );
    try {
      byte chunk[] = new byte[65536]; // 64 Kilo-Bytes

      // try (FileInputStream fis = new FileInputStream(targetFilePath)) {
      //   int bytesRead = 0;
      //   while ((bytesRead = fis.read(chunk)) != -1) {
      //     LZ77Encoder.encodeStream(chunk, debugMode);
      //   }
      //   System.out.println("Done");
      // } catch (Exception e) {
      //   System.out.println("[ERROR] " + e.getMessage());
      // }
    } catch (Exception e) {
      System.out.println("[ERROR] " + e.getMessage());
      System.exit(1);
    }
  }
}
