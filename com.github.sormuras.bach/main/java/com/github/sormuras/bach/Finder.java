package com.github.sormuras.bach;

import com.github.sormuras.bach.lookup.ExternalModuleLookup;
import com.github.sormuras.bach.lookup.JUnitJupiter;
import com.github.sormuras.bach.lookup.JUnitPlatform;
import com.github.sormuras.bach.lookup.Maven;
import com.github.sormuras.bach.lookup.ModuleLookup;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

/** A finder of external modules. */
public record Finder(ModuleLookup... lookups) {

  public static Finder of(ModuleLookup... lookups) {
    return new Finder(lookups);
  }

  public boolean isEmpty() {
    return lookups.length == 0;
  }

  public record Found(String uri, ModuleLookup by) {}

  public Optional<Found> find(String name) {
    for (var lookup : lookups) {
      var uri = lookup.lookupModule(name);
      if (uri.isPresent()) return Optional.of(new Found(uri.get(), lookup));
    }
    return Optional.empty();
  }

  public class Linker {

    private final String module;

    public Linker(String module) {
      this.module = module;
    }

    public Finder toMaven(
        String group, String artifact, String version, Consumer<Maven.Joiner> consumer) {
      var joiner = Maven.Joiner.of(group, artifact, version);
      consumer.accept(joiner);
      return toUri(joiner.toString());
    }

    public Finder toMavenCentral(String group, String artifact, String version) {
      return toUri(Maven.central(group, artifact, version));
    }

    public Finder toUri(String uri) {
      return with(new ExternalModuleLookup(module, uri));
    }
  }

  public Linker link(String module) {
    return new Linker(module);
  }

  public Finder with(ModuleLookup lookup, ModuleLookup... more) {
    var copy = Arrays.copyOf(lookups, lookups.length + 1 + more.length);
    copy[lookups.length] = lookup;
    if (more.length > 0) System.arraycopy(more, 0, copy, lookups.length + 1, more.length);
    return new Finder(copy);
  }

  /** Link well-known JUnit modules to their Maven Central artifacts. */
  public enum JUnit implements ModuleLookup {

    /** Link modules of JUnit 5.7.0 to their Maven Central artifacts. */
    V_5_7_0("5.7.0", "1.7.0", "1.1.1", "1.2.0"),
    /** Link modules of JUnit 5.7.1 to their Maven Central artifacts. */
    V_5_7_1("5.7.1", "1.7.1", "1.1.1", "1.2.0");

    private final String version;
    private final Finder finder;

    JUnit(String jupiter, String platform, String apiguardian, String opentest4j) {
      this.version = jupiter;
      this.finder =
          new Finder(new JUnitJupiter(jupiter), new JUnitPlatform(platform))
              .link("org.apiguardian.api")
              .toMavenCentral("org.apiguardian", "apiguardian-api", apiguardian)
              .link("org.opentest4j")
              .toMavenCentral("org.opentest4j", "opentest4j", opentest4j);
    }

    @Override
    public Optional<String> lookupModule(String name) {
      return finder.find(name).map(Finder.Found::uri);
    }

    @Override
    public String toString() {
      return "JUnit " + version;
    }
  }
}
