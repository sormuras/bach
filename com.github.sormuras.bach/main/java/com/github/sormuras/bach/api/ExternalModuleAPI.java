package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.lookup.LookupException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Methods related to manage external modules. */
public interface ExternalModuleAPI {

  Bach bach();

  default String computeExternalModuleUri(String module) {
    var bach = bach();
    var libraries = bach.project().libraries();
    var found = libraries.find(module).orElseThrow(() -> new LookupException(module));
    bach.debug("%s <- %s", module, found);
    return found.uri();
  }

  default Path computeExternalModuleFile(String module) {
    return bach().folders().externalModules(module + ".jar");
  }

  default Set<String> computeMissingExternalModules() {
    var folders = bach().folders();
    var finder = ModuleFinder.of(folders.externalModules());
    var missing = required(finder);
    missing.addAll(bach().project().libraries().requires());
    if (missing.isEmpty()) return Set.of();
    missing.removeAll(declared(finder));
    missing.removeAll(declared(ModuleFinder.of(folders.bin())));
    missing.removeAll(declared(ModuleFinder.ofSystem()));
    if (missing.isEmpty()) return Set.of();
    return missing;
  }

  default void loadExternalModules(String... modules) {
    var bach = bach();
    bach.debug("Load %d external module%s", modules.length, modules.length == 1 ? "" : "s");
    if (modules.length == 0) return;
    var browser = bach.browser();
    UnaryOperator<String> uri = this::computeExternalModuleUri;
    Function<String, Path> jar = this::computeExternalModuleFile;
    if (modules.length == 1) browser.load(uri.apply(modules[0]), jar.apply(modules[0]));
    else browser.load(Stream.of(modules).collect(Collectors.toMap(uri, jar)));
  }

  default void loadMissingExternalModules() {
    var bach = bach();
    bach.debug("Load missing external modules");
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var missing = bach.computeMissingExternalModules();
      if (missing.isEmpty()) break;
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      bach.loadExternalModules(missing.toArray(String[]::new));
      loaded.addAll(missing);
    }
    bach.debug("Loaded %d module%s", loaded.size(), loaded.size() == 1 ? "" : "s");
  }

  default void verifyExternalModules() throws Exception {
    var directory = bach().folders().externalModules();
    if (Files.notExists(directory)) return;
    bach().debug("Verify external modules located in %s", directory.toUri());
    try (var jars = Files.newDirectoryStream(directory, "*.jar")) {
      for (var jar : jars) {
        var module = ModuleFinder.of(jar).findAll().iterator().next().descriptor();
        bach().debug("Verify module %s (%s)", module.toNameAndVersion(), jar.getFileName());
        verifyExternalModule(module, jar);
      }
    }
  }

  default void verifyExternalModule(ModuleDescriptor module, Path jar) throws Exception {
    var expectedJarFileName = module.name() + ".jar";
    var actualJarFileName = jar.getFileName().toString();
    if (!actualJarFileName.equals(expectedJarFileName))
      throw new Exception("JAR file name verification failed: " + actualJarFileName);

    if (bach().is(Options.Flag.STRICT))
      throw new Exception(
          "Override method verifyExternalModule(ModuleDescriptor, Path) in order to actually"
              + " verify external modules and also to prevent this exception to be thrown.");
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
