package com.github.sormuras.bach.tools;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

record Commander(PrintWriter out, PrintWriter err, ToolProvider provider) {

  static Commander of(PrintWriter out, PrintWriter err) {
    var provider = (ToolProvider) System.getProperties().get("ToolProvider(bach-call)");
    return new Commander(out, err, provider);
  }

  void println(String string) {
    out.println(string);
  }

  void execute(String tool, Object... arguments) {
    execute(Command.of(tool, arguments));
  }

  void execute(Command command) {
    var tool = provider != null ? provider : ToolProvider.findFirst(command.name()).orElseThrow();
    var code = tool.run(out, err, command.stream().toArray(String[]::new));
    if (code != 0) throw new RuntimeException("Non-zero exit code: " + code);
  }
}
