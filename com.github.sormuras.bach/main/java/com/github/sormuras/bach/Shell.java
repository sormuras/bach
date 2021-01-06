package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.ToolProviders;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** An environment used by Bach's Boot Script. */
@SuppressWarnings("unused")
public class Shell {

  private static final Consumer<Object> out = System.out::println;
  private static final Consumer<Object> err = System.err::println;

  private static Bach bach;
  private static byte[] hash = {};

  static {
    refresh();
    Executors.newSingleThreadScheduledExecutor()
        .scheduleWithFixedDelay(Shell::refreshOnChanges, 3, 2, TimeUnit.SECONDS);
  }

  public static void bach(String... actions) {
    new Main(bach).performActions(actions);
  }

  public static void refresh() {
    bach = Bach.of("build");
    hash = computeHash();
  }

  private static void refreshOnChanges() {
    if (Arrays.equals(hash, computeHash())) return;
    refresh();
    bach.printInfo();
  }

  private static byte[] computeHash() {
    return computeDigest(bach.base().directory(".bach", "build"));
  }

  private static byte[] computeDigest(Path path) {
    if (Files.notExists(path)) return new byte[0];
    try {
      var md = MessageDigest.getInstance("MD5");
      if (Files.isRegularFile(path)) return md.digest(Files.readAllBytes(path));
      try (var walk = Files.walk(path)) {
        var paths = walk.filter(Files::isRegularFile).sorted().toArray(Path[]::new);
        for (var it : paths) md.update(Files.readAllBytes(it));
      }
      return md.digest();
    } catch (Exception exception) {
      bach.debug("Compute digest failed for: %s", path);
      bach.debug("%s", exception);
      return new byte[0];
    }
  }

  /**
   * Prints a module description of the given module.
   *
   * @param module the name of the module to describe
   */
  public static void printModuleDescription(String module) {
    ModuleFinder.compose(
            ModuleFinder.of(bach.base().workspace("modules")),
            ModuleFinder.of(bach.base().externals()),
            ModuleFinder.ofSystem())
        .find(module)
        .ifPresentOrElse(
            reference -> out.accept(Modules.describe(reference)),
            () -> err.accept("No such module found: " + module));
  }

  /**
   * Print a sorted list of all modules locatable by the given module finder.
   *
   * @param finder the module finder to query for modules
   */
  private static void printModules(ModuleFinder finder) {
    var all = finder.findAll();
    all.stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::toNameAndVersion)
        .sorted()
        .forEach(out);
    out.accept(String.format("-> %d module%s", all.size(), all.size() == 1 ? "" : "s"));
  }

  /** Prints a list of all external modules. */
  public static void printExternalModules() {
    printModules(ModuleFinder.of(bach.base().externals()));
  }

  /** Prints a list of all system modules. */
  public static void printSystemModules() {
    printModules(ModuleFinder.ofSystem());
  }

  public static void printPublicStaticShellMethods() {
    Stream.of(Shell.class.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .filter(method -> Modifier.isStatic(method.getModifiers()))
        .filter(method -> !method.getName().equals("printAPI"))
        .sorted(Comparator.comparing(Method::getName))
        .map(Method::toGenericString)
        .map(line -> line.substring(line.indexOf("Shell") + 6))
        .map(line -> line.replace("java.lang.", ""))
        .map(line -> line.replace("com.github.sormuras.bach.", ""))
        .forEach(out);
  }

  public static void printRecording(Recording recording) {
    out.accept(recording);
  }

  public static void printRecordings() {
    bach.recordings().forEach(Shell::printRecording);
  }

  public static void printToolProviders() {
    bach.computeToolProviders()
        .map(ToolProviders::describe)
        .sorted()
        .map(description -> "\n" + description)
        .forEach(out);
  }

  public static void run(String tool, Object... args) {
    var command = Command.of(tool);
    var recording = bach.run(args.length == 0 ? command : command.add("", args));
    if (!recording.errors().isEmpty()) out.accept(recording.errors());
    if (!recording.output().isEmpty()) out.accept(recording.output());
    if (recording.isError()) out.accept("Tool " + tool + " returned exit code " + recording.code());
  }

  private Shell() {}
}
