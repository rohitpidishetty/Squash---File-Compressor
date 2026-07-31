package compressor.engine;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import lz77.encoder.LZ77Encoder;

public final class FileSquasher {

  private static final int DATA_CHUNK = 65536;

  public static void compress(
    File targetFile,
    boolean debugMode,
    enums.Files fileDescriptor,
    DataOutputStream dos
  ) {
    if (
      targetFile == null || !targetFile.exists()
    ) throw new IllegalArgumentException("Target does not exist");

    try {
      /*
       * File type:
       * 0 = file
       * 1 = directory
       */
      dos.writeByte(fileDescriptor == enums.Files.FILE ? 0 : 1);

      dos.writeUTF(targetFile.getAbsolutePath());

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

      if (debugMode) System.out.println("[INFO] " + targetFile + " squashed.");
    } catch (IOException exception) {
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
}
