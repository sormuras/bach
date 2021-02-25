package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.lookup.Maven;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

public record GoogleJavaFormat(Path jar, List<Argument> arguments)
    implements ExecutableJar<GoogleJavaFormat> {

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
    var jar = ExecutableJar.load(bach, "google-java-format", version, uri);
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
  public Java java() {
    return new Java()
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");
  }
}
