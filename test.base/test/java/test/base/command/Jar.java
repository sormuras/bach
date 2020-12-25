package test.base.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

record Jar(Mode mode, Path file, List<Argument> arguments) implements Command<Jar> {

  public enum Mode {
    CREATE,
    DESCRIBE_MODULE
  }

  public static Jar create(String file) {
    return new Jar(Mode.CREATE, Path.of(file), List.of());
  }

  @Override
  public String tool() {
    return "jar";
  }

  public Jar mode(Mode mode) {
    if (this.mode == mode) return this;
    return new Jar(mode, file, arguments);
  }

  public Jar file(Path file) {
    if (this.file == file) return this;
    return new Jar(mode, file, arguments);
  }

  @Override
  public Jar arguments(List<Argument> arguments) {
    if (this.arguments == arguments) return this;
    return new Jar(mode, file, List.copyOf(arguments));
  }

  public Jar addMainClass(String mainClass) {
    return add("--main-class", mainClass);
  }

  @Override
  public List<String> toStrings() {
    var strings = new ArrayList<String>();
    strings.add("--" + mode.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    if (file != null) {
      strings.add("--file");
      strings.add(file.toString());
    }
    strings.addAll(Command.super.toStrings());
    return List.copyOf(strings);
  }
}
