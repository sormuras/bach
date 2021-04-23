package test.integration.api;

import static com.github.sormuras.bach.api.Option.Modifier.EXTRA;
import static com.github.sormuras.bach.api.Option.Modifier.REPEATABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.Option;
import com.github.sormuras.bach.api.UnsupportedOptionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OptionTests {

  @Test
  void chroot() {
    var option = Option.CHROOT;
    assertSame(option, Option.ofCli("--chroot"));
    assertEquals("--chroot", option.cli());
    assertEquals(1, option.cardinality());
    assertFalse(option.isFlag());
    assertTrue(option.is(EXTRA));
    assertFalse(option.is(REPEATABLE));
    assertEquals(".", option.defaultValue().orElseThrow().elements().get(0));
  }

  @Test
  void version() {
    var option = Option.VERSION;
    assertSame(option, Option.ofCli("--version"));
    assertEquals("--version", option.cli());
    assertEquals(0, option.cardinality());
    assertTrue(option.isFlag());
    assertFalse(option.is(EXTRA));
    assertFalse(option.is(REPEATABLE));
    assertEquals(Option.Value.of("false"), option.defaultValue().orElseThrow());
  }

  @Test
  void externalModuleLocation() {
    var option = Option.EXTERNAL_MODULE_LOCATION;
    assertSame(option, Option.ofCli("--external-module-location"));
    assertEquals("--external-module-location", option.cli());
    assertEquals(2, option.cardinality());
    assertFalse(option.isFlag());
    assertFalse(option.is(EXTRA));
    assertTrue(option.is(REPEATABLE));
    assertTrue(option.defaultValue().isEmpty());
  }

  @Test
  void action() {
    var option = Option.ACTION;
    assertSame(option, Option.ofCli("--action"));
    assertEquals("--action", option.cli());
    assertEquals(1, option.cardinality());
    assertFalse(option.isFlag());
    assertFalse(option.is(EXTRA));
    assertTrue(option.is(REPEATABLE));
    assertTrue(option.defaultValue().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "-", "--", "--undefined", "-- chroot"})
  void factoryThrowsOnUnsupportedOption(String string) {
    assertThrows(UnsupportedOptionException.class, () -> Option.ofCli(string));
  }
}
