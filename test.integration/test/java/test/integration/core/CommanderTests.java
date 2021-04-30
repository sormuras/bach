package test.integration.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.tool.Javac;
import java.lang.module.ModuleFinder;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import test.integration.Auxiliary;

class CommanderTests {

  @Test
  void defaultToolProviderStream() {
    var bach = Auxiliary.newEmptyBach();
    var providers = bach.streamToolProviders().toList();
    assertLinesMatch(
        """
        >> HEAD >>
        jar
        >> TOOLS >>
        jpackage
        >> TAIL >>
        """.lines(),
        providers.stream().map(ToolProvider::name).sorted());
  }

  @Test
  void oneCommand() {
    var bach = Auxiliary.newEmptyBach();
    var result = bach.run(new Javac().with("--version"));
    assertDoesNotThrow(result::requireSuccessful);
  }

  @Test
  void oneCommandViaCustomModuleFinder() {
    var bach = Auxiliary.newEmptyBach();
    var command = new Javac().with("--version");
    var finder = ModuleFinder.of();
    var result = bach.run(command, finder);
    assertDoesNotThrow(result::requireSuccessful);
  }

  @Test
  void threeCommandsViaVarArgs() {
    var bach = Auxiliary.newEmptyBach();
    var javac = new Javac().with("--version");
    var results = bach.run(javac, javac, javac);
    assertDoesNotThrow(results::requireSuccessful);
    assertEquals(3, results.list().size());
  }

  @Test
  void threeCommandsViaSequentialStream() {
    var bach = Auxiliary.newEmptyBach();
    var javac = new Javac().with("--version");
    var stream = Stream.of(javac, javac, javac).sequential();
    var results = bach.run(stream);
    assertDoesNotThrow(results::requireSuccessful);
    assertEquals(3, results.list().size());
  }

  @Test
  void threeCommandsViaParallelStream() {
    var bach = Auxiliary.newEmptyBach();
    var javac = new Javac().with("--version");
    var stream = Stream.of(javac, javac, javac).parallel();
    var results = bach.run(stream);
    assertDoesNotThrow(results::requireSuccessful);
    assertEquals(3, results.list().size());
  }
}
