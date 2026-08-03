package decompressor.engine;

import java.io.*;
import lz77.decoder.LZ77Decoder;

public final class FileDeSquasher {

  public static void decompress(DataInputStream input, File outputRoot)
    throws IOException {
    byte type = input.readByte();

    if (type == 0) decompressFile(input, outputRoot);
    else if (type == 1) {
      while (true) {
        try {
          decompressFile(input, outputRoot);
        } catch (EOFException eof) {
          break;
        }
      }
    } else {
      throw new IOException("Invalid squash type");
    }
  }

  private static void decompressFile(DataInputStream input, File outputRoot)
    throws IOException {
    String relativePath = input.readUTF();

    File outputFile = new File(outputRoot, relativePath);

    File parent = outputFile.getParentFile();

    if (parent != null) parent.mkdirs();

    int chunks = input.readInt();

    try (
      BufferedOutputStream output = new BufferedOutputStream(
        new FileOutputStream(outputFile)
      )
    ) {
      for (int i = 0; i < chunks; i++) {
        byte mode = input.readByte();
        int originalLength = input.readInt();
        int storedLength = input.readInt();
        byte[] data = input.readNBytes(storedLength);
        if (mode == 0) output.write(data);
        else if (mode == 1) {
          byte[] decoded = LZ77Decoder.decode(data, originalLength);
          output.write(decoded);
        } else throw new IOException("Unknown block mode");
      }
    }
  }

  private FileDeSquasher() {}
}
