import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

class CollectingLogger implements System.Logger {

  private final String name;
  private final List<String> lines = new ArrayList<>();

  CollectingLogger(String name) {
    this.name = name;
  }

  void clear() {
    lines.clear();
  }

  List<String> getLines() {
    return lines;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isLoggable(Level level) {
    return true;
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
    lines.add(msg);
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String format, Object... params) {
    var msg = params != null && params.length > 0 ? MessageFormat.format(format, params) : format;
    lines.add(msg);
  }

  @Override
  public String toString() {
    return "Logger " + name + " with " + lines.size() + " lines:\n" + String.join("\n", lines);
  }
}
