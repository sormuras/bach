package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.internal.StreamGobbler;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

/**
 * Run a Java program packaged in an executable JAR file.
 *
 * @param <T> the type of the program
 */
public interface ExecutableJar<T extends ToolCall<T>> extends ToolCall<T>, ToolProvider {

  Path jar();

  default JavaCall java() {
    return new JavaCall();
  }

  default ProcessBuilder newProcessBuilder() {
    return new ProcessBuilder("java");
  }

  @Override
  default int run(PrintWriter out, PrintWriter err, String... args) {
    var jar = jar();
    if (!Files.exists(jar)) {
      err.println("File not found: " + jar);
      return -2;
    }
    var builder = newProcessBuilder();
    builder.command().addAll(java().executeJar(jar, args).arguments());
    try {
      var process = builder.start();
      new Thread(new StreamGobbler(process.getInputStream(), out::println)).start();
      new Thread(new StreamGobbler(process.getErrorStream(), err::println)).start();
      return process.waitFor();
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return -1;
    }
  }
}
