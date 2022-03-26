package com.github.sormuras.bach.tools;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

record BachCaller(PrintWriter out, PrintWriter err, ToolProvider caller) {

  BachCaller {
    Objects.requireNonNull(out);
    Objects.requireNonNull(err);
    Objects.requireNonNull(caller);
  }

  BachCaller(PrintWriter out, PrintWriter err) {
    this(out, err, (ToolProvider) System.getProperties().get("ToolProvider(bach-call)"));
  }

  void call(String tool, Object... arguments) {
    var args = Stream.concat(Stream.of(tool), Stream.of(arguments).map(Object::toString));
    var code = caller.run(out, err, args.toArray(String[]::new));
    if (code == 0) return;
    throw new RuntimeException("Non-zero exit code: " + code);
  }
}
