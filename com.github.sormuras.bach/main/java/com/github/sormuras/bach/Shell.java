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
   refresh("build");
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
    return computeHash(bach.base().directory(".bach", "build"));
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

  public static void init() throws Exception {
    initBachLaunchScripts();
    initBuildModule("build");
  }

  public static void initBachLaunchScripts() throws Exception {
    var bash = bach.base().directory("bach");
    if (Files.exists(bash) && !Boolean.getBoolean("force-init")) {
      out.accept("Launch script already exists: " + bash);
    } else {
      out.accept("Generate launch script: " + bash);
      Files.writeString(
              bash,
              """
          #!/usr/bin/env bash

          if [[ $1 != 'init' ]]; then
            java --module-path .bach/cache --module com.github.sormuras.bach "$@"
          else
            rm -f .bach/cache/com.github.sormuras.bach@*.jar
            jshell -R-Dreboot -R-Dversion="${2:-17-ea}" https://bit.ly/bach-main-init
          fi
          """)
          .toFile()
          .setExecutable(true);
    }
    var bat = bach.base().directory("bach.bat");
    if (Files.exists(bat) && !Boolean.getBoolean("force-init")) {
      out.accept("Launch script already exists: " + bat);
    } else {
      out.accept("Generate launch script: " + bat);
      Files.writeString(
          bat,
          """
          @ECHO OFF

          IF [%1]==[init] GOTO INIT

          java --module-path .bach\\cache --module com.github.sormuras.bach %*

          GOTO END

          :INIT
          del .bach\\cache\\com.github.sormuras.bach@*.jar >nul 2>&1
          SETLOCAL
          IF [%2]==[] ( SET tag=17-ea ) ELSE ( SET tag=%2 )
          jshell -R-Dreboot -R-Dversion=%tag% https://bit.ly/bach-main-init
          ENDLOCAL

          :END
          """);
    }
  }

  public static void initBuildModule(String name) throws Exception {
    var info = bach.base().directory(".bach", name, "module-info.java");
    if (Files.exists(info) && !Boolean.getBoolean("force-init")) {
      out.accept("Build module already exists: " + info);
    } else {
      out.accept("Generate build module declaration: " + info);
      Files.createDirectories(info.getParent());
      Files.writeString(
              info,
              """
              // @ProjectInfo()
              module %1$s {
                requires com.github.sormuras.bach;
                // provides com.github.sormuras.bach.Bach with %1$s.CustomBach;
              }
              """.formatted(name))
          .toFile()
          .setExecutable(true);
    }
  }

  public static void loadExternalModules(String... modules) {
    bach.loadExternalModules(modules);
  }

  public static void loadMissingExternalModules() {
    bach.loadMissingExternalModules();
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
    var modules = bach.computeMissingExternalModules();
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
