package com.github.sormuras.bach.internal;

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.module.Configuration.resolveAndBind;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;

public record ExecuteModuleToolProvider(ModuleFinder finder) implements ToolProvider {

  @Override
  public String name() {
    return "execute-module";
  }

  private String main(String name) {
    var reference = finder.find(name);
    if (reference.isEmpty()) throw new RuntimeException("Module %s not found".formatted(name));
    var descriptor = reference.get().descriptor();
    var mainClassName = descriptor.mainClass();
    if (mainClassName.isEmpty()) throw new RuntimeException("No main class present in " + name);
    return mainClassName.get();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      err.println("Usage: execute-module MODULE[/MAINCLASS] ARGS...");
      return 1;
    }
    var module = args[0];

    var split = module.split("/");
    var name = split[0];
    var main = split.length > 1 ? split[1] : main(name);

    var parentClassLoader = ClassLoader.getPlatformClassLoader();
    var parentModuleLayer = ModuleLayer.boot();
    var parents = List.of(parentModuleLayer.configuration());
    var roots = Set.of(name);
    var configuration = resolveAndBind(ModuleFinder.of(), parents, finder, roots);
    var layers = List.of(parentModuleLayer);
    var controller = defineModulesWithOneLoader(configuration, layers, parentClassLoader);
    var layer = controller.layer();
    var loader = layer.findLoader(name);

    var systemOut = System.out;
    var systemErr = System.err;
    var bufferOut = new ByteArrayOutputStream(1024);
    var bufferErr = new ByteArrayOutputStream(1024);
    System.setOut(new PrintStream(bufferOut, false, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(bufferErr, false, StandardCharsets.UTF_8));

    try {
      var mainClass = loader.loadClass(main);
      var mainModule = layer.findModule(name).orElseThrow();
      var mainMethod = mainClass.getMethod("main", String[].class);
      var mainPackage = mainClass.getPackageName();
      var thisModule = getClass().getModule();
      if (!mainModule.isOpen(mainPackage, thisModule)) {
        controller.addOpens(mainModule, mainPackage, thisModule);
      }
      if (!mainMethod.canAccess(null /* as required by a static element */)) {
        if (!mainMethod.trySetAccessible()) {
          throw new IllegalAccessException("Cannot access " + mainMethod);
        }
      }
      // TODO Capture exit code from "System.exit(code);" calls and prevent shutdown of this JVM.
      mainMethod.invoke(null, (Object) args);
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(System.err);
      return 1;
    } finally {
      System.out.flush(); // flushes bufferOut
      System.err.flush(); // flushes bufferErr
      System.setOut(systemOut);
      System.setErr(systemErr);
      out.println(bufferOut);
      err.println(bufferErr);
    }
  }
}
