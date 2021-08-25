package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor.Version;
import org.junit.jupiter.api.Test;

class VersionSupportTests {

  @Test
  void numberOne() {
    var one = Version.parse("1");
    assertEquals("1", one.toString());
    assertEquals("1", VersionSupport.components(one).toNumber());
    assertEquals("1", VersionSupport.toNumberAndPreRelease(one));
  }

  @Test
  void number() {
    assertEquals("1", VersionSupport.components(Version.parse("1")).toNumber());
    assertEquals("1", VersionSupport.components(Version.parse("1+2")).toNumber());
    assertEquals("1", VersionSupport.components(Version.parse("1-2")).toNumber());
    assertEquals("1", VersionSupport.components(Version.parse("1+2+3")).toNumber());
    assertEquals("1", VersionSupport.components(Version.parse("1-2+3")).toNumber());
  }

  @Test
  void numberAndPreRelease() {
    assertEquals("1", VersionSupport.components(Version.parse("1")).toNumberAndPreRelease());
    assertEquals("1+2", VersionSupport.components(Version.parse("1+2")).toNumberAndPreRelease());
    assertEquals("1-2", VersionSupport.components(Version.parse("1-2")).toNumberAndPreRelease());
    assertEquals("1+2", VersionSupport.components(Version.parse("1+2+3")).toNumberAndPreRelease());
    assertEquals("1-2", VersionSupport.components(Version.parse("1-2+3")).toNumberAndPreRelease());
  }
}
