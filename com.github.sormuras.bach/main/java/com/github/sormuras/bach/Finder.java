package com.github.sormuras.bach;

import com.github.sormuras.bach.lookup.ExternalModuleLookup;
import com.github.sormuras.bach.lookup.Maven;
import com.github.sormuras.bach.lookup.ModuleLookup;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** A finder of external modules. */
public record Finder(ModuleLookup... lookups) {

  public static Finder empty() {
    return new Finder();
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
      return Finder.this.with(new ExternalModuleLookup(module, uri));
    }
  }

  public Linker link(String module) {
    return new Linker(module);
  }

  public Finder with(UnaryOperator<Finder> operator) {
    return operator.apply(this);
  }

  public Finder with(Finder that) {
    if (that.isEmpty()) return this;
    var copy = Arrays.copyOf(lookups, lookups.length + that.lookups.length);
    System.arraycopy(that.lookups, 0, copy, lookups.length, that.lookups.length);
    return new Finder(copy);
  }

  public Finder with(ModuleLookup lookup, ModuleLookup... more) {
    var copy = Arrays.copyOf(lookups, lookups.length + 1 + more.length);
    copy[lookups.length] = lookup;
    if (more.length > 0) System.arraycopy(more, 0, copy, lookups.length + 1, more.length);
    return new Finder(copy);
  }
}
