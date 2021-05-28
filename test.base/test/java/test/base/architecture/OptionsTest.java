package test.base.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.Character.UnicodeScript;
import java.util.List;
import org.junit.jupiter.api.Test;

class OptionsTest {

  @Test
  void a() {
    assertNull(Options.of().a());
    assertNull(Options.of("--a", null).a());
    assertFalse(Options.of("--a", "false").a());
    assertTrue(Options.of("--a", "true").a());
  }

  @Test
  void b() {
    assertEquals(List.of("1"), Options.of("--b", "1").b());
    assertNull(Options.of("--b", "1").with("--b", null).b());
    assertEquals(List.of("1,2"), Options.of("--b", "1,2").b());
    assertEquals(List.of("1", "2"), Options.of("--b", "1").with("--b", "2").b());
    assertEquals(List.of("1", "2"), Options.of().with("--b", "1", "2").b());
  }

  @Test
  void c() {
    assertEquals(99, Options.of("--c", "99").c());
  }

  @Test
  void d() {
    assertEquals(9.9, Options.of("--d", "9.9").d());
  }

  @Test
  void e() {
    assertEquals(List.of(UnicodeScript.RUNIC), Options.of("--e", "RUNIC").e());
  }

  @Test
  void f() {
    assertEquals(List.of("a"), Options.of("--f", "a").f());
  }

  @Test
  void g() {
    assertEquals("g", Options.of("--g", "g").g());
  }

  @Test
  void abcdefff() {
    var options =
        Options.of(
            "--a", "true", "--b", "S", "--c", "1", "--d", "2.3", "--e", "RUNIC", "--f", "Hello",
            "World", "!");
    assertTrue(options.a());
    assertEquals(List.of("S"), options.b());
    assertEquals(1, options.c());
    assertEquals(2.3, options.d());
    assertEquals(List.of(UnicodeScript.RUNIC), options.e());
    assertEquals(List.of("Hello", "World", "!"), options.f());
  }

  @Test
  void underlay() {
    var options =
        Options.of()
            .underlay(
                Options.of("--a", "true", "--g", "Lorem\nipsum"),
                Options.of("--b", "S"),
                Options.of("--c", "1"),
                Options.of("--d", "2.3"),
                Options.of("--e", "RUNIC"),
                Options.of("--f", "Hello", "--f", "World", "--f", "!"));
    assertTrue(options.a());
    assertEquals(List.of("S"), options.b());
    assertEquals(1, options.c());
    assertEquals(2.3, options.d());
    assertEquals(List.of(UnicodeScript.RUNIC), options.e());
    assertEquals(List.of("Hello", "World", "!"), options.f());
    assertLinesMatch(
            """
            Lorem
            ipsum
            """.lines(),
            options.g().lines());
  }
}
