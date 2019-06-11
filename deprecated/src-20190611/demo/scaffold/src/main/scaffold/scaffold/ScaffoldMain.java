package scaffold;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import javax.script.ScriptEngineManager;
import org.jooq.lambda.Unchecked;

class ScaffoldMain {
  public static void main(String[] args) {
    listFiles(Path.of(".")).forEach(System.out::println);
    listScriptEngines().forEach(System.out::println);
  }

  // Uses "Unchecked" from "org.jooq.lambda" package provided by "org.jooq.jool" module.
  static Stream<?> listFiles(Path path) {
    return Arrays.stream(path.toFile().listFiles()).map(Unchecked.function(File::getCanonicalPath));
  }

  static Stream<?> listScriptEngines() {
    return new ScriptEngineManager().getEngineFactories().stream();
  }
}
