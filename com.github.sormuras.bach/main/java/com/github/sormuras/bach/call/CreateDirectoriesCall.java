package com.github.sormuras.bach.call;

import com.github.sormuras.bach.Call;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

public record CreateDirectoriesCall(Path path) implements Call, ToolProvider {

  @Override
  public String name() {
    return "create-directories";
  }

  @Override
  public List<String> arguments() {
    return List.of(path.toString());
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      Files.createDirectories(path);
      return 0;
    } catch (IOException exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }
}
