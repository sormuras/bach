import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

class BachContext implements ParameterResolver {

  class Recorder implements BiConsumer<System.Logger.Level, String> {

    class Entry {
      final System.Logger.Level level;
      final String message;

      Entry(System.Logger.Level level, String message) {
        this.level = level;
        this.message = message;
      }
    }

    List<String> all = new CopyOnWriteArrayList<>();
    List<Entry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void accept(System.Logger.Level level, String message) {
      all.add(message);
      entries.add(new Entry(level, message));
    }

    List<String> level(System.Logger.Level level) {
      return entries
          .stream()
          .filter(e -> e.level.getSeverity() >= level.getSeverity())
          .map(e -> e.message)
          .collect(Collectors.toList());
    }
  }

  final Bach bach;
  final Recorder recorder;

  BachContext() {
    this.bach = new Bach();
    this.recorder = new Recorder();

    bach.vars.logger = recorder;
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext unused) {
    var type = parameterContext.getParameter().getType();
    return type.equals(getClass()) || type.equals(Bach.class) || type.equals(Bach.Util.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext unused)
      throws ParameterResolutionException {
    var context = new BachContext();
    var type = parameterContext.getParameter().getType();
    if (type.equals(getClass())) {
      return context;
    }
    if (type.equals(Bach.class)) {
      return context.bach;
    }
    if (type.equals(Bach.Util.class)) {
      return context.bach.util;
    }
    throw new ParameterResolutionException("Can't resolve parameter of type: " + type);
  }

  Stream<Supplier<Integer>> tasks(int size) {
    return IntStream.rangeClosed(1, size).boxed().map(i -> () -> task("" + i));
  }

  int task(String name) {
    return task(name, () -> 0);
  }

  int task(String name, IntSupplier result) {
    bach.info("%s begin", name);
    var millis = (long) (Math.random() * 200 + 50);
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.interrupted();
    }
    bach.info("%s done. %d", name, millis);
    return result.getAsInt();
  }
}
