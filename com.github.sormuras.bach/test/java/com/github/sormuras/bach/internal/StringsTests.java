package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor.Version;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class StringsTests {

  @Test
  void durations() {
    assertEquals("0s", Strings.toString(Duration.ZERO));
    assertEquals("0.001s", Strings.toString(Duration.ofMillis(1)));
    assertEquals("0.999s", Strings.toString(Duration.ofMillis(999)));
    assertEquals("1.001s", Strings.toString(Duration.ofMillis(1001)));
    assertEquals("1s", Strings.toString(Duration.ofSeconds(1)));
    assertEquals("59s", Strings.toString(Duration.ofSeconds(59)));
    assertEquals("1m", Strings.toString(Duration.ofSeconds(60)));
    assertEquals("1m 1s", Strings.toString(Duration.ofSeconds(61)));
  }

  @Test
  void toNumberAndPreRelease() {
    assertEquals("1", Strings.toNumberAndPreRelease(Version.parse("1")));
    assertEquals("1-ea", Strings.toNumberAndPreRelease(Version.parse("1-ea")));
    assertEquals("1-ea", Strings.toNumberAndPreRelease(Version.parse("1-ea+2")));
    assertEquals("1+ea", Strings.toNumberAndPreRelease(Version.parse("1+ea+3")));
  }
}
