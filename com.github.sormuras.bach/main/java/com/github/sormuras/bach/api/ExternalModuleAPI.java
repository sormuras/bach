package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.lookup.LookupException;
import com.github.sormuras.bach.project.Flag;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Methods related to manage external modules. */
public interface ExternalModuleAPI extends API {

  default String computeExternalModuleUri(String module) {
    var bach = bach();
    var libraries = bach.project().libraries();
    var found = libraries.find(module).orElseThrow(() -> new LookupException(module));
    bach.log("%s <- %s", module, found);
    return found.uri();
  }

  default Path computeExternalModuleFile(String module) {
    return bach().folders().externalModules(module + ".jar");
  }

  default Set<String> computeMissingExternalModules() {
    return computeMissingExternalModules(!bach().is(Flag.STRICT));
  }

  default Set<String> computeMissingExternalModules(boolean includeRequiresOfExternalModules) {
    // Populate a set with all module names being in a "requires MODULE;" directive
    var requires = new TreeSet<>(bach().project().libraries().requires()); // project-info
    requires.addAll(required(bach().project().spaces().main().declarations())); // main module-info
    requires.addAll(required(bach().project().spaces().test().declarations())); // test module-info
    if (includeRequiresOfExternalModules) {
      var externalFinder = ModuleFinder.of(bach().folders().externalModules());
      requires.addAll(required(externalFinder)); // external-modules
    }
    log("Computed %d required modules: %s", requires.size(), requires);
    // Remove names of locatable modules from various locations
    if (requires.isEmpty()) return Set.of();
    requires.removeAll(declared(ModuleFinder.ofSystem()));
    if (requires.isEmpty()) return Set.of();
    requires.removeAll(declared(bach().project().spaces().main().declarations()));
    requires.removeAll(declared(bach().project().spaces().test().declarations()));
    if (requires.isEmpty()) return Set.of();
    requires.removeAll(declared(ModuleFinder.of(bach().folders().externalModules())));
    if (requires.isEmpty()) return Set.of();
    requires.removeAll(declared(ModuleFinder.of(Bach.bin())));
    if (requires.isEmpty()) return Set.of();
    // Still here? Return names of missing modules
    log("Computed %d missing external modules: %s", requires.size(), requires);
    return Set.copyOf(requires);
  }

  default void loadExternalModules(String... modules) {
    var bach = bach();
    if (modules.length == 0) return;
    bach.say("Load %d external module%s", modules.length, modules.length == 1 ? "" : "s");
    for (var module : modules) say("  %s", module);
    var browser = bach.browser();
    UnaryOperator<String> uri = this::computeExternalModuleUri;
    Function<String, Path> jar = this::computeExternalModuleFile;
    if (modules.length == 1) browser.load(uri.apply(modules[0]), jar.apply(modules[0]));
    else browser.load(Stream.of(modules).collect(Collectors.toMap(uri, jar)));
  }

  default void loadMissingExternalModules() {
    var bach = bach();
    bach.log("Load missing external modules");
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
    bach.log("Loaded %d module%s", loaded.size(), loaded.size() == 1 ? "" : "s");
  }

  default void verifyExternalModules() throws Exception {
    var directory = bach().folders().externalModules();
    if (Files.notExists(directory)) return;
    say("Verify external modules located in %s", directory.toUri());
    var verified = new ArrayList<String>();
    try (var jars = Files.newDirectoryStream(directory, "*.jar")) {
      for (var jar : jars) {
        var module = ModuleFinder.of(jar).findAll().iterator().next().descriptor();
        var name = module.toNameAndVersion();
        log("Verify module %s (%s)", name, jar.getFileName());
        verifyExternalModule(module, jar);
        verified.add(name);
      }
    }
    say("Verified %d external modules", verified.size());
  }

  default void verifyExternalModule(ModuleDescriptor module, Path jar) throws Exception {
    var expectedJarFileName = module.name() + ".jar";
    var actualJarFileName = jar.getFileName().toString();
    if (!actualJarFileName.equals(expectedJarFileName))
      throw new Exception("JAR file name verification failed: " + actualJarFileName);

    var name = module.name();
    var metadata = bach().project().libraries().metamap().get(name);
    if (metadata == null) {
      log("No verification metadata available for module: %s", name);
      if (bach().is(Flag.STRICT)) throw new IllegalArgumentException("No metadata for: " + name);
      return;
    }

    var expectedSize = metadata.size();
    var actualSize = Files.size(jar);
    if (expectedSize != actualSize)
      throw new Exception(
          "Size mismatch of module %s detected! Expected %d but got: %d"
              .formatted(name, expectedSize, actualSize));

    for (var checksum : metadata.checksums()) {
      var expectedHash = checksum.value();
      var actualHash = hash(checksum.algorithm(), jar);
      if (!expectedHash.equals(actualHash))
        throw new Exception(
            "Hash mismatch of module %s! Expected %s of %s but got: %s"
                .formatted(name, checksum.algorithm(), expectedHash, actualHash));
    }
  }

  static String hash(String algorithm, Path file) throws Exception {
    var md = MessageDigest.getInstance(algorithm);
    try (var in = new BufferedInputStream(new FileInputStream(file.toFile()));
        var out = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
      in.transferTo(out);
    }
    return String.format("%0" + (md.getDigestLength() * 2) + "x", new BigInteger(1, md.digest()));
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
