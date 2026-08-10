import compressor.engine.FileSquasher;
import decompressor.engine.FileDeSquasher;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Squash {

  private static String FILENAME_REGEX = "^[A-Za-z0-9 _()-]+$";
  private static String PWD = ".";
  private static String CLEAN = ".class";

  public static void main(String[] args) {
    if (args.length == 0) showSquashUsage();

    switch (args[0]) {
      case "-squash":
        if (args.length != 4) showSquashUsage();
        File targetFilePath = new File(args[1]);
        String squashFileName = args[2];
        File squashOutputPath = new File(args[3]);
        if (!targetFilePath.exists()) {
          throwError("[ERROR] Intended file not found.");
        }
        if (!Pattern.matches(FILENAME_REGEX, squashFileName)) {
          throwError("[ERROR] Invalid filename.");
        }
        if (squashOutputPath.getName().contains(".")) {
          throwError("[ERROR] Invalid output file path.");
        }
        if (!squashOutputPath.exists()) squashOutputPath.mkdirs();
        try (
          FileOutputStream fos = new FileOutputStream(
            new File(squashOutputPath, squashFileName.concat(".sq"))
          );
          DataOutputStream dos = new DataOutputStream(fos);
        ) {
          // ############### (VERSION CONFLICT) ##################
          dos.writeUTF(versioning.Squash.header); // Squash Program Version
          dos.writeInt(versioning.Squash.versionId);
          // ############### (VERSION CONFLICT) ##################
          if (targetFilePath.isFile()) {
            System.out.println("[INFO] Squashing File..");
            /*
             * File type:
             * 0 = file
             * 1 = directory
             */
            dos.writeByte(0);
            FileSquasher.compress(
              targetFilePath.getName(),
              targetFilePath,
              false,
              dos
            );
          } else {
            // DFS & -squash every file
            System.out.println("[INFO] Squashing Files..");
            dos.writeByte(1);
            // System.out.println(Arrays.toString(targetFilePath.list()));
            // System.exit(1);
            FileSquasher.depthFirstSearchAllFilesAndCompress(
              targetFilePath.getName(),
              targetFilePath,
              dos,
              false
            );
          }
        } catch (Exception e) {}

        System.out.println("[INFO] Squashing completed.");

        break;
      case "-desquash":
        if (args.length != 3) showSquashUsage();

        File squashFile = new File(args[1]);
        File outputPath = new File(args[2]);

        if (!squashFile.exists() || !squashFile.isFile()) {
          throwError("[ERROR] Squash file not found.");
        }
        if (outputPath.getName().contains(".")) {
          throwError("[ERROR] Invalid output path.");
        }
        if (!outputPath.exists()) {
          outputPath.mkdirs();
        }
        System.out.println("[INFO] DeSquashing..");
        try (
          FileInputStream fis = new FileInputStream(squashFile);
          BufferedInputStream bis = new BufferedInputStream(fis);
          DataInputStream dis = new DataInputStream(bis)
        ) {
          FileDeSquasher.decompress(dis, outputPath);
        } catch (Exception e) {
          e.printStackTrace();
          throwError("[ERROR] Could not desquash file.");
        }
        System.out.println("[INFO] DeSquashing completed.");
        break;
      case "-clean":
        cleanClassFiles(new File(PWD));
        break;
      default:
        showSquashUsage();
        break;
    }
  }

  private static void showSquashUsage() {
    System.err.println("Invalid arguments");
    System.out.println(
      """
      java Squash -squash <file-path | folder> <squash-as-name> <output-path>
      java Squash -desquash <squash-file> <output-path>

      Example:
      java Squash -squash "C:\\Users\\<YourName>\\Pictures\\Screenshots 1\\Image.png" squashed "C:\\Users\\<YourName>\\Desktop"
      java Squash -desquash "C:\\Users\\<YourName>\\OneDrive\\Desktop\\test\\pic.sq" "C:\\Users\\<YourName>\\OneDrive\\Desktop\\test"
      """
    );
    System.exit(1);
  }

  private static void throwError(String message) {
    System.err.println(message);
    System.exit(404);
  }

  public static void cleanClassFiles(File file) {
    if (file.isFile()) {
      if (file.getName().endsWith(CLEAN)) {
        try {
          Files.delete(Paths.get(file.getAbsolutePath()));
        } catch (Exception e) {
          System.out.println(
            "IOException " + e.getCause().getLocalizedMessage()
          );
        }
      }
      return;
    }
    for (File subFile : file.listFiles()) cleanClassFiles(subFile);
  }
}
