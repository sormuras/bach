package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.ArgumentsParser;
import com.github.sormuras.bach.internal.PathSupport;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Optional;

public class Checksum implements ToolOperator {
  public record Arguments(Path file, Optional<String> algorithm, Optional<String> expected) {}

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var arguments = ArgumentsParser.create(Arguments.class).parse(args);
    var path = arguments.file();
    var algorithm = arguments.algorithm().orElse("SHA-256");
    var computed = PathSupport.computeChecksum(path, algorithm);
    if (arguments.expected().isEmpty()) {
      out.printf("%s %s%n", computed, path);
      return 0;
    }
    var expected = arguments.expected().get();
    if (computed.equalsIgnoreCase(expected)) return 0;
    err.printf(
        """
        Checksum mismatch detected!
               path: %s
          algorithm: %s
           computed: %s
           expected: %s
        """,
        path, algorithm, computed, expected);
    return 2;
  }
}
