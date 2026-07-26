import compressor.engine.FileSquasher;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Squash {

  private static String FILENAME_REGEX = "^[A-Za-z0-9 _()-]+$";

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

        if (targetFilePath.isFile()) {
          FileSquasher.read(targetFilePath, true);
        }

        break;
      case "-desquash":
        if (args.length != 3) showSquashUsage();

        break;
      case "-clean":
        cleanClassFiles(new File("."));
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
      java Squash -squash "C:\\Users\\rohit\\Pictures\\Screenshots 1\\Screenshot 2025-12-29 000431.png" squashed "C:\\Users\\rohit\\Desktop"
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
      if (file.getName().endsWith(".class")) {
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
    for (File f : file.listFiles()) cleanClassFiles(f);
  }
}
