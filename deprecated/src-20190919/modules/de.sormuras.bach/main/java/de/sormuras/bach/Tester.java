package de.sormuras.bach;

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/*BODY*/
/** Test all modules of the project. */
/*STATIC*/ class Tester {

  private final Bach bach;
  private final TestRealm test;

  Tester(Bach bach, TestRealm test) {
    this.bach = bach;
    this.test = test;
  }

  @SuppressWarnings("unused")
  private void log(System.Logger.Level level, String format, Object... args) {
    if (bach.configuration.debug() || level.getSeverity() > DEBUG.getSeverity()) {
      var writer = level.getSeverity() > INFO.getSeverity() ? bach.err : bach.out;
      writer.println(String.format(format, args));
    }
  }

  void test(Collection<String> modules) {
    var console =
        ModuleFinder.of(bach.configuration.getLibraryPaths().toArray(Path[]::new))
            .find("org.junit.platform.console");
    var errors = new StringBuilder();
    for (var module : modules) {
      var info = test.getDeclaredModuleInfo(module);
      if (info == null) {
        log(WARNING, "No test module available for: %s", module);
        continue;
      }
      log(INFO, "%n%n%n%s%n%n%n", module);
      // errors.append(testModuleMainClass(module));
      if (console.isPresent()) {
        // errors.append(testClassPathDirect(module));
        // errors.append(testClassPathForked(module));
        errors.append(testModulePathDirect(info));
        errors.append(testModulePathForked(info));
      } else {
        log(WARNING, "Module 'org.junit.platform.console' not present");
      }
    }
    if (errors.toString().replace('0', ' ').isBlank()) {
      return;
    }
    throw new AssertionError("Test run with errors: " + errors);
  }

  //    int testModuleMainClass(String module) {
  //      var mainClass =
  //          ModuleFinder.of(project.test.modularJar(module))
  //              .find(module)
  //              .orElseThrow()
  //              .descriptor()
  //              .mainClass();
  //      if (mainClass.isEmpty()) { // No main class present...
  //        return 0;
  //      }
  //      var needsPatch = project.main.declaredModules.containsKey(module);
  //      var java =
  //          new Command("java")
  //              .add("--module-path", project.test.modulePathRuntime(needsPatch))
  //              .add("--module", module);
  //      return runner.run(java);
  //    }

  //    int testClassPathDirect(String module) {
  //      var classPath = project.test.classPathRuntime(module);
  //      var urls = classPath.stream().map(Util::url).toArray(URL[]::new);
  //      var parentLoader = ClassLoader.getPlatformClassLoader();
  //      var junitLoader = new URLClassLoader("junit", urls, parentLoader);
  //      var junit = new Command("junit").addEach(configuration.lines(Property.OPTIONS_JUNIT));
  //      project.test.packages(module).forEach(path -> junit.add("--select-package", path));
  //      return launchJUnitPlatformConsole(junitLoader, junit);
  //    }

  //    int testClassPathForked(String module) {
  //      var java =
  //          new Command("java")
  //              .add("-ea")
  //              .add("--class-path", project.test.classPathRuntime(module))
  //              .add("org.junit.platform.console.ConsoleLauncher")
  //              .addEach(configuration.lines(Property.OPTIONS_JUNIT));
  //      project.test.packages(module).forEach(path -> java.add("--select-package", path));
  //      return runner.run(java);
  //    }

  private int testModulePathDirect(Realm.Info info) {
    var junit =
        new Command("junit")
            .addEach(bach.configuration.lines(Property.TOOL_JUNIT_OPTIONS))
            .add("--select-module", info.module);
    try {
      return testModulePathDirect(info, junit);
    } finally {
      var windows = System.getProperty("os.name", "?").toLowerCase().contains("win");
      if (windows) {
        System.gc();
        Util.sleep(1234);
      }
    }
  }

  private int testModulePathForked(Realm.Info info) {
    var module = info.module;
    var java =
        new Command("java")
            .add("-ea")
            .add("--module-path", test.getRuntimeModulePaths(info.getModularJar()))
            .add("--add-modules", module)
            .add("--module")
            .add("org.junit.platform.console")
            .addEach(bach.configuration.lines(Property.TOOL_JUNIT_OPTIONS))
            .add("--select-module", module);
    bach.out.println(java.toCommandLine());
    return 0;
  }

  /** Launch JUnit Platform Console for the given "module under test". */
  private int testModulePathDirect(Realm.Info info, Command junit) {
    var modulePath = test.getRuntimeModulePaths(info.getModularJar());
    log(DEBUG, "Module path:");
    for (var element : modulePath) {
      log(DEBUG, "  -> %s", element);
    }
    var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
    log(DEBUG, "Finder finds module(s):");
    for (var reference : finder.findAll()) {
      log(DEBUG, "  -> %s", reference);
    }
    var roots = List.of(info.module, "org.junit.platform.console");
    log(DEBUG, "Root module(s):");
    for (var root : roots) {
      log(DEBUG, "  -> %s", root);
    }
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
    var parentLoader = ClassLoader.getSystemClassLoader();
    var controller = defineModulesWithOneLoader(configuration, List.of(boot), parentLoader);
    var junitConsoleLayer = controller.layer();
    controller.addExports(
        junitConsoleLayer.findModule("org.junit.platform.console").orElseThrow(),
        "org.junit.platform.console",
        Bach.class.getModule());
    var junitConsoleLoader = junitConsoleLayer.findLoader("org.junit.platform.console");
    var junitLoader = new URLClassLoader("junit", new URL[0], junitConsoleLoader);
    return launchJUnitPlatformConsole(junitLoader, junit);
  }

  /** Launch JUnit Platform Console passing all arguments of the given command. */
  private int launchJUnitPlatformConsole(ClassLoader loader, Command junit) {
    log(DEBUG, "Launching JUnit Platform Console: %s", junit.getList());
    log(DEBUG, "Using class loader: %s - %s", loader.getName(), loader);
    var currentThread = Thread.currentThread();
    var currentContextLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(loader);
    var parent = loader;
    while (parent != null) {
      parent.setDefaultAssertionStatus(true);
      parent = parent.getParent();
    }
    try {
      var launcher = loader.loadClass("org.junit.platform.console.ConsoleLauncher");
      var params = new Class<?>[] {PrintStream.class, PrintStream.class, String[].class};
      var execute = launcher.getMethod("execute", params);
      var out = new ByteArrayOutputStream();
      var err = new ByteArrayOutputStream();
      var args = junit.toStringArray();
      var result = execute.invoke(null, new PrintStream(out), new PrintStream(err), args);
      bach.out.write(out.toString());
      bach.out.flush();
      bach.err.write(err.toString());
      bach.err.flush();
      return (int) result.getClass().getMethod("getExitCode").invoke(result);
    } catch (Exception e) {
      throw new Error("ConsoleLauncher.execute(...) failed: " + e, e);
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }
}
