package run.duke;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A printer of messages.
 *
 * @param out a writer to which "expected" output should be written
 * @param err a writer to which any error messages should be written
 */
public record ToolLogger(String name, PrintWriter out, PrintWriter err) implements System.Logger {
  public ToolLogger {
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(out, "out must not be null");
    Objects.requireNonNull(err, "err must not be null");
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
  public void log(Level level, ResourceBundle bundle, String key, Throwable thrown) {
    if (isLoggable(level)) {
      if (bundle != null) {
        key = getString(bundle, key);
      }
      if (thrown == null) {
        out.println(key);
        return;
      }
      err.println(key);
      thrown.printStackTrace(err);
    }
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String format, Object... params) {
    if (isLoggable(level)) {
      if (bundle != null) {
        format = getString(bundle, format);
      }
      if (params == null || params.length == 0) {
        out.println(format);
        return;
      }
      out.println(MessageFormat.format(format, params));
    }
  }

  public void debug(Object message) {
    log(Level.DEBUG, String.valueOf(message));
  }

  public void error(Object message) {
    log(Level.ERROR, String.valueOf(message));
  }

  public void error(Object message, Throwable thrown) {
    log(Level.ERROR, String.valueOf(message), thrown);
  }

  public void log(Object message) {
    log(Level.INFO, String.valueOf(message));
  }

  private static String getString(ResourceBundle bundle, String key) {
    if (bundle == null || key == null) return key;
    if (bundle.containsKey(key)) return bundle.getString(key);
    return key;
  }
}
