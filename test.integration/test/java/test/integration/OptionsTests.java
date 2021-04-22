package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.Option;
import org.junit.jupiter.api.Test;

class OptionsTests {

  @Test
  void empty() {
    assertTrue(Options.of().map().isEmpty());
    assertTrue(Options.of("<empty>").map().isEmpty());
  }

  @Test
  void title() {
    var options = Options.of();
    assertEquals("title@0", options.title());
    assertNotEquals("title@0", Options.of().title());
  }

  @Test
  void compose() {
    var logbook = Logbook.ofNullPrinter();
    var defaultValues = Options.ofDefaultValues();
    var options =
        Options.compose(
            "test options",
            logbook,
            Options.of("top-most layer"),
            Options.of("second layer").with(Option.VERBOSE),
            defaultValues);

    assertTrue(options.is(Option.VERBOSE), options.toString());
    assertLinesMatch(
        """
        Compose options from 3 components
        [0] = top-most layer with 0 elements
        [1] = second layer with 1 element
        [2] = default values with %d elements
        >>>>
        [1] --verbose true
        >>>>
        """
            .formatted(defaultValues.map().size())
            .lines(),
        logbook.lines());
  }

  @Test
  void overrideNonRepeatableOption() {
    var a = Options.of().with(Option.CHROOT, "a");
    assertEquals("overrideNonRepeatableOption@0", a.title());
    var b = a.with(Option.CHROOT, "b");
    assertEquals("a", a.get(Option.CHROOT));
    assertEquals("b", b.get(Option.CHROOT));
  }

  @Test
  void linesOfAllAvailableFlags() {
    var flags = Options.of("Flags");
    for (var option : Option.values()) {
      if (option.isFlag()) flags = flags.with(option);
    }
    assertLinesMatch(
        """
        --verbose
        --version
        --help
        --help-extra
        --show-configuration
        """
            .lines(),
        flags.lines());
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
        """
            .lines(),
        options.lines(Option::isExtra));
  }

  @Test
  void linesOfRepeatableOptions() {
    var options =
        Options.of("Repeatable Options")
            .with(Option.ACTION, "a1")
            .with(Option.ACTION, "a2")
            .with(Option.ACTION, "a3")
            .with(Option.EXTERNAL_MODULE_LOCATION, "m1", "l1");
    assertLinesMatch(
        """
        --external-module-location
          m1
          l1
        --action
          a1
        --action
          a2
        --action
          a3
        """
            .lines(),
        options.lines(Option::isRepeatable));
  }

  @Test
  void linesOfAllDefaultsAndWithSomeMore() {
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
