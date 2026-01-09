import compressor.engine.FileReader;
import compressor.engine.QueueBuilder;
import compressor.engine.TreeBuilder;
import compressor.engine.TreeNodeTuple;
import compressor.iterator.List;
import compressor.std_target_output.SquashFileWriter;
import decompressor.engine.SquashReader;
import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.PriorityQueue;

public class Squash {

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println(
        """
        Enter the following command to compress
        ./squash -compress ~\\folder-path target-name

        To decompress
        ./squash -decompress ~\\target-name.tar.sq
        """
      );
      return;
    }
    if (args[0].equals("-compress")) {
      if (args.length != 3) {
        System.out.println(
          """
          Enter the following command to compress
          ./squash -compress ~\\folder-path target-name
          """
        );
        return;
      }
      File file = new File(args[1]);
      if (!file.exists()) {
        System.err.printf(
          "%s not found, make sure the path is valid\n",
          args[1]
        );
      }
      try {
        String TARGET_FILE = args[2] + ".tar.sq";
        File targetFile = new File(TARGET_FILE);
        targetFile.createNewFile();
        SquashFileWriter sqfw = new SquashFileWriter(targetFile, TARGET_FILE);
        sqfw.commit_header();
        String base_dir;
        ArrayList<File> files = new List(
          file,
          base_dir = args[1] + "\\"
        ).getFiles();

        Map<Byte, String> embeddings = new TreeBuilder(
          new QueueBuilder(files).getQueue()
        ).generateEmbeddings();

        int map_size = embeddings.size();
        DataOutputStream dos = sqfw.getWriter();
        dos.writeInt(map_size);
        for (Map.Entry<Byte, String> em : embeddings.entrySet()) {
          dos.writeByte(em.getKey());
          dos.writeUTF(em.getValue());
        }
        new FileReader(
          files,
          base_dir = args[1] + "\\",
          dos,
          embeddings
        ).mapEmbeddingsAndWriteToSquash();
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    } else if (args[0].equals("-decompress")) {
      if (args.length != 2) {
        System.out.println(
          """
          Enter the following command to compress
          ./squash -decompress ~\\target-name.tar.sq
          """
        );
        return;
      }
      File sqFile = new File(args[1]);
      if (!sqFile.exists()) {
        throw new Error("Squash file " + args[1] + " does not exist");
      }
      try {
        String[] folderTokens = args[1].split("[.]");
        new SquashReader(sqFile).readAndWriteFile(
          "unsquashed_" +
            (folderTokens[folderTokens.length - 3]).replaceAll("/", "")
        );
      } catch (Exception e) {
        System.out.println(e.getLocalizedMessage());
      }
    }
  }
}
