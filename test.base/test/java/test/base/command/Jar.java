package test.base.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

record Jar(Mode mode, Path file, List<Argument> arguments) implements ToolCall<Jar> {

  public enum Mode {
    CREATE,
    LIST,
    UPDATE,
    EXTRACT,
    DESCRIBE_MODULE
  }

  public static Jar create(String file) {
    return new Jar(Mode.CREATE, Path.of(file), List.of());
  }

  public Jar withMode(Mode mode) {
    return new Jar(mode, file, arguments);
  }

  public Jar withFile(Path file) {
    return new Jar(mode, file, arguments);
  }

  @Override
  public Jar with(List<Argument> arguments) {
    if (this.arguments == arguments) return this;
    return new Jar(mode, file, List.copyOf(arguments));
  }

  public Jar withMainClass(String mainClass) {
    return with("--main-class", mainClass);
  }

  @Override
  public List<String> toStrings() {
    var strings = new ArrayList<String>();
    strings.add("--" + mode.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    if (file != null) {
      strings.add("--file");
      strings.add(file.toString());
    }
    strings.addAll(ToolCall.super.toStrings());
    return List.copyOf(strings);
  }
}
