package com.github.sormuras.bach.call;

import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.internal.Paths;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

public record CreateDirectoriesCall(Path path, boolean remake) implements Call, ToolProvider {

  @Override
  public String name() {
    return "create-directories";
  }

  @Override
  public List<String> arguments() {
    return List.of(path.toString(), "--remake", Boolean.toString(remake));
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      if (remake) Paths.deleteDirectories(path);
      Files.createDirectories(path);
      return 0;
    } catch (IOException exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }
}
