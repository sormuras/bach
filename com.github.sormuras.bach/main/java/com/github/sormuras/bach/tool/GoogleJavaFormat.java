package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;

public record GoogleJavaFormat(List<Argument> arguments) implements Command<GoogleJavaFormat>, ToolProvider {

  public GoogleJavaFormat() {
    this(List.of());
  }

  @Override
  public GoogleJavaFormat arguments(List<Argument> arguments) {
    return new GoogleJavaFormat(arguments);
  }

  @Override
  public String name() {
    return "google-java-format";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println("Download Google Java Format");
    out.println("Call java -jar ... with args = " + List.of(args));
    return 0;
  }
}
