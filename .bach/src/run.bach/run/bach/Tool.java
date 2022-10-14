package run.bach;

import java.util.List;
import java.util.spi.ToolProvider;
import run.bach.internal.NativeProcessToolProvider;

public sealed interface Tool {
  String name();

  default String nick() {
    if (name().endsWith("/")) throw new IllegalStateException(name());
    return name().substring(name().lastIndexOf('/') + 1);
  }

  default boolean matches(String string) {
    return name().equals(string) || name().endsWith('/' + string);
  }

  static Tool ofToolOperator(ToolOperator operator) {
    var name = computeName(operator.name(), operator);
    return new ToolOperatorTool(name, operator);
  }

  static Tool ofToolProvider(ToolProvider provider) {
    var name = computeName(provider.name(), provider);
    return new ToolProviderTool(name, provider);
  }

  static Tool ofNativeProcess(String name, List<String> command) {
    var provider = new NativeProcessToolProvider(name, command);
    return new ToolProviderTool(name, provider);
  }

  private static String computeName(String name, Object object) {
    if (name.indexOf('/') >= 0) return name;
    var module = object.getClass().getModule();
    var prefix = module.isNamed() ? module.getName() : object.getClass().getCanonicalName();
    return prefix + '/' + name;
  }

  record ToolProviderTool(String name, ToolProvider provider) implements Tool {}

  record ToolOperatorTool(String name, ToolOperator operator) implements Tool {}
}
