package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.tool.ExecutableJar;
import java.nio.file.Path;
import java.util.List;

public record PomChecker(Path jar, List<Argument> arguments) implements ExecutableJar<PomChecker> {

  public static PomChecker install(Bach bach) {
    var version = "1.1.0-SNAPSHOT";
    var file = "pomchecker-toolprovider-" + version + ".jar";
    var uri = "https://github.com/kordamp/pomchecker/releases/download/early-access/" + file;
    var jar = ExecutableJar.load(bach, "kordamp-pomchecker", version, uri);
    return new PomChecker(jar, List.of());
  }

  @Override
  public PomChecker arguments(List<Argument> arguments) {
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
