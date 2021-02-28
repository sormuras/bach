package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.tool.ExecutableJar;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;

public record PomChecker(Path jar, List<String> arguments) implements ExecutableJar<PomChecker> {

  public static PomChecker install(Bach bach) {
    return install(bach, "1.1.0");
  }

  public static PomChecker install(Bach bach, String version) {
    var uri =
        new StringJoiner("/")
            .add("https://repo.maven.apache.org/maven2")
            .add("org.kordamp.maven".replace('.', '/'))
            .add("pomchecker-toolprovider")
            .add(version)
            .add("pomchecker-toolprovider-" + version + ".jar");
    var jar = ExecutableJar.load(bach, "kordamp-pomchecker", version, uri.toString());
    return new PomChecker(jar, List.of());
  }

  @Override
  public PomChecker arguments(List<String> arguments) {
    return new PomChecker(jar, arguments);
  }

  @Override
  public String name() {
    return "pomchecker";
  }

  public PomChecker checkMavenCentral(Path pom) {
    return add("check-maven-central").add("--file", pom);
  }
}
