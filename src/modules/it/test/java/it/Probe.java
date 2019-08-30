package it;

import de.sormuras.bach.Bach;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

class Probe extends Bach {

  final StringWriter out;

  Probe(Path home) {
    this(home, home);
  }

  private Probe(Path home, Path work) {
    this(new StringWriter(), home, work);
  }

  private Probe(StringWriter out, Path home, Path work) {
    super(new PrintWriter(out), new PrintWriter(System.err), home, work);
    this.out = out;
  }

  List<String> lines() {
    return out.toString().lines().collect(Collectors.toList());
  }
}
