package compressor.engine;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class FileReader {

  private ArrayList<File> files;
  private String base_dir;
  private DataOutputStream dos;
  private Map<Byte, String> embeddings;

  public FileReader(
    ArrayList<File> files,
    String base_dir,
    DataOutputStream dos,
    Map<Byte, String> embeddings
  ) {
    this.files = files;
    this.base_dir = base_dir;
    this.dos = dos;
    this.embeddings = embeddings;
  }

  public void mapEmbeddingsAndWriteToSquash() throws Exception {
    for (File file : this.files) {
      String fileName = file.toString().replace(base_dir, "");
      byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
      this.dos.writeInt(fileNameBytes.length);
      this.dos.write(fileNameBytes);
      int bitBuffer = 0, bitCount = 0;
      FileInputStream fis = new FileInputStream(file);
      byte[] arr = fis.readAllBytes();
      this.dos.writeInt(arr.length); // original buffer length
      ArrayList<Byte> compressed = new ArrayList<>();
      for (byte b : arr) {
        String code = this.embeddings.get(b);
        if (code == null) {
          fis.close();
          throw new RuntimeException("Missing embedding for byte " + b);
        }
        for (char ch : code.toCharArray()) {
          bitBuffer = (bitBuffer << 1) | (ch == '1' ? 1 : 0);
          bitCount++;
          if (bitCount == 8) {
            compressed.add((byte) bitBuffer);

            bitBuffer = 0;
            bitCount = 0;
          }
        }
      }
      int paddingBits = 0;
      if (bitCount > 0) {
        paddingBits = 8 - bitCount;
        bitBuffer <<= paddingBits;
        compressed.add((byte) bitBuffer);
      }

      this.dos.writeInt(compressed.size()); // compressed bits length
      this.dos.writeInt(paddingBits); // extra padding len
      for (Byte com : compressed) this.dos.write(com);

      System.out.printf("File %s compressed\n", fileName);

      fis.close();
    }
  }
}
