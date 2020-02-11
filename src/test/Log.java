import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Log implements System.Logger, Consumer<String>, Bach.Listener {

  private final Collection<Entry> entries = new ConcurrentLinkedQueue<>();

  @Override
  public void accept(String message) {
    entries.add(new Entry("P", Instant.now(), Level.ALL, message, null));
  }

  @Override
  public void executionBegin(Bach.Task task) {
    entries.add(new Entry("E", Instant.now(), Level.ALL, "BEGIN " + task.toMarkdown(), null));
  }

  @Override
  public void executionEnd(Bach.Task task, Bach.Task.Result result) {
    entries.add(new Entry("E", Instant.now(), Level.ALL, "END " + task.toMarkdown(), null));
  }

  @Override
  public String getName() {
    return "Log";
  }

  @Override
  public boolean isLoggable(Level level) {
    return true;
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
    entries.add(new Entry("L", Instant.now(), level, msg, thrown));
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String format, Object... params) {
    entries.add(new Entry("L", Instant.now(), level, MessageFormat.format(format, params), null));
  }

  public List<String> lines() {
    return entries.stream().map(e -> e.source + " " + e.message).collect(Collectors.toList());
  }

  public static final class Entry {
    private final String source;
    private final Instant instant;
    private final Level level;
    private final String message;
    private final Throwable thrown;

    public Entry(String source, Instant instant, Level level, String message, Throwable thrown) {
      this.source = source;
      this.instant = instant;
      this.level = level;
      this.message = message;
      this.thrown = thrown;
    }
  }
}
