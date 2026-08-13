package compressor.engine;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import lz77.encoder.LZ77Encoder;

public final class FileSquasher {

  private static final int DATA_CHUNK = 65536;
  private static final String PRESENT_WORKING_DIR = System.getProperty(
    "user.dir"
  );
  private static String PresentWorkingDir;

  static {
    PresentWorkingDir = new String(
      PRESENT_WORKING_DIR.substring(PRESENT_WORKING_DIR.lastIndexOf("\\") + 1)
    );
  }

  public static void compress(
    String rootFolder,
    File targetFile,
    boolean debugMode,
    DataOutputStream dos
  ) {
    if (
      targetFile == null || !targetFile.exists()
    ) throw new IllegalArgumentException("Target does not exist");
    try {
      String path = targetFile.getAbsolutePath();

      path = path.substring(path.indexOf(rootFolder));

      dos.writeUTF(path);

      long fileSize = targetFile.length();

      // Chunks in single file.
      dos.writeInt((int) ((fileSize + DATA_CHUNK - 1L) / DATA_CHUNK));

      byte[] chunk = new byte[DATA_CHUNK];

      try (
        BufferedInputStream input = new BufferedInputStream(
          new FileInputStream(targetFile),
          DATA_CHUNK
        )
      ) {
        int bytesRead;
        while (
          (bytesRead = readChunk(input, chunk)) > 0
        ) LZ77Encoder.encodeStream(chunk, bytesRead, debugMode, dos);
      }

      if (debugMode) System.out.printf("[INFO] %s squashed.\n", targetFile);
    } catch (Exception exception) {
      System.out.println("[Error] " + exception.getMessage());
      throw new RuntimeException("Could not compress " + targetFile, exception);
    }
  }

  private static int readChunk(BufferedInputStream input, byte[] buffer)
    throws IOException {
    int totalRead = 0;

    while (totalRead < buffer.length) {
      int count = input.read(buffer, totalRead, buffer.length - totalRead);
      if (count == -1) break;
      totalRead += count;
    }
    return totalRead;
  }

  public static void depthFirstSearchAllFilesAndCompress(
    String rootFolder,
    File targetFile,
    DataOutputStream dos,
    boolean debug
  ) {
    if (targetFile.isFile()) {
      System.out.printf("[INFO ] Compressing.. : %s%n", targetFile.getName());
      FileSquasher.compress(rootFolder, targetFile, debug, dos);
      System.out.printf("[DONE ] Compressed : %s%n", targetFile.getName());
      return;
    }
    for (File subFile : targetFile.listFiles())
      depthFirstSearchAllFilesAndCompress(rootFolder, subFile, dos, debug);
  }
}
