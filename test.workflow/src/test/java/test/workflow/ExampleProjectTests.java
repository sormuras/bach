package test.workflow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Printer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExampleProjectTests {

  @ParameterizedTest
  @MethodSource
  void buildExampleProject(Path example, @TempDir Path temp) throws Exception {
    var bach =
        Bach.of(
            Printer.ofSilence(),
            args ->
                args.with("--verbose")
                    .with("--root-directory", example)
                    .with("--output-directory", temp));
    assertDoesNotThrow(() -> bach.run("build"), bach.configuration().printer()::toString);
    var init = !bach.project().spaces().init().modules().list().isEmpty();
    if (init && OS.WINDOWS.isCurrentOs()) {
      System.gc(); // try to release file handles and...
      Thread.sleep(123); // hope JAR files are not locked...
    }
  }

  static List<Path> buildExampleProject() {
    var projects = Path.of("test.workflow/example-projects");
    try (var stream = Files.newDirectoryStream(projects, Files::isDirectory)) {
      return StreamSupport.stream(stream.spliterator(), false).toList();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
