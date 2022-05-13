package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.ArgVester;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class Load implements ToolOperator {
  public record Options(Path target, String source, Optional<Boolean> reload) {}

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var options = ArgVester.create(Options.class).parse(args);
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
