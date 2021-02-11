package com.github.sormuras.bach;

import com.github.sormuras.bach.lookup.ExternalModuleLookupBuilder;
import com.github.sormuras.bach.lookup.JUnitJupiterModuleLookup;
import com.github.sormuras.bach.lookup.JUnitPlatformModuleLookup;
import com.github.sormuras.bach.lookup.JavaFXModuleLookup;
import com.github.sormuras.bach.lookup.LWJGLModuleLookup;
import com.github.sormuras.bach.lookup.ModuleLookup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * An external module manager.
 *
 * @param requires modules on which the current project has a dependence
 * @param lookups sequence of module lookup objects to query for uris
 */
public record Libraries(Set<String> requires, List<ModuleLookup> lookups) {

  public static Libraries of(ModuleLookup... lookups) {
    return new Libraries(Set.of(), List.of(lookups));
  }

  public static ExternalModuleLookupBuilder lookup(String module) {
    return new ExternalModuleLookupBuilder(module);
  }

  public static ModuleLookup lookupJavaFX(String version) {
    return new JavaFXModuleLookup(version);
  }

  public static ModuleLookup lookupLWJGL(String version) {
    return new LWJGLModuleLookup(version);
  }

  public record Found(String uri, ModuleLookup by) {}

  public Optional<Found> find(String module) {
    for (var lookup : lookups) {
      var uri = lookup.lookupUri(module);
      if (uri.isPresent()) return Optional.of(new Found(uri.get(), lookup));
    }
    return Optional.empty();
  }

  public Libraries withRequires(String module) {
    var requires = new HashSet<>(this.requires);
    requires.add(module);
    return new Libraries(Set.copyOf(requires), lookups);
  }

  public Libraries withModuleLookup(ModuleLookup lookup) {
    var lookups = new ArrayList<>(this.lookups);
    lookups.add(lookup);
    return new Libraries(requires, List.copyOf(lookups));
  }

  /** Find well-known JUnit modules published at Maven Central. */
  public enum JUnit implements ModuleLookup {

    /** Link modules of JUnit 5.7.0 to their Maven Central artifacts. */
    V_5_7_0("5.7.0", "1.7.0", "1.1.1", "1.2.0"),
    /** Link modules of JUnit 5.7.1 to their Maven Central artifacts. */
    V_5_7_1("5.7.1", "1.7.1", "1.1.1", "1.2.0");

    private final String version;
    private final Libraries libraries;

    JUnit(String jupiter, String platform, String apiguardian, String opentest4j) {
      this.version = jupiter;
      this.libraries =
          Libraries.of(
              new JUnitJupiterModuleLookup(jupiter),
              new JUnitPlatformModuleLookup(platform),
              lookup("org.junit.vintage.engine")
                  .viaMavenCentral("org.junit.vintage:junit-vintage-engine:" + jupiter),
              lookup("org.apiguardian.api")
                  .viaMavenCentral("org.apiguardian:apiguardian-api:" + apiguardian),
              lookup("org.opentest4j")
                  .viaMavenCentral("org.opentest4j:opentest4j:" + opentest4j));
    }

    @Override
    public Optional<String> lookupUri(String module) {
      return libraries.find(module).map(Libraries.Found::uri);
    }

    @Override
    public String toString() {
      return "JUnit " + version;
    }
  }
}
