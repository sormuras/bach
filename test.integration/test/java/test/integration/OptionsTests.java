package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.Option;
import org.junit.jupiter.api.Test;

class OptionsTests {

  @Test
  void empty() {
    assertTrue(Options.of().isEmpty());
  }

  @Test
  void compose() {
    var options =
        Options.compose(Options.of(), Options.of(Option.VERSION, "true"), Options.ofDefaultValues());

    assertTrue(options.is(Option.VERSION), options.toString());
  }

  @Test
  void overrideNonRepeatableOption() {
    var a = Options.of(Option.CHROOT, "a");
    var b = a.with(Option.CHROOT, "b");
    assertEquals("a", a.get(Option.CHROOT));
    assertEquals("b", b.get(Option.CHROOT));
  }

  @Test
  void linesOfAllDefaultFlags() {
    var options = Options.of().with(Option.HELP);
    assertLinesMatch(
        """
        --help
        """.lines(),
        options.lines(Option::isFlag));
  }

  @Test
  void linesOfAllDefaultExtraOptions() {
    var options = Options.ofDefaultValues();
    assertLinesMatch(
        """
        --chroot
          .
        --bach-info-module
          bach.info
        """.lines(),
        options.lines(Option::isExtra));
  }

  @Test
  void linesOfAllDefaultRepeatableOptions() {
    var options = Options.of()
        .with(Option.ACTION, "a")
        .with(Option.EXTERNAL_MODULE_LOCATION, "m1", "l1");
    assertLinesMatch(
        """
        --external-module-location
          m1
          l1
        --action
          a
        """.lines(),
        options.lines(Option::isRepeatable));
  }

  @Test
  void linesOfAllDefaultsAndWithMore() {
    var options =
        Options.ofDefaultValues()
            .with(Option.EXTERNAL_MODULE_LOCATION, "m1", "l1")
            .with(Option.EXTERNAL_MODULE_LOCATION, "m2", "l2")
            .with(Option.EXTERNAL_MODULE_LOCATION, "m3", "l3")
            .with(Action.BUILD);

    assertFalse(options.is(Option.VERSION));
    assertEquals("false", options.get(Option.VERSION));

    assertLinesMatch(
        """
        --chroot
          .
        --bach-info-module
          bach.info
        --project-name
          noname
        --external-module-location
          m1
          l1
        --external-module-location
          m2
          l2
        --external-module-location
          m3
          l3
        --action
          build
        """
            .lines(),
        options.lines());
  }
}
