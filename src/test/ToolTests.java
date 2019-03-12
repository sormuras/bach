import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToolTests {

  @Nested
  class Format {

    @Test
    @SwallowSystem
    void version(SwallowSystem.Streams streams) throws Exception {
      var bach = new Bach(true, Path.of(""));
      bach.properties.setProperty(Bach.Property.RUN_REDIRECT_TYPE.key, "FILE");

      Bach.Tool.format(bach, "--version");
      assertLinesMatch(
          List.of("format([--version])", ">> INSTALL >>", "Running tool in a new process: .+"),
          streams.outLines());
      assertLinesMatch(
          List.of("google-java-format: Version 1.7"),
          Files.readAllLines(Path.of(bach.get(Bach.Property.RUN_REDIRECT_FILE))));
    }
  }

  @Nested
  class Maven {

    @Test
    @SwallowSystem
    void version(SwallowSystem.Streams streams) throws Exception {
      var bach = new Bach(true, Path.of(""));
      bach.properties.setProperty(Bach.Property.RUN_REDIRECT_TYPE.key, "FILE");

      Bach.Tool.maven(bach, "--version");
      assertLinesMatch(
          List.of("maven([--version])", ">> INSTALL >>", "Running tool in a new process: .+"),
          streams.outLines());
      assertLinesMatch(
          List.of(
              "\\QApache Maven 3.6.0 (97c98ec64a1fdfee7767ce5ffb20918da4f719f3; 2018-10-24T\\E.+",
              ">> MORE VERSIONS >>"),
          Files.readAllLines(Path.of(bach.get(Bach.Property.RUN_REDIRECT_FILE))));
    }
  }
}
