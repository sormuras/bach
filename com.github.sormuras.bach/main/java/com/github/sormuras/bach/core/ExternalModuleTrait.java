package com.github.sormuras.bach.core;

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

public /*sealed*/ interface ExternalModuleTrait extends BachTrait {

  private String computeExternalModuleUri(String module) {
    var bach = bach();
    var externals = bach.project().externals();
    var found = externals.findExternal(module).orElseThrow(() -> new FindException(module));
    bach().log("%s <- %s (by %s)".formatted(module, found.location().uri(), found.by().title()));
    return found.location().uri();
  }

  private Path computeExternalModuleFile(String module) {
    return bach().project().folders().externals(module + ".jar");
  }

  private Set<String> computeMissingExternalModules() {
    return computeMissingExternalModules(true);
  }

  private Set<String> computeMissingExternalModules(boolean includeRequiresOfExternalModules) {
    // Populate a set with all module names being in a "requires MODULE;" directive
    var project = bach().project();
    var requires = new TreeSet<>(project.externals().requires()); // project-info
    // TODO requires.addAll(required(project.spaces().main().declarations())); // main module-info
    // TODO requires.addAll(required(project.spaces().test().declarations())); // test module-info
    var externalModulesFinder = ModuleFinder.of(project.folders().externals());
    if (includeRequiresOfExternalModules) requires.addAll(required(externalModulesFinder));

    bach().log("Computed %d required modules: %s".formatted(requires.size(), requires));
    // Remove names of locatable modules from various module realms
    requires.removeAll(declared(ModuleFinder.ofSystem()));
    // TODO requires.removeAll(declared(project.spaces().main().declarations()));
    // TODO requires.removeAll(declared(project.spaces().test().declarations()));
    requires.removeAll(declared(externalModulesFinder));
    // TODO requires.removeAll(declared(ModuleFinder.of(Bach.bin())));
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
      bach().httpLoad(uri.apply(modules[0]), jar.apply(modules[0]));
    } else {
      bach().httpLoad(Stream.of(modules).collect(Collectors.toMap(uri, jar)));
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
