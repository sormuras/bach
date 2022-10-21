package run.bach;

import java.util.List;
import java.util.spi.ToolProvider;

@FunctionalInterface
public interface ToolRunner {
  void run(ToolCall call);

  default void run(Class<?> tool, String... args) {
    run(name(tool), args);
  }

  default void run(Class<?> tool, ToolTweak composer) {
    run(name(tool), composer);
  }

  default void run(String tool, String... args) {
    run(new ToolCall(tool, List.of(args)));
  }

  default void run(String tool, ToolTweak composer) {
    run(new ToolCall(tool).withTweaks(List.of(composer)));
  }

  private static String name(Class<?> type) {
    try {
      if (ToolOperator.class.isAssignableFrom(type)) {
        return ((ToolOperator) type.getConstructor().newInstance()).name();
      }
      if (ToolProvider.class.isAssignableFrom(type)) {
        return ((ToolProvider) type.getConstructor().newInstance()).name();
      }
      throw new IllegalArgumentException("Type not supported: " + type);
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException(exception);
    }
  }
}
