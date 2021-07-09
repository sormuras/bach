package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;

import java.lang.System.Logger.Level;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Resolver(Bach bach) {

  public void resolveExternalModule(String... modules) {
    if (modules.length == 0) return;
    var s = modules.length == 1 ? "" : "s";
    bach.log(Level.INFO, "Resolve %d external module%s".formatted(modules.length, s));
    for (var module : modules) bach.log(Level.INFO, "  " + module);

    UnaryOperator<String> uri = this::computeExternalModuleUri;
    Function<String, Path> jar = this::computeExternalModuleFile;
    if (modules.length == 1) {
      bach.browser().load(uri.apply(modules[0]), jar.apply(modules[0]));
    } else {
      bach.browser().load(Stream.of(modules).collect(Collectors.toMap(uri, jar)));
    }
    bach.log("%s module%s loaded".formatted(modules.length, s));
  }

  public void resolveMissingExternalModules() {
    bach.log("Resolve missing external modules");
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var missing = computeMissingExternalModules();
      if (missing.isEmpty()) break;
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      resolveExternalModule(missing.toArray(String[]::new));
      loaded.addAll(missing);
    }
    var size = loaded.size();
    var level = size == 0 ? Level.DEBUG : Level.INFO;
    bach.log(level, "Loaded %d missing module%s".formatted(size, size == 1 ? "" : "s"));
  }

  String computeExternalModuleUri(String module) {
    var externals = bach.project().externals();
    for (var locator : externals.locators()) {
      var location = locator.locate(bach, module);
      if (location.isEmpty()) continue;
      var uri = location.get().uri();
      bach.log("%s <- %s (by %s)".formatted(module, uri, locator.title()));
      return uri;
    }
    throw new FindException(module);
  }

  Path computeExternalModuleFile(String module) {
    return bach.folders().externalModules(module + ".jar");
  }

  Set<String> computeMissingExternalModules() {
    return computeMissingExternalModules(true);
  }

  Set<String> computeMissingExternalModules(boolean includeRequiresOfExternalModules) {
    // Populate a set with all module names being in a "requires MODULE;" directive
    var project = bach.project();
    var mains = DeclaredModuleFinder.of(project.spaces().main().modules());
    var tests = DeclaredModuleFinder.of(project.spaces().test().modules());
    var requires = new TreeSet<>(project.externals().requires()); // project-requires
    requires.addAll(required(mains)); // main module-info
    requires.addAll(required(tests)); // test module-info
    var externalModulesFinder = ModuleFinder.of(bach.folders().externalModules());
    if (includeRequiresOfExternalModules) requires.addAll(required(externalModulesFinder));

    bach.log("Computed %d required modules: %s".formatted(requires.size(), requires));
    // Remove names of locatable modules from various module spaces
    requires.removeAll(declared(ModuleFinder.ofSystem()));
    requires.removeAll(declared(mains));
    requires.removeAll(declared(tests));
    requires.removeAll(declared(externalModulesFinder));
    // Still here? Return names of missing modules
    bach.log("Computed %d missing external modules: %s".formatted(requires.size(), requires));
    return Set.copyOf(requires);
  }

  static TreeSet<String> declared(ModuleFinder finder) {
    return declared(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  static TreeSet<String> declared(Stream<ModuleDescriptor> descriptors) {
    return descriptors.map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
  }

  static TreeSet<String> required(ModuleFinder finder) {
    return required(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  static TreeSet<String> required(Stream<ModuleDescriptor> descriptors) {
    return descriptors
        .map(ModuleDescriptor::requires)
        .flatMap(Set::stream)
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.STATIC))
        .filter(requires -> !requires.modifiers().contains(Requires.Modifier.SYNTHETIC))
        .map(Requires::name)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
