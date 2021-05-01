package test.base.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OptionsTests {

  static final String ALL_STRING =
      """
      --verbose
      --version
      --name
        World
      --state
        terminated
      --state
        new
      """;

  static final Options ALL_OPTIONS =
      Options.empty()
          .with(Cli::verbose, true)
          .with(Cli::version, true)
          .with(Cli::name, Optional.of("World"))
          .with(Cli::states, List.of(Thread.State.TERMINATED, Thread.State.NEW));

  @Test
  void parse() {
    assertEquals(ALL_OPTIONS, Options.parse(ALL_STRING.lines().toArray(String[]::new)));
  }

  @Test
  void lines() {
    assertLinesMatch(ALL_OPTIONS.lines(), ALL_STRING.lines().toList());
  }

  @Test
  void compose() {
    var o1 = Options.empty();
    var o2 = Options.empty().with("verbose", true).with(Thread.State.NEW, Thread.State.BLOCKED);
    var o3 = Options.empty().with("verbose", false).with("name", Optional.of("o3"));
    var composite = Options.compose(o1, o2, o3);
    assertTrue(composite.verbose());
    assertFalse(composite.version());
    assertEquals("o3", composite.name().orElseThrow());
    assertEquals(List.of(Thread.State.NEW, Thread.State.BLOCKED), composite.states());
  }

  @Test
  void help() {
    assertLinesMatch(
        """
        --name NAME
          Set the name.
        --state STATE
          This option is repeatable.
        --verbose
          Print messages about what is going on.
        --version
          Print version information and exit.
        """
            .lines(),
        Options.generateHelpMessage().lines());
  }
}
