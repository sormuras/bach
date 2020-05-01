package de.sormuras.bach.util;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

/** An entry-collecting system logger implementation. */
public /*static*/ class Logbook implements System.Logger {

  private final Collection<Entry> entries = new ConcurrentLinkedQueue<>();

  @Override
  public String getName() {
    return "Logbook";
  }

  @Override
  public boolean isLoggable(Level level) {
    return true;
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
    entries.add(new Entry(level, message, thrown));
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String pattern, Object... arguments) {
    entries.add(new Entry(level, MessageFormat.format(pattern, arguments), null));
  }

  public List<String> messages() {
    return lines(Entry::message);
  }

  public List<String> lines(Function<Entry, String> mapper) {
    return entries.stream().map(mapper).collect(Collectors.toList());
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

    public Level level() {
      return level;
    }

    public String message() {
      return message;
    }

    public Throwable thrown() {
      return thrown;
    }
  }
}
