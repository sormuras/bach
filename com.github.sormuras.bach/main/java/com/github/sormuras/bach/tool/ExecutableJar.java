package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
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
public interface ExecutableJar<T> extends Command<T>, ToolProvider {

  static Path load(Bach bach, String name, String version, String uri) {
    var dir = bach.folders().externalTools(name, version);
    var jar = dir.resolve(name + "@" + version + ".jar");
    if (Files.exists(jar)) return jar;
    try {
      Files.createDirectories(dir);
      bach.browser().load(uri, jar);
    } catch (Exception exception) {
      throw new RuntimeException("Install failed: " + exception.getMessage());
    }
    return jar;
  }

  Path jar();

  default Java java() {
    return new Java();
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
