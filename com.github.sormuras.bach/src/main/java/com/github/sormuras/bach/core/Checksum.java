package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.PathSupport;
import java.io.PrintWriter;
import java.nio.file.Path;

public class Checksum implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    if (args.length < 1 || args.length > 3) {
      err.println("Usage: checksum FILE [ALGORITHM [EXPECTED-CHECKSUM]]");
      return 1;
    }
    var path = Path.of(args[0]);
    var algorithm = args.length > 1 ? args[1] : "SHA-256";
    var computed = PathSupport.computeChecksum(path, algorithm);
    if (args.length == 1 || args.length == 2) {
      out.printf("%s %s%n", computed, path);
      return 0;
    }
    var expected = args[2];
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
