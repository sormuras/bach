package run.bach.internal;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StringPrintWriter extends PrintWriter {
  public StringPrintWriter() {
    super(new StringWriter());
  }

  @Override
  public String toString() {
    return super.out.toString();
  }
}
