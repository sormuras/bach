package de.sormuras.bach;

import static java.lang.ModuleLayer.defineModulesWithOneLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/*BODY*/
/** Test all modules of the project. */
/*STATIC*/ class Tester {

  final Bach bach;
  final TestRealm test;

  Tester(Bach bach, TestRealm test) {
    this.bach = bach;
    this.test = test;
  }

  void test(Collection<String> modules) {
    StringBuilder errors = new StringBuilder();
    for (var module : modules) {
      //        if (Files.notExists(project.test.modularJar(module))) {
      //          // log(DEBUG, "No test module available for: %s", module);
      //          continue;
      //        }
      bach.out.printf("%n%n%n%s%n%n%n", module);
      // errors.append(testModuleMainClass(module));
      if (ModuleFinder.of(test.getModulePaths().toArray(Path[]::new))
          .find("org.junit.platform.console")
          .isPresent()) {
        // errors.append(testClassPathDirect(module));
        // errors.append(testClassPathForked(module));
        errors.append(testModulePathDirect(module));
        // errors.append(testModulePathForked(module));
      } else {
        bach.out.println("Module 'org.junit.platform.console' not present");
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

  private int testModulePathDirect(String module) {
    var junit =
        new Command("junit")
            .addEach(bach.configuration.lines(Property.TOOL_JUNIT_OPTIONS))
            .add("--select-module", module);
    try {
      return testModulePathDirect(module, junit);
    } finally {
      var windows = System.getProperty("os.name", "?").toLowerCase().contains("win");
      if (windows) {
        System.gc();
        Util.sleep(1234);
      }
    }
  }

  //    int testModulePathForked(String module) {
  //      var needsPatch = project.main.declaredModules.containsKey(module);
  //      var java =
  //          new Command("java")
  //              .add("-ea")
  //              .add("--module-path", project.test.modulePathRuntime(needsPatch))
  //              .add("--add-modules", module);
  //      if (needsPatch) {
  //        java.add("--patch-module", module + "=" + project.main.modularJar(module));
  //      }
  //      java.add("--module")
  //          .add("org.junit.platform.console")
  //          .addEach(configuration.lines(Property.OPTIONS_JUNIT))
  //          .add("--select-module", module);
  //      return runner.run(java);
  //    }

  /** Launch JUnit Platform Console for the given module. */
  private int testModulePathDirect(String module, Command junit) {
    // var needsPatch = test.main.declaredModules.containsKey(module);
    var modulePath = test.getModulePaths();
    //      log(DEBUG, "Module path:");
    //      for (var element : modulePath) {
    //        log(DEBUG, "  -> %s", element);
    //      }
    var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
    //      log(DEBUG, "Finder finds module(s):");
    //      for (var reference : finder.findAll()) {
    //        log(DEBUG, "  -> %s", reference);
    //      }
    var roots = List.of(module, "org.junit.platform.console");
    //      log(DEBUG, "Root module(s):");
    //      for (var root : roots) {
    //        log(DEBUG, "  -> %s", root);
    //      }
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
    var parentLoader = ClassLoader.getPlatformClassLoader();
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
    // log(DEBUG, "Launching JUnit Platform Console: %s", junit.list);
    // log(DEBUG, "Using class loader: %s - %s", loader.getName(), loader);
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
      bach.err.write(err.toString());
      return (int) result.getClass().getMethod("getExitCode").invoke(result);
    } catch (Exception e) {
      throw new Error("ConsoleLauncher.execute(...) failed: " + e, e);
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }
}
