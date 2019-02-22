package scaffold;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import org.jooq.lambda.Unchecked;

class ScaffoldMain {
  public static void main(String[] args) {
    Arrays.stream(Path.of(".").toFile().listFiles())
        .map(Unchecked.function(File::getCanonicalPath))
        .forEach(System.out::println);
  }
}
