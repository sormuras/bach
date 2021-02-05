package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.ToolProviders;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
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

  public static Bach bach() {
    return bach;
  }

  public static void beep() {
    System.out.print("\007");
    System.out.flush();
  }

  /**
   * Prints a listing of all files matching the given glob pattern.
   *
   * @param glob the glob pattern
   */
  public static void find(String glob) {
    var start = Path.of("");
    var pattern = glob;
    while (pattern.startsWith(".") || pattern.startsWith("/")) pattern = pattern.substring(1);
    var matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    try (var paths = Files.find(start, 99, (path, __) -> matcher.matches(start.relativize(path)))) {
      paths.filter(Shell::isVisible).map(Path::normalize).map(Shell::slashed).forEach(out);
    } catch (Exception exception) {
      throw new RuntimeException("find failed: " + start + " -> " + glob, exception);
    }
  }

  private static boolean isVisible(Path path) {
    try {
      for (int endIndex = 1; endIndex <= path.getNameCount(); endIndex++) {
        var subpath = path.subpath(0, endIndex);
        // work around https://bugs.openjdk.java.net/browse/JDK-8255576
        var probe = subpath.toString().isEmpty() ? path.toAbsolutePath() : subpath;
        if (!Files.isReadable(probe)) return false;
        if (Files.isHidden(probe)) return false;
      }
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String slashed(Path path) {
    return path.toString().replace('\\', '/');
  }

  public static void refresh() {
   refresh("configuration");
  }

  public static void refresh(String module) {
    try {
      bach = Bach.of(module);
    } catch (Exception exception) {
      err.accept(
          """

          Refresh failed: %s

            Falling back to default Bach instance.
          """
              .formatted(exception.getMessage()));
      bach = new Bach();
    }
    hash = computeHash();
  }

  private static void refreshOnChanges() {
    if (Arrays.equals(hash, computeHash())) return;
    refresh();
    beep();
  }

  private static byte[] computeHash() {
    return computeHash(bach.base().directory(".bach", "configuration"));
  }

  private static byte[] computeHash(Path path) {
    if (Files.notExists(path)) return new byte[0];
    try {
      var md = MessageDigest.getInstance("MD5");
      if (Files.isRegularFile(path)) return md.digest(Files.readAllBytes(path));
      try (var walk = Files.walk(path)) {
        var paths = walk.filter(Files::isRegularFile).sorted().toArray(Path[]::new);
        for (var file : paths) md.update(Files.readAllBytes(file));
      }
      return md.digest();
    } catch (Exception exception) {
      bach.debug("Compute digest failed for: %s", path);
      bach.debug("%s", exception);
      return new byte[0];
    }
  }

  public static void loadExternalModule(String module) {
    bach.loadExternalModules(module);
    var missing = bach.computeMissingExternalModules();
    if (missing.isEmpty()) return;
    listMissingExternalModules(missing);
  }

  public static void loadExternalModuleComGithubSormurasModules() {
    loadExternalModuleComGithubSormurasModules("0-ea");
  }

  public static void loadExternalModuleComGithubSormurasModules(String version) {
    var module = "com.github.sormuras.modules";
    var jar = module + "@" + version + ".jar";
    var uri = "https://github.com/sormuras/modules/releases/download/" + version + "/" + jar;
    bach.browser().load(uri, bach.computeExternalModuleFile(module));
  }

  public static void loadMissingExternalModules() {
    bach.loadMissingExternalModules();
  }

  public static void deleteExternalModule(String module) throws Exception {
    Files.deleteIfExists(bach.computeExternalModuleFile(module));
  }

  public static void deleteExternalModules() throws Exception {
    var externals = bach.base().externals();
    if (!Files.isDirectory(externals)) return;
    try (var jars = Files.newDirectoryStream(externals, "*.jar")) {
      for (var jar : jars) try {
        bach.debug("Delete %s", jar);
        Files.deleteIfExists(jar);
      } catch (Exception exception) {
        bach.print("Delete failed: %s", jar);
      }
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
    out.accept(String.format("%n-> %d module%s", all.size(), all.size() == 1 ? "" : "s"));
  }

  /** Prints a list of all external modules. */
  public static void listExternalModules() {
    printModules(ModuleFinder.of(bach.base().externals()));
  }

  public static void listMissingExternalModules() {
    listMissingExternalModules(bach.computeMissingExternalModules());
  }

  private static void listMissingExternalModules(Set<String> modules) {
    var size = modules.size();
    modules.stream().sorted().forEach(out);
    out.accept(String.format("%n-> %d module%s missing", size, size == 1 ? " is" : "s are"));
  }

  public static void listModulesWithRequiresDirectivesMatching(String regex) {
    var finder = ModuleFinder.of(bach.base().externals());
    var descriptors =
        finder.findAll().stream()
            .map(ModuleReference::descriptor)
            .sorted(Comparator.comparing(ModuleDescriptor::name))
            .toList();
    for (var descriptor : descriptors) {
      var matched =
          descriptor.requires().stream()
              .filter(requires -> requires.name().matches(regex))
              .toList();
      if (matched.isEmpty()) continue;
      out.accept(descriptor.toNameAndVersion());
      matched.forEach(requires -> out.accept("  " + requires));
    }
  }

  /** Prints a list of all system modules. */
  public static void listSystemModules() {
    printModules(ModuleFinder.ofSystem());
  }

  public static void listPublicStaticShellMethods() {
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

  public static void listRecordings() {
    var recordings = bach.recordings();
    var size = recordings.size();
    recordings.forEach(Shell::printRecording);
    out.accept(String.format("%n-> %d recording%s", size, size == 1 ? "" : "s"));
  }

  public static void listToolProviders() {
    var providers = bach.computeToolProviders().toList();
    var size = providers.size();
    providers.stream()
        .map(ToolProviders::describe)
        .sorted()
        .map(description -> "\n" + description)
        .forEach(out);
    out.accept(String.format("%n-> %d provider%s", size, size == 1 ? "" : "s"));
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
