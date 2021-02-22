package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.internal.StreamGobbler;
import com.github.sormuras.bach.lookup.Maven;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

public record GoogleJavaFormat(Path jar, List<Argument> arguments)
    implements Command<GoogleJavaFormat>, ToolProvider {

  public static ToolProvider provider() {
    return install(new Bach(Options.of()));
  }

  public static GoogleJavaFormat install(Bach bach) {
    return install(bach, "1.10-SNAPSHOT");
  }

  public static GoogleJavaFormat install(Bach bach, String version) {
    var uri =
        version.equals("1.10-SNAPSHOT")
            ? "https://oss.sonatype.org/content/repositories/snapshots/com/google/googlejavaformat/google-java-format/1.10-SNAPSHOT/google-java-format-1.10-20210217.055657-9-all-deps.jar"
            : Maven.central(
                "com.google.googlejavaformat", "google-java-format", version, "all-deps");
    var dir = bach.folders().externalTools("google-java-format", version);
    var jar = dir.resolve("google-java-format@" + version + ".jar");
    if (!Files.exists(jar))
      try {
        Files.createDirectories(dir);
        bach.browser().load(uri, jar);
      } catch (Exception exception) {
        throw new RuntimeException("Install failed: " + exception.getMessage());
      }
    return new GoogleJavaFormat(jar, List.of());
  }

  @Override
  public GoogleJavaFormat arguments(List<Argument> arguments) {
    return new GoogleJavaFormat(jar, arguments);
  }

  @Override
  public String name() {
    return "google-java-format";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (!Files.exists(jar)) {
      err.println("File not found: " + jar);
      return -2;
    }
    var java =
        Command.of("java")
            .add("--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
            .add("--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED")
            .add("--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
            .add("--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED")
            .add("--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
            .add("--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
            .add("--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
            .add("-jar", jar.toString());
    var builder = new ProcessBuilder("java");
    builder.command().addAll(java.toStrings());
    builder.command().addAll(List.of(args));
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
