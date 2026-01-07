package decompressor.engine;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SquashReader {

  protected File file;

  public SquashReader(File file) {
    this.file = file;
  }

  public void readAndWriteFile(String targetFolder) throws Exception {
    FileInputStream fis = new FileInputStream(this.file);
    DataInputStream dInp = new DataInputStream(fis);
    System.out.println("Decompressing..");
    File file = new File(targetFolder);
    if (file.exists()) file.delete();
    file.mkdir();

    if (!new String(dInp.readNBytes(dInp.readInt())).equals("squash")) {
      fis.close();
      dInp.close();
      throw new Error("Squash file is corrupted");
    }

    if (!new String(dInp.readNBytes(dInp.readInt())).equals("--version")) {
      fis.close();
      dInp.close();
      throw new Error("Squash file is corrupted");
    }

    if (dInp.readInt() != 1) {
      fis.close();
      dInp.close();
      throw new Error("Squash version mismatched");
    }

    // Map size
    int map_size = dInp.readInt();
    Map<String, Byte> mappings = new HashMap<>();
    for (int i = 0; i < map_size; i++) {
      byte b = dInp.readByte();
      String embedding = dInp.readUTF();
      mappings.put(embedding, b);
    }

    try {
      while (true) {
        String filename = new String(
          dInp.readNBytes(dInp.readInt()),
          StandardCharsets.UTF_8
        );
        File sub = new File(file, filename);
        File sub_root = sub.getParentFile();
        if (!sub_root.exists()) sub_root.mkdirs();
        sub.createNewFile();
        FileOutputStream fos = new FileOutputStream(sub);

        int originalBufferLen = dInp.readInt();
        int compressedArrLen = dInp.readInt();
        int paddingLen = dInp.readInt();
        System.out.printf("De-squashing %s ", filename);
        int written = 0;
        StringBuilder key = new StringBuilder();
        int i = 0;
        for (; i < compressedArrLen; i++) {
          byte b = (byte) (dInp.readByte() & 0xff);
          int bitsToRead = 8;
          if (i == compressedArrLen - 1 && paddingLen > 0) bitsToRead -=
            paddingLen;
          for (int j = 7; j >= 8 - bitsToRead; j--) {
            boolean is_set_bit = ((b >> j) & 1) == 1;
            key.append(is_set_bit ? '1' : '0');
            String ref = key.toString();
            if (mappings.containsKey(ref)) {
              byte original = mappings.get(ref);
              fos.write((char) (original & 0xff));
              key.setLength(0);
              written++;
              if (written >= originalBufferLen) break;
            }
          }
        }

        fos.close();
        System.out.printf("Successful\n");
      }
    } catch (Exception e) {
      System.out.println("De-squashed all the files");
    }

    fis.close();
    dInp.close();
  }
}
