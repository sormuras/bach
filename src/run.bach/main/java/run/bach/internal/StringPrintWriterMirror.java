package run.bach.internal;

import java.io.PrintWriter;

public class StringPrintWriterMirror extends StringPrintWriter {
  private final PrintWriter other;

  public StringPrintWriterMirror(PrintWriter other) {
    this.other = other;
  }

  @Override
  public void flush() {
    super.flush();
    other.flush();
  }

  @Override
  public void write(int c) {
    super.write(c);
    other.write(c);
  }

  @Override
  public void write(char[] buf, int off, int len) {
    super.write(buf, off, len);
    other.write(buf, off, len);
  }

  @Override
  public void write(String s, int off, int len) {
    super.write(s, off, len);
    other.write(s, off, len);
  }

  @Override
  public void println() {
    super.println();
    other.println();
  }
}
