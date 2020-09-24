package test.base;

import java.text.MessageFormat;
import java.util.Deque;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class CollectingLogger implements System.Logger {

  private final String name;
  private final Deque<Entry> entries;

  public CollectingLogger(String name) {
    this.name = name;
    this.entries = new ConcurrentLinkedDeque<>();
  }

  @Override
  public String getName() {
    return name;
  }

  public Deque<Entry> getEntries() {
    return entries;
  }

  public List<Entry> getEntries(Level level) {
    return entries.stream().filter(entry -> entry.is(level)).collect(Collectors.toList());
  }

  @Override
  public boolean isLoggable(Level level) {
    return true;
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
    entries.add(new Entry(level, msg, thrown));
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String format, Object... params) {
    var message = params != null ? MessageFormat.format(format, params) : format;
    entries.add(new Entry(level, message, null));
  }

  public static final class Entry {
    private final Level level;
    private final String message;
    private final Throwable thrown;

    public Entry(Level level, String message, Throwable thrown) {
      this.level = level;
      this.message = message;
      this.thrown = thrown;
    }

    public boolean is(Level level) {
      return this.level == level;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Entry.class.getSimpleName() + "[", "]")
          .add("level=" + level)
          .add("message='" + message + "'")
          .add("thrown=" + thrown)
          .toString();
    }
  }
}
