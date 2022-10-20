package run.bach.toolbox;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import run.bach.Bach;
import run.bach.ExternalAssetsRepository;
import run.bach.ToolCall;
import run.bach.ToolOperator;
import run.bach.internal.PathSupport;

public record InstallTool(String name) implements ToolOperator {

  public InstallTool() {
    this("install");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    var cli = new CLI().withParsingCommandLineArguments(arguments);
    if (cli.help()) {
      bach.info("Usage: %s [--from <repository>] <tools...>".formatted(name()));
      return;
    }
    var index = ExternalAssetsRepository.Info.EXTERNAL_TOOL_DIRECTORY;
    var from = cli.from();
    var names = cli.names();

    bach.debug("Install tool directory info file from repository: %s".formatted(from.home()));
    if (names.isEmpty() || names.contains("?")) {
      var walker = ExternalAssetsRepository.walk(bach.browser().client(), from);
      var tools = walker.map().get(index);
      if (tools == null || tools.isEmpty()) {
        bach.info("No tool directory index files found in " + from);
        return;
      }
      var joiner = new StringJoiner("\n");
      for (var tool : tools) {
        var command = ToolCall.of("bach");
        command = command.with(name()); // "install"
        if (cli.__from().isPresent()) command = command.with("--from", cli.__from().get());
        command = command.with(index.name(tool));
        joiner.add(command.toCommandLine(" "));
      }
      var size = tools.size();
      joiner.add("    %d tool directory info fil%s".formatted(size, size == 1 ? "" : "s"));
      bach.info(joiner.toString());
      return;
    }

    for (var name : cli.names()) {
      var source = from.source(index, name);
      var target = bach.paths().externalTools().resolve(name + index.extension());
      acquireToolDirectory(bach, source, target);
      explodeToolDirectory(bach, target);
    }
  }

  void acquireToolDirectory(Bach bach, String source, Path target) {
    bach.run("load", "file", source, target.toString());
  }

  void explodeToolDirectory(Bach bach, Path file) {
    var properties = PathSupport.properties(file);
    var name = file.getFileName().toString();
    var extension = ExternalAssetsRepository.Info.EXTERNAL_TOOL_DIRECTORY.extension();
    var parent = file.resolveSibling(name.substring(0, name.length() - extension.length()));
    for (var key : properties.stringPropertyNames()) {
      if (key.startsWith("@")) continue;
      var value = properties.getProperty(key);
      if (!value.startsWith("http")) {
        bach.debug("Unsupported protocol: " + value);
        continue;
      }
      var source = URI.create(value);
      var target = parent.resolve(key);
      bach.run("load", "file", source.toString(), target.toString());
    }
  }

  record CLI(Optional<Boolean> __help, Optional<String> __from, List<String> names) {
    CLI() {
      this(Optional.empty(), Optional.empty(), List.of());
    }

    boolean help() {
      return __help.orElse(false);
    }

    ExternalAssetsRepository from() {
      return __from.map(ExternalAssetsRepository::of).orElse(ExternalAssetsRepository.DEFAULT);
    }

    CLI withParsingCommandLineArguments(List<String> args) {
      var arguments = new ArrayDeque<>(args);
      var help = __help.orElse(null);
      var from = __from.orElse(null);
      var names = new ArrayList<>(names());
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        /* parse flags */ {
          if (run.bach.CLI.HELP_FLAGS.contains(argument)) {
            help = Boolean.TRUE;
            continue;
          }
        }
        /* parse key-value pairs */ {
          int sep = argument.indexOf('=');
          var key = sep == -1 ? argument : argument.substring(0, sep);
          if (key.equals("--from")) {
            from = sep == -1 ? arguments.removeFirst() : argument.substring(sep + 1);
            continue;
          }
        }
        // restore argument because first unhandled option marks the beginning of the tool names
        arguments.addFirst(argument);
        break;
      }
      // parse variadic elements from remaining arguments
      names.addAll(arguments);
      // compose from components
      return new CLI(Optional.ofNullable(help), Optional.ofNullable(from), List.copyOf(names));
    }
  }
}
