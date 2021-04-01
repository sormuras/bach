package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.lookup.Maven;
import java.nio.file.Path;
import java.util.List;

public record GoogleJavaFormat(Path jar, List<String> arguments)
    implements ExecutableJar<GoogleJavaFormat> {

  public static GoogleJavaFormat install(Bach bach) {
    return install(bach, "1.10.0");
  }

  public static GoogleJavaFormat install(Bach bach, String version) {
    var group = "com.google.googlejavaformat";
    var artifact = "google-java-format";
    var uri = Maven.central(group, artifact, version, "all-deps");
    var jar = ExecutableJar.load(bach, artifact, version, uri);
    return new GoogleJavaFormat(jar, List.of());
  }

  @Override
  public GoogleJavaFormat arguments(List<String> arguments) {
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
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
        .add("--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");
  }
}
