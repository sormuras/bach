package com.github.sormuras.bach;

import com.github.sormuras.bach.lookup.JUnitJupiter;
import com.github.sormuras.bach.lookup.JUnitPlatform;
import java.util.function.UnaryOperator;

public class Finders {

  /** Link well-known JUnit modules to their Maven Central artifacts. */
  public enum JUnit implements UnaryOperator<Finder> {

    /** Link modules of JUnit 5.7.0 to their Maven Central artifacts. */
    V_5_7_0("5.7.0", "1.7.0", "1.1.1", "1.2.0"),
    /** Link modules of JUnit 5.7.1 to their Maven Central artifacts. */
    V_5_7_1("5.7.1", "1.7.1", "1.1.1", "1.2.0");

    private final String jupiter;
    private final String platform;
    private final String apiguardian;
    private final String opentest4j;

    JUnit(String jupiter, String platform, String apiguardian, String opentest4j) {
      this.jupiter = jupiter;
      this.platform = platform;
      this.apiguardian = apiguardian;
      this.opentest4j = opentest4j;
    }

    @Override
    public Finder apply(Finder finder) {
      return finder
          .with(new JUnitJupiter(jupiter))
          .with(new JUnitPlatform(platform))
          .link("org.apiguardian.api")
          .toMavenCentral("org.apiguardian", "apiguardian-api", apiguardian)
          .link("org.opentest4j")
          .toMavenCentral("org.opentest4j", "opentest4j", opentest4j);
    }

    @Override
    public String toString() {
      return "JUnit " + jupiter;
    }
  }

  private Finders() {}
}
