package it;

import de.sormuras.bach.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class Recorder {

  private static List<String> lines(StringWriter writer) {
    return List.copyOf(writer.toString().lines().collect(Collectors.toList()));
  }

  static Optional<String> findMethodName(int skip) {
    return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
        .walk(frames -> frames.skip(skip).findFirst().map(StackWalker.StackFrame::getMethodName));
  }

  final String caption;
  final Log log;
  final StringWriter out, err;

  Recorder() {
    this(findMethodName(2).orElse("?"), new StringWriter(), new StringWriter());
  }

  private Recorder(String caption, StringWriter out, StringWriter err) {
    this.caption = caption;
    this.log = new Log(new PrintWriter(out, true), new PrintWriter(err, true), true);
    this.out = out;
    this.err = err;
  }

  List<String> lines() {
    return lines(out);
  }

  List<String> errors() {
    return lines(err);
  }

  @Override
  public String toString() {
    return "Recorder{" + "caption=" + caption + ", out=" + out + ", err=" + err + '}';
  }
}
