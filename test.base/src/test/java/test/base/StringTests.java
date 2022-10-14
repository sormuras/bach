package test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringTests {

  @Test
  void formatWithPrecision() {
    var s10 = "1234567890";
    assertEquals(s10, String.format("%s", s10));
    assertEquals("", String.format("%.0s", s10));
    assertEquals("1", String.format("%.1s", s10));
    assertEquals("123456789", String.format("%.9s", s10));
    assertEquals("1234567890", String.format("%.10s", s10));
    assertEquals("1234567890", String.format("%.11s", s10));
    assertEquals("1234567890", String.format("%.99s", s10));
  }
}
