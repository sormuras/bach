package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class Load implements ToolOperator {

  record Options(Optional<Boolean> reload, Path target, String source) {
    static final String USAGE = """
        Usage: load [--reload] TARGET SOURCE
        """;

    static Optional<Options> of(String... args) {
      if (args.length < 2 || args.length > 3) return Optional.empty();
      var reload = "--reload".equals(args[0]);
      return Optional.of(
          new Options(
              reload ? Optional.of(Boolean.TRUE) : Optional.empty(),
              Path.of(args[reload ? 1 : 0]),
              args[reload ? 2 : 1]));
    }
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var optionalOptions = Options.of(args);
    if (optionalOptions.isEmpty()) {
      err.println(Options.USAGE);
      return 1;
    }
    var options = optionalOptions.get();
    var reload = options.reload().orElse(Boolean.FALSE);
    var target = options.target();
    if (Files.exists(target) && !reload) return 0;

    var source = options.source();
    if (source.startsWith("string:")) {
      try {
        var parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        var string = source.substring(7);
        Files.writeString(target, string);
        out.printf("Wrote %,12d %s%n", string.length(), target.getFileName());
        return 0;
      } catch (Exception exception) {
        exception.printStackTrace(err);
        return 2;
      }
    }

    var paths = bach.configuration().paths();
    var from = URI.create(source);
    var scheme = from.getScheme();
    var uri = scheme == null ? paths.root(source).normalize().toAbsolutePath().toUri() : from;
    out.printf("Loading %s...%n", uri);
    try (var stream = uri.toURL().openStream()) {
      var parent = target.getParent();
      if (parent != null) Files.createDirectories(parent);
      var size = Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
      out.printf("Loaded %,12d %s%n", size, target.getFileName());
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 3;
    }
  }
}
