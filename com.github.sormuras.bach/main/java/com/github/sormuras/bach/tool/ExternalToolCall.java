package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.internal.Strings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record ExternalToolCall(String name, Path jar, List<String> arguments)
    implements ExecutableJar<ExternalToolCall> {

  @Override
  public JavaCall java() {
    var javaArgs = jar.resolveSibling("java.args");
    if (Files.notExists(javaArgs)) return new JavaCall();
    return new JavaCall().withAll(Strings.arguments(javaArgs));
  }

  @Override
  public ExternalToolCall arguments(List<String> arguments) {
    return new ExternalToolCall(name, jar, arguments);
  }
}
