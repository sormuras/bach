package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;

import java.io.PrintWriter;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToIntBiFunction;
import java.util.spi.ToolProvider;

public interface BachToolProvider
    extends ToIntBiFunction<BiConsumer<String, List<String>>, List<String>>, ToolProvider {

  @Override
  default int applyAsInt(BiConsumer<String, List<String>> runner, List<String> arguments) {
    var api = new BachAPI() {

      final Project project = new Project();

      @Override
      public void run(Command command) {
        runner.accept(command.name(), command.arguments());
      }

      @Override
      public Project project() {
        return project;
      }
    };
    return run(api, arguments);
  }

  @Override
  default int run(PrintWriter out, PrintWriter err, String... args) {
    throw new UnsupportedOperationException("Use run(BachAPI, List) instead?");
  }

  int run(BachAPI bach, List<String> arguments);
}
