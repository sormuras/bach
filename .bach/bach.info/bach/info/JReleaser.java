package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.tool.ExecutableJar;
import java.nio.file.Path;
import java.util.List;

public record JReleaser(Path jar, List<String> arguments) implements ExecutableJar<JReleaser> {

  public static JReleaser install(Bach bach) {
    var uri =
        "https://github.com/jreleaser/jreleaser/releases/download/early-access/jreleaser-tool-provider-0.1.0-SNAPSHOT.jar";
    var jar = ExecutableJar.load(bach, "jreleaser", "0.1.0-SNAPSHOT", uri);
    return new JReleaser(jar, List.of());
  }

  @Override
  public JReleaser arguments(List<String> arguments) {
    return new JReleaser(jar, arguments);
  }

  @Override
  public String name() {
    return "jreleaser";
  }
}
