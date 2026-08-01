package compressor.std_target_output;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class SquashFileWriter {

  public File tar;
  protected FileOutputStream fos;
  protected DataOutputStream dos;

  public SquashFileWriter(File tar, String TARGET_FILE) throws Exception {
    this.tar = tar;
    this.fos = new FileOutputStream(TARGET_FILE);
    this.dos = new DataOutputStream(fos);
  }

  public void commit_header() throws Exception {
    this.dos.writeInt(6);
    this.dos.writeBytes("squash");
    this.dos.writeInt(9);
    this.dos.writeBytes("--version");
    this.dos.writeInt(1);
  }

  public DataOutputStream getWriter() {
    return this.dos;
  }
}
