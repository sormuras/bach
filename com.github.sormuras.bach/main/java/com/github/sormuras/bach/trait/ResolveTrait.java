package com.github.sormuras.bach.trait;

import com.github.sormuras.bach.Trait;
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

public /*sealed*/ interface ResolveTrait extends Trait {

  private String computeExternalModuleUri(String module) {
    var externals = bach().project().externals();
    for (var locator : externals.locators()) {
      var location = locator.locate(module);
      if (location.isEmpty()) continue;
      var uri = location.get().uri();
      bach().log("%s <- %s (by %s)".formatted(module, uri, locator.title()));
      return uri;
    }
    throw new FindException(module);
  }

  private Path computeExternalModuleFile(String module) {
    return bach().project().folders().externalModules(module + ".jar");
  }

  private Set<String> computeMissingExternalModules() {
    return computeMissingExternalModules(true);
  }

  private Set<String> computeMissingExternalModules(boolean includeRequiresOfExternalModules) {
    // Populate a set with all module names being in a "requires MODULE;" directive
    var project = bach().project();
    var requires = new TreeSet<>(project.externals().requires()); // project-requires
    requires.addAll(required(project.spaces().main().modules())); // main module-info
    requires.addAll(required(project.spaces().test().modules())); // test module-info
    var externalModulesFinder = ModuleFinder.of(project.folders().externalModules());
    if (includeRequiresOfExternalModules) requires.addAll(required(externalModulesFinder));

    bach().log("Computed %d required modules: %s".formatted(requires.size(), requires));
    // Remove names of locatable modules from various module realms
    requires.removeAll(declared(ModuleFinder.ofSystem()));
    requires.removeAll(declared(project.spaces().main().modules()));
    requires.removeAll(declared(project.spaces().test().modules()));
    requires.removeAll(declared(externalModulesFinder));
    // Still here? Return names of missing modules
    bach().log("Computed %d missing external modules: %s".formatted(requires.size(), requires));
    return Set.copyOf(requires);
  }

  default void loadExternalModules(String... modules) {
    if (modules.length == 0) return;
    var s = modules.length == 1 ? "" : "s";
    bach().say("Load %d external module%s".formatted(modules.length, s));
    for (var module : modules) bach().say("  " + module);

    UnaryOperator<String> uri = this::computeExternalModuleUri;
    Function<String, Path> jar = this::computeExternalModuleFile;
    if (modules.length == 1) {
      bach().core().httpLoad(uri.apply(modules[0]), jar.apply(modules[0]));
    } else {
      bach().core().httpLoad(Stream.of(modules).collect(Collectors.toMap(uri, jar)));
    }
    bach().say("%s module%s loaded".formatted(modules.length, s));
  }

  default void loadMissingExternalModules() {
    var bach = bach();
    bach.log("Load missing external modules");
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var missing = computeMissingExternalModules();
      if (missing.isEmpty()) break;
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      bach.loadExternalModules(missing.toArray(String[]::new));
      loaded.addAll(missing);
    }
    bach.log("Loaded %d missing module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
  }

  private static TreeSet<String> declared(ModuleFinder finder) {
    return declared(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  private static TreeSet<String> declared(Stream<ModuleDescriptor> descriptors) {
    return descriptors.map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
  }

  private static TreeSet<String> required(ModuleFinder finder) {
    return required(finder.findAll().stream().map(ModuleReference::descriptor));
  }

  private static TreeSet<String> required(Stream<ModuleDescriptor> descriptors) {
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
