/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class ModuleFinders {
  public static ModuleReferenceFinder ofReferences() {
    return new ModuleReferenceFinder();
  }

  public static ModuleFinder ofProperties(String properties) {
    return new ModuleReferenceFinder(references(properties));
  }

  public record ModuleReferenceFinder(List<? extends ModuleReference> references)
      implements ModuleFinder {
    public ModuleReferenceFinder() {
      this(List.of());
    }

    @Override
    public Optional<ModuleReference> find(String name) {
      return references.stream()
          .filter(reference -> reference.descriptor().name().equals(name))
          .map(ModuleReference.class::cast)
          .findFirst();
    }

    @Override
    public Set<ModuleReference> findAll() {
      return Set.copyOf(references);
    }

    public ModuleReferenceFinder with(ModuleReference reference) {
      var references = Stream.concat(references().stream(), Stream.of(reference)).toList();
      return new ModuleReferenceFinder(references);
    }

    public ModuleReferenceFinder with(String name, String location) {
      return with(new ExternalModuleReference(name, location));
    }
  }

  private static List<ModuleReference> references(String string) {
    try {
      var properties = new Properties();
      properties.load(new StringReader(string));
      var map = new TreeMap<String, ModuleReference>();
      for (var name : properties.stringPropertyNames()) {
        map.put(name, new ExternalModuleReference(name, properties.getProperty(name)));
      }
      return List.copyOf(map.values());
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static class ExternalModuleReference extends ModuleReference
      implements Comparable<ExternalModuleReference> {
    public ExternalModuleReference(String name, String location) {
      this(name, URI.create(location));
    }

    public ExternalModuleReference(String name, URI location) {
      this(ModuleDescriptor.newModule(name).build(), location);
    }

    public ExternalModuleReference(ModuleDescriptor descriptor, URI location) {
      super(descriptor, location);
    }

    @Override
    public int compareTo(ExternalModuleReference that) {
      return this.descriptor().name().compareTo(that.descriptor().name());
    }

    @Override
    public ModuleReader open() throws IOException {
      var name = descriptor().name();
      var target = Files.createTempDirectory("open-" + name + "-").resolve(name + ".jar");
      copy(target, location().orElseThrow());
      return ModuleFinder.of(target).find(name).orElseThrow().open();
    }

    @Override
    public String toString() {
      return descriptor().name() + " <- " + location().orElseThrow();
    }
  }

  private static void copy(Path target, URI source) {
    if (!Files.exists(target)) {
      try (var stream =
          source.getScheme().startsWith("http")
              ? source.toURL().openStream()
              : Files.newInputStream(Path.of(source))) {
        var parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
    // TODO Verify target bits.
  }

  private ModuleFinders() {}
}
