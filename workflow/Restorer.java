/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import run.bach.external.ModuleLookupTable;
import run.bach.internal.ModulesSupport;
import run.bach.workflow.Structure.DeclaredModules;
import run.bach.workflow.Structure.Space;

public interface Restorer extends Action {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<ModuleLookupTable> TABLE = new InheritableThreadLocal<>();

  private ModuleLookupTable table() {
    return TABLE.get();
  }

  default void restore() {
    if (TABLE.get() != null) throw new IllegalStateException();
    try {
      TABLE.set(restorerUsesModuleLookupTable());
      say("Restoring required modules ...");
      restoreModules(restorerUsesModuleNames());
      say("Restoring missing modules recursively ...");
      restoreMissingModules();
    } finally {
      TABLE.remove();
    }
  }

  default ModuleLookupTable restorerUsesModuleLookupTable() {
    return ModuleLookupTable.of(restorerUsesLibraryDirectory());
  }

  default void restoreModules(Collection<String> names) {
    var lib = restorerUsesLibraryDirectory();
    try {
      with_next_module:
      for (var name : names) {
        if (ModuleFinder.of(lib).find(name).isPresent()) {
          log("Module %s is already present".formatted(name));
          continue; // with next module
        }
        for (var locator : table().lookups()) {
          var target = lib.resolve(name + ".jar");
          var source = locator.lookup(name);
          if (source == null) continue;
          download(target, URI.create(source));
          say("Restored %s from %s".formatted(name, source));
          continue with_next_module;
        }
        throw new IllegalStateException("Module not locatable: " + name);
      }
    } catch (Exception exception) {
      throw exception instanceof RuntimeException re ? re : new RuntimeException(exception);
    }
  }

  default void restoreMissingModules() {
    var lib = restorerUsesLibraryDirectory();
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var finders = List.of(ModuleFinder.of(lib)); // recreate in every loop
      var missing = ModulesSupport.listMissingNames(finders, Set.of());
      if (missing.isEmpty()) break;
      var size = missing.size();
      log("Load %d missing module%s".formatted(size, size == 1 ? "" : "s"));
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      restoreModules(missing);
      loaded.addAll(missing);
      missing.forEach(this::log);
    }
    log("Loaded %d module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
  }

  default Path restorerUsesLibraryDirectory() {
    return workflow().folders().root("lib");
  }

  default Set<String> restorerUsesModuleNames() {
    return restorerFindMissingModuleNames();
  }

  default Set<String> restorerFindMissingModuleNames() {
    var spaces = workflow().structure().spaces();
    var finders =
        spaces.list().stream().map(Space::modules).map(DeclaredModules::toModuleFinder).toList();
    var missing = ModulesSupport.listMissingNames(finders, Set.of());
    if (missing.isEmpty()) return Set.of();
    return Set.copyOf(missing);
  }

  private void download(Path target, URI source) throws IOException {
    if (!Files.exists(target)) {
      try (var stream =
          source.getScheme().startsWith("http")
              ? source.toURL().openStream()
              : Files.newInputStream(Path.of(source))) {
        var parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    // TODO Verify target bits.
  }
}
