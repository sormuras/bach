package com.github.sormuras.bach.core;

import com.github.sormuras.bach.internal.ArgVester;
import com.github.sormuras.bach.internal.PathSupport;
import java.nio.file.Path;
import java.security.Security;
import java.util.List;
import java.util.Optional;

public record Checksum(
    Optional<Boolean> help,
    Optional<Boolean> list_algorithms,
    Optional<String> algorithm,
    Optional<String> expected,
    List<String> files)
    implements ArgVester.Program {

  @Override
  public void main(ArgVester.Printer printer) {
    if (list_algorithms().orElse(false)) {
      Security.getAlgorithms("MessageDigest").stream().sorted().forEach(printer::out);
      return;
    }
    if (files().isEmpty()) {
      printer.err("No file passed, run with --help for usage information");
      return;
    }
    var algorithm = algorithm().orElse("SHA-256");
    if (expected().isEmpty()) {
      for (var file : files()) {
        var computed = PathSupport.computeChecksum(Path.of(file), algorithm);
        printer.out("%s %s".formatted(computed, file));
      }
      return;
    }
    var file = Path.of(files().get(0));
    var computed = PathSupport.computeChecksum(file, algorithm);
    var expected = expected().get();
    if (computed.equalsIgnoreCase(expected)) return;
    throw new RuntimeException(
        """
        Checksum mismatch detected!
               file: %s
          algorithm: %s
           computed: %s
           expected: %s
        """
            .formatted(file, algorithm, computed, expected));
  }

  public static void main(String... args) {
    var code = new Tool().run(System.out, System.err, args);
    if (code == 0) return;
    System.exit(code);
  }

  public record Tool(Class<Checksum> meta, String name) implements ArgVester.Tool<Checksum> {
    public Tool() {
      this(Checksum.class, "checksum");
    }
  }
}
