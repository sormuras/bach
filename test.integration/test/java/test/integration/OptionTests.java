package test.integration;

import static com.github.sormuras.bach.api.Option.Modifier.EXTRA;
import static com.github.sormuras.bach.api.Option.Modifier.REPEATABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.Option;
import org.junit.jupiter.api.Test;

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
    assertEquals(".", option.defaultValue().origin());
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
    assertSame(Option.Value.FALSE, option.defaultValue());
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
    assertSame(Option.Value.EMPTY, option.defaultValue());
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
    assertSame(Option.Value.EMPTY, option.defaultValue());
  }
}
