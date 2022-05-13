package com.github.sormuras.bach.core;

import com.github.sormuras.bach.internal.ArgVester;
import com.github.sormuras.bach.internal.PathSupport;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Optional;
import java.util.spi.ToolProvider;

public class Checksum implements ToolProvider {
  public record Arguments(Path file, Optional<String> algorithm, Optional<String> expected) {}

  @Override
  public String name() {
    return "checksum";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var arguments = ArgVester.create(Arguments.class).parse(args);
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
