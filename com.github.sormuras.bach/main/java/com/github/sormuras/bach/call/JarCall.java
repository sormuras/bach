package com.github.sormuras.bach.call;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record JarCall(List<String> arguments) implements CallWith<JarCall> {

  public JarCall() {
    this(List.of());
  }

  @Override
  public String name() {
    return "jar";
  }

  @Override
  public JarCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JarCall(arguments);
  }

  public JarCall with(Path path) {
    if (Files.isDirectory(path)) return with("-C", path, ".");
    return with(path.toString());
  }
}
