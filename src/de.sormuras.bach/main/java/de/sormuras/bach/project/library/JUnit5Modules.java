package de.sormuras.bach.project.library;

import de.sormuras.bach.project.Locator;

/** Abstract base class for modules published by the JUnit 5 team. */
/*static*/ abstract class JUnit5Modules extends Locator.AbstractLocator {

  private final String group;
  private final String version;

  JUnit5Modules(String group, String version) {
    this.group = group;
    this.version = version;
  }

  void put(String suffix, long size, String md5) {
    var module = group + suffix;
    var artifact = module.substring(4).replace('.', '-');
    var gav = String.join(":", group, artifact, version);
    var central = Maven.central(gav, module, size, md5);
    put(module, central);
  }
}
