package compressor.iterator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class List {

  ArrayList<File> files = new ArrayList<>();

  private void depthFirstSearch(File file, String base_dir) {
    File[] children = file.listFiles();
    if (children == null) return;
    for (File pointer : children) {
      if (pointer.isFile()) {
        System.out.printf(
          "Found file %s staged to be squashed\n",
          pointer.toString().replace(base_dir, "")
        );
        files.add(pointer);
      } else depthFirstSearch(pointer, base_dir);
    }
  }

  public List(File file, String base_dir) {
    depthFirstSearch(file, base_dir);
  }

  public ArrayList<File> getFiles() {
    return this.files;
  }
}
