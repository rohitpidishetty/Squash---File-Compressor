// package squash.archive;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Squash {

  protected static PriorityQueue<CharFrequency> freq;
  protected static HashMap<Character, Integer> map;
  protected static Map<Character, String> encodings;
  protected static Map<String, CompressedPayload> compressedFiles =
    new LinkedHashMap<>();

  static {
    freq = new PriorityQueue<>((a, b) -> a.freq - b.freq);
    map = new HashMap<>();
  }

  private static boolean updateFrequency(String word) {
    int len = word.length();
    for (int i = 0; i < len; i++) {
      char ch = word.charAt(i);
      map.put(ch, map.getOrDefault(ch, 0) + 1);
    }
    map.put('\n', map.getOrDefault('\n', 0) + 1);
    return true;
  }

  private void generateCharFreq() {
    for (Map.Entry<Character, Integer> m : map.entrySet()) {
      freq.offer(new CharFrequency(m.getKey(), m.getValue(), NodeType.LEAF));
    }
  }

  private static void scan(File file) {
    String[] buffer = file.list();
    String root = file.getPath().replace("\\", "/");

    for (String next : buffer) {
      File subFile = new File(root.concat("/").concat(next));
      if (next.matches(".*\\.(class|jar|exe|o|obj|dll|so)$")) {
        System.out.println("Cannot squash object file: " + next);
        continue;
      } else if (subFile.isDirectory()) scan(subFile);
      else {
        try {
          String file_path = subFile.toString();
          FileReader fileRead = new FileReader(subFile);
          BufferedReader buffRead = new BufferedReader(fileRead);
          String line;
          while ((line = buffRead.readLine()) != null) updateFrequency(line);

          fileRead.close();
          buffRead.close();
          System.out.printf("Compressing file %s.%n", file_path);
        } catch (Exception e) {
          System.err.println(
            "Error compressing data: " +
            e.getLocalizedMessage() +
            ". Please ensure the files contain only 8-bit characters (0-255)."
          );
        }
      }
    }
  }

  class CompressedPayload {

    protected byte[] bytes;
    protected int validBitsAtLast;

    public CompressedPayload(byte[] bytes, int validBitsAtLast) {
      this.bytes = bytes;
      this.validBitsAtLast = validBitsAtLast;
    }
  }

  private CompressedPayload mapEncodings(String data) {
    StringBuilder binary = new StringBuilder();
    int length = data.length();
    for (int i = 0; i < length; i++) binary.append(
      encodings.get(data.charAt(i))
    );
    length = binary.length();
    byte[] buffer = new byte[(length + 7) / 8];
    int index = 0;
    int validBitsAtLast = length % 8;
    if (validBitsAtLast == 0) validBitsAtLast = 8;
    for (int i = 0; i < length; i += 8) {
      int offset = Math.min(i + 8, length);
      String __byte__ = binary.substring(i, offset);

      buffer[index++] = (byte) Integer.parseInt(__byte__, 2);
    }

    return new CompressedPayload(buffer, validBitsAtLast);
  }

  private static void applyCompressionEmbeddings(File file, Squash sq) {
    String[] buffer = file.list();
    String root = file.getPath().replace("\\", "/");

    for (String next : buffer) {
      File subFile = new File(root.concat("/").concat(next));
      if (next.matches(".*\\.(class|jar|exe|o|obj|dll|so)$")) {
        System.out.println("Cannot squash object file: " + next);
        continue;
      } else if (subFile.isDirectory()) applyCompressionEmbeddings(subFile, sq);
      else {
        try {
          String file_path = subFile.toString();
          FileReader fileRead = new FileReader(subFile);
          BufferedReader buffRead = new BufferedReader(fileRead);
          String line;
          StringBuilder __buffer__ = new StringBuilder();
          while ((line = buffRead.readLine()) != null) {
            __buffer__.append(line);
            __buffer__.append('\n');
          }
          CompressedPayload payload = sq.mapEncodings(__buffer__.toString());
          compressedFiles.put(file_path, payload);
          fileRead.close();
          buffRead.close();
          System.out.printf("Compressed file %s successfully.%n", file_path);
        } catch (Exception e) {
          System.err.println(
            "Error compressing data: " +
            e.getLocalizedMessage() +
            ". Please ensure the files contain only 8-bit characters (0-255)."
          );
        }
      }
    }
  }

  private static void writeSQSH(
    String targetFile,
    Map<String, CompressedPayload> files,
    Map<Character, String> encodings
  ) {
    try {
      FileOutputStream fos = new FileOutputStream(targetFile);
      DataOutputStream dos = new DataOutputStream(fos);

      // 1. Header
      dos.write("SQSH".getBytes(StandardCharsets.US_ASCII));

      // 2. Total no. of files
      dos.writeInt(compressedFiles.size());

      // 3. Writing each file
      for (Map.Entry<
        String,
        CompressedPayload
      > e : compressedFiles.entrySet()) {
        byte[] pathBytes = e.getKey().getBytes(StandardCharsets.UTF_8);
        dos.writeInt(pathBytes.length);
        dos.write(pathBytes);

        CompressedPayload payload = e.getValue();
        dos.writeInt(payload.bytes.length);
        dos.writeByte(payload.validBitsAtLast);
        dos.write(payload.bytes);
      }

      // 4. Writing encodings
      dos.writeInt(encodings.size());
      for (Map.Entry<Character, String> entry : encodings.entrySet()) {
        dos.writeChar(entry.getKey());
        byte[] codeBytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
        dos.writeInt(codeBytes.length);
        dos.write(codeBytes);
      }
      dos.close();
      System.out.printf("SQSH archived to %s successfully", targetFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void decompress(String file) throws Exception {
    FileInputStream fis = new FileInputStream(file);
    DataInputStream dis = new DataInputStream(fis);
    byte[] header = new byte[4];
    dis.readFully(header);
    String headerStr = new String(header, StandardCharsets.US_ASCII);
    if (!headerStr.equals("SQSH")) {
      dis.close();
      throw new IllegalStateException("Invalid SQSH file");
    }
    int totalFiles = dis.readInt();
    System.out.printf("Decompressing %s\n", file);
    System.out.printf("Total file detected in %s are %d\n", file, totalFiles);

    HashMap<String, String> buffer = new HashMap<>();

    for (int i = 0; i < totalFiles; i++) {
      int pathLen = dis.readInt();
      byte[] pathBytes = new byte[pathLen];
      dis.readFully(pathBytes);
      String path = new String(pathBytes, StandardCharsets.UTF_8);

      int payloadLen = dis.readInt();
      int validBitsAtLast = dis.readByte();
      byte[] payload = new byte[payloadLen];
      dis.readFully(payload);
      StringBuilder binary = new StringBuilder();
      for (int b = 0; b < payload.length; b++) {
        int value = payload[b] & 0xFF; // Signed to Unsigned
        String bits = String.format(
          "%8s",
          Integer.toBinaryString(value)
        ).replace(' ', '0');
        binary.append(bits);
      }

      int totalBits = ((payload.length - 1) * 8) + validBitsAtLast;
      binary.setLength(totalBits);
      buffer.put(path, binary.toString());
    }
    int len = dis.readInt();
    Map<String, Character> decodeMap = new HashMap<>();

    for (int i = 0; i < len; i++) {
      char ch = dis.readChar();
      int codeLen = dis.readInt();
      byte[] codeBytes = new byte[codeLen];
      dis.readFully(codeBytes);
      decodeMap.put(new String(codeBytes, StandardCharsets.UTF_8), ch);
    }
    for (Map.Entry<String, String> writeable : buffer.entrySet()) {
      String path = writeable.getKey();
      System.out.printf("Decompressing file %s\n", path);
      patchToFile(path, getContent(writeable.getValue(), decodeMap));
      System.out.printf("Decompressed %s\n", path);
    }
    dis.close();
  }

  private static void patchToFile(String file_path, String originalContent) {
    System.out.printf("Creating %s\n", file_path);
    String[] path_tokens = file_path.split("\\\\");
    ArrayDeque<String> path = new ArrayDeque<>();
    File folder;
    for (int i = 0; i < path_tokens.length; i++) path.offer(path_tokens[i]);
    StringBuffer FILE_PATH = new StringBuffer();
    while (!path.isEmpty()) {
      String dir = path.poll();
      if (dir.matches("[.]+$")) continue;
      if (dir.matches("[a-zA-Z0-9 _\\-\\(\\)\\[\\]]+")) {
        FILE_PATH.append(dir).append("//");
        folder = new File(FILE_PATH.toString());
        if (!folder.exists()) folder.mkdir();
      } else if (dir.matches("[a-zA-Z0-9 _\\-\\(\\)\\[\\]\\.]+")) {
        try {
          FILE_PATH.append(dir);
          folder = new File(FILE_PATH.toString());
          FileWriter fileWriter = new FileWriter(folder);
          BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
          bufferedWriter.write(originalContent);
          bufferedWriter.close();
          fileWriter.close();
        } catch (Exception e) {}
      }
    }
  }

  private static String getContent(String bits, Map<String, Character> map) {
    int streamLen = bits.length();
    StringBuilder decompressedText = new StringBuilder();
    StringBuilder key = new StringBuilder();
    for (int i = 0; i < streamLen; i++) {
      key.append(bits.charAt(i));
      String KEY = key.toString();
      if (map.containsKey(KEY)) {
        decompressedText.append(map.get(KEY));
        key.setLength(0);
      }
    }
    return decompressedText.toString();
  }

  public static void main(String[] args) {
    if (args[0].length() == 0 || args[1].length() == 0) {
      System.err.println(
        """
        squash -compress origin_path target_file_name
        squash -decompress target.tar.sq
        """
      );
      return;
    }
    if (args[0].equals("-compress")) {
      if (
        args.length < 3 || args[1].trim().isEmpty() || args[2].trim().isEmpty()
      ) {
        System.err.println(
          """
          Usage:
          squash -compress origin_path target_file_name
          """
        );
        return;
      }

      String path = args[1];
      File file = new File(path);
      if (!file.exists()) {
        System.out.printf("Unable to locate file %s\n", path);
        return;
      }
      Encoder encoder = new Encoder();
      Squash sq = new Squash();
      scan(file);
      sq.generateCharFreq();
      encoder.generateTree(freq);
      encodings = encoder.generateEncodings();
      System.out.printf(
        "Encodings generated for %d unique characters.%n",
        encodings.size()
      );
      applyCompressionEmbeddings(file, sq);
      writeSQSH(args[2] + ".tar.sq", compressedFiles, encodings);
    } else if (args[0].equals("-decompress")) {
      if (args.length < 2 || !args[1].toLowerCase().endsWith(".tar.sq")) {
        System.err.println(
          """
          Usage:
          squash -decompress target.tar.sq
          """
        );
        return;
      }
      try {
        new Squash().decompress(args[1]);
        System.out.printf("Decompression successful\n");
      } catch (Exception e) {
        System.out.println(e);
      }
    }
  }
}
