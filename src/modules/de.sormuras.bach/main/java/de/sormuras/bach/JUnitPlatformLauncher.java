package de.sormuras.bach;

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.System.Logger.Level.DEBUG;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/*BODY*/
/** Launch the JUnit Platform Console using compiled test modules. */
public /*STATIC*/ class JUnitPlatformLauncher implements Callable<Integer> {

  final Bach bach;
  final Run run;
  final Path bin;
  final Path lib;
  final String version;

  JUnitPlatformLauncher(Bach bach) {
    this.bach = bach;
    this.run = bach.run;
    this.version = bach.project.version;
    this.bin = bach.run.work.resolve(bach.project.path(Project.Property.PATH_BIN));
    this.lib = bach.run.home.resolve(bach.project.path(Project.Property.PATH_LIB));
  }

  @Override
  public Integer call() throws Exception {
    var modules = bach.project.modules("test");
    if (modules.isEmpty()) {
      return 0;
    }
    junit();
    bach.run.log(DEBUG, "JUnit successful.");
    return 0;
  }

  /** Launch JUnit Platform for given modular realm. */
  private void junit() throws Exception {
    var junit =
        new Command("junit")
            .add("--fail-if-no-tests")
            .add("--reports-dir", bin.resolve("test/junit-reports"))
            .add("--scan-modules");
    try {
      launchJUnitPlatformConsole(junit);
    } finally {
      var windows = System.getProperty("os.name", "?").toLowerCase(Locale.ENGLISH).contains("win");
      if (windows) {
        System.gc();
        Thread.sleep(1234);
      }
    }
  }

  /** Launch JUnit Platform for given modular realm. */
  private void launchJUnitPlatformConsole(Command junit) {
    var modulePath = bach.project.modulePath("test", "runtime", "main");
    run.log(DEBUG, "Module path:");
    for (var element : modulePath) {
      run.log(DEBUG, "  -> %s", element);
    }
    var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
    run.log(DEBUG, "Finder finds module(s):");
    for (var reference : finder.findAll()) {
      run.log(DEBUG, "  -> %s", reference);
    }
    var rootModules = new ArrayList<>(bach.project.modules("test"));
    rootModules.add("org.junit.platform.console");
    run.log(DEBUG, "Root module(s):");
    for (var module : rootModules) {
      run.log(DEBUG, "  -> %s", module);
    }
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), rootModules);
    var parentLoader = ClassLoader.getPlatformClassLoader();
    var controller = defineModulesWithOneLoader(configuration, List.of(boot), parentLoader);
    var junitConsoleLayer = controller.layer();
    controller.addExports( // "Bach.java" resides in an unnamed module...
        junitConsoleLayer.findModule("org.junit.platform.console").orElseThrow(),
        "org.junit.platform.console",
        Bach.class.getModule());
    var junitConsoleLoader = junitConsoleLayer.findLoader("org.junit.platform.console");
    var junitLoader = new URLClassLoader("junit", new URL[0], junitConsoleLoader);
    launchJUnitPlatformConsole(run, junitLoader, junit);
  }

  private void launchJUnitPlatformConsole(Run run, ClassLoader loader, Command junit) {
    run.log(DEBUG, "__CHECK__");
    run.log(DEBUG, "Launching JUnit Platform Console: %s", junit.list);
    run.log(DEBUG, "Using class loader: %s - %s", loader.getName(), loader);
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
      run.out.write(out.toString());
      run.out.flush();
      run.err.write(err.toString());
      run.err.flush();
      var code = (int) result.getClass().getMethod("getExitCode").invoke(result);
      if (code != 0) {
        throw new AssertionError("JUnit run exited with code " + code);
      }
    } catch (Throwable t) {
      throw new Error("ConsoleLauncher.execute(...) failed: " + t, t);
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }
}
