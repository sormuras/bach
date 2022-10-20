package run.bach.toolbox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import run.bach.Bach;
import run.bach.ExternalAssetsRepository;
import run.bach.ToolCall;
import run.bach.ToolOperator;

public record ImportTool(String name) implements ToolOperator {
  public ImportTool() {
    this("import");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    var cli = new CLI().withParsingCommandLineArguments(arguments);
    if (cli.help()) {
      bach.info("Usage: %s [--from <repository>] <locators...>".formatted(name()));
      return;
    }
    var info = ExternalAssetsRepository.Info.EXTERNAL_MODULES_LOCATOR;
    var from = cli.from();
    var names = cli.names();

    bach.debug("Import external modules locators from repository: %s".formatted(from.home()));
    if (names.isEmpty() || names.contains("?")) {
      var walker = ExternalAssetsRepository.walk(bach.browser().client(), from);
      var locators = walker.map().get(info);
      if (locators == null || locators.isEmpty()) {
        bach.info("No external modules locator info file found in " + from);
        return;
      }
      var joiner = new StringJoiner("\n");
      for (var locator : locators) {
        var command = ToolCall.of("bach");
        command = command.with(name()); // "import"
        if (cli.__from().isPresent()) command = command.with("--from", cli.__from().get());
        command = command.with(info.name(locator));
        joiner.add(command.toCommandLine(" "));
      }
      var size = locators.size();
      joiner.add("    %d external modules locator%s".formatted(size, size == 1 ? "" : "s"));
      bach.info(joiner.toString());
      return;
    }

    for (var name : names) {
      var source = from.source(info, name);
      var target = bach.paths().externalModules(name + info.extension());
      bach.run("load-file", source, target.toString());
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
        // restore argument because first unhandled option marks the beginning of the locator names
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
