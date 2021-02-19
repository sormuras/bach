package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.internal.StreamGobbler;
import com.github.sormuras.bach.lookup.Maven;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

public record GoogleJavaFormat(String version, List<Argument> arguments)
    implements Command<GoogleJavaFormat>, ToolInstaller, ToolProvider {

  public GoogleJavaFormat() {
    this("1.9", List.of());
  }

  public GoogleJavaFormat version(String version) {
    return new GoogleJavaFormat(version, arguments);
  }

  @Override
  public GoogleJavaFormat arguments(List<Argument> arguments) {
    return new GoogleJavaFormat(version, arguments);
  }

  @Override
  public void install(Bach bach) throws Exception {
    var uri = Maven.central("com.google.googlejavaformat", "google-java-format", version, "all-deps");
    var dir = bach.base().directory(".bach","external-tools", "google-java-format", version);
    var jar = dir.resolve("google-java-format@" + version + ".jar");
    if (Files.exists(jar)) return;
    Files.createDirectories(dir);
    bach.browser().load(uri, jar);
  }

  @Override
  public String name() {
    return "google-java-format";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var dir = Path.of(".bach","external-tools", "google-java-format", version);
    var jar = dir.resolve("google-java-format@" + version + ".jar");
    if (!Files.exists(jar)) {
      err.println("File not found: " + jar);
      return -2;
    }
    var java = Command.of("java")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
        .add("-jar", jar.toString());
    var builder = new ProcessBuilder(java.toStrings());
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
