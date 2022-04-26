package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.StringSupport;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;

public class LoadAndVerify implements ToolOperator {
  @Override
  public String name() {
    return "load-and-verify";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    bach.run("load", args[0], args[1]);

    var target = Path.of(args[0]);
    var calls = new ArrayList<ToolCall>();
    Consumer<String> checker =
        string -> {
          var property = StringSupport.parseProperty(string);
          var algorithm = property.key();
          var expected = property.value();
          calls.add(ToolCall.of("checksum", target, algorithm, expected));
        };

    var index = 2;
    while (index < args.length) checker.accept(args[index++]);

    if (!args[1].startsWith("string:")) {
      var fragment = URI.create(args[1]).getFragment();
      var elements = fragment == null ? new String[0] : fragment.split("&");
      for (var element : elements) checker.accept(element);
    }

    if (calls.isEmpty()) throw new IllegalStateException("No expected checksum given.");

    calls.stream().parallel().forEach(bach::run);
    return 0;
  }
}
