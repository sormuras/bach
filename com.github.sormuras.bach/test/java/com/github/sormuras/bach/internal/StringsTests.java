package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor.Version;
import org.junit.jupiter.api.Test;

class StringsTests {

  @Test
  void toNumberAndPreRelease() {
    assertEquals("1", Strings.toNumberAndPreRelease(Version.parse("1")));
    assertEquals("1-ea", Strings.toNumberAndPreRelease(Version.parse("1-ea")));
    assertEquals("1-ea", Strings.toNumberAndPreRelease(Version.parse("1-ea+2")));
    assertEquals("1+ea", Strings.toNumberAndPreRelease(Version.parse("1+ea+3")));
  }
}
