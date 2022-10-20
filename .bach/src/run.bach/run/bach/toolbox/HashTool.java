package run.bach.toolbox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import run.bach.Bach;
import run.bach.ToolOperator;
import run.bach.internal.PathSupport;

public record HashTool(String name) implements ToolOperator {
  public HashTool() {
    this("hash");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    var cli = new CLI().withParsingCommandLineArguments(arguments);
    if (cli.help()) {
      bach.info("%s [--algorithm <name>] [--expected <value>] path <more...>".formatted(name()));
      return;
    }
    var algorithm = cli.__algorithm().orElse("SHA-256");
    var expected = cli.__expected();
    var paths = cli.paths();
    if (paths.isEmpty()) {
      throw new IllegalArgumentException("At least one path expect, but got none.");
    }
    if (expected.isPresent()) {
      var size = paths.size();
      if (size != 1) throw new IllegalArgumentException("Single file expected, but got: " + size);
      checkHash(paths.get(0), algorithm, expected.get());
      return;
    }
    if (paths.size() == 1) {
      var path = paths.get(0);
      if (Files.isRegularFile(path)) {
        bach.info(PathSupport.checksum(path, algorithm));
        return;
      }
    }
    var files = new ArrayList<Path>();
    for (var path : paths) {
      if (Files.isDirectory(path)) {
        files.addAll(PathSupport.list(path, Files::isRegularFile));
        continue;
      }
      if (Files.isRegularFile(path)) {
        files.add(path);
        continue;
      }
      throw new RuntimeException("Path not found: " + path);
    }
    bach.info(renderHash(null, algorithm.toUpperCase(Locale.ROOT)));
    for (var file : files) bach.info(renderHash(file, algorithm));
  }

  static void checkHash(Path file, String algorithm, String expected) {
    var computed = PathSupport.checksum(file, algorithm);
    if (computed.equalsIgnoreCase(expected)) return;
    throw new RuntimeException(
        """
            Hash mismatch detected!
                   file: %s
              algorithm: %s
               computed: %s
               expected: %s
            """
            .formatted(file, algorithm, computed, expected));
  }

  static String renderHash(Path file, String algorithm) {
    var header = file == null;
    var hash = header ? "Hash [" + algorithm + "]" : PathSupport.checksum(file, algorithm);
    var size = header ? "Size [Bytes]" : PathSupport.checksum(file, "SIZE");
    var name = header ? "Path" : file.normalize().toString().replace('\\', '/');
    return "%-64s %12s %s".formatted(hash, size, name);
  }

  record CLI(
      Optional<Boolean> __help,
      Optional<String> __algorithm,
      Optional<String> __expected,
      List<Path> paths) {
    CLI() {
      this(Optional.empty(), Optional.empty(), Optional.empty(), List.of());
    }

    boolean help() {
      return __help.orElse(false);
    }

    CLI withParsingCommandLineArguments(List<String> args) {
      var arguments = new ArrayDeque<>(args);
      // extract
      var help = __help.orElse(null);
      var algorithm = __algorithm.orElse(null);
      var expected = __expected.orElse(null);
      var paths = new ArrayList<>(paths());
      // handle
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
          if (key.equals("--algorithm")) {
            algorithm = sep == -1 ? arguments.removeFirst() : argument.substring(sep + 1);
            continue;
          }
          if (key.equals("--expected")) {
            expected = sep == -1 ? arguments.removeFirst() : argument.substring(sep + 1);
            continue;
          }
        }
        /* treat first unhandled argument as path */ {
          paths.add(Path.of(argument));
          break;
        }
      }
      arguments.stream().map(Path::of).forEach(paths::add);
      // compose
      return new CLI(
          Optional.ofNullable(help),
          Optional.ofNullable(algorithm),
          Optional.ofNullable(expected),
          List.copyOf(paths));
    }
  }
}
