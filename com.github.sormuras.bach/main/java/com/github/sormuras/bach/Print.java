package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.Paths;
import java.io.PrintStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

/** Print-related API. */
public /*sealed*/ interface Print /*permits Bach*/ {

  /**
   * Returns the print stream for printing messages.
   *
   * @return the print stream for printing messages
   */
  PrintStream printStream();

  /**
   * Print a listing of all files matching the given glob pattern.
   *
   * @param glob the glob pattern
   */
  default void printFind(String glob) {
    Paths.find(Path.of(""), glob, path -> printStream().println(Paths.slashed(path)));
  }

  /**
   * Print a sorted list of all modules locatable by the given module finder.
   *
   * @param finder the module finder to query for modules
   */
  default void printModules(ModuleFinder finder) {
    finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::toNameAndVersion)
        .sorted()
        .forEach(printStream()::println);
  }

  /**
   * Print a description of the given module locatable by the given module finder.
   *
   * @param finder the module finder to query for modules
   * @param module the name of the module to describe
   */
  default void printModuleDescription(ModuleFinder finder, String module) {
    finder
        .find(module)
        .ifPresentOrElse(
            reference -> Modules.describeModule(printStream(), reference),
            () -> printStream().println("No such module found: " + module));
  }

  /**
   * Print a sorted list of all provided tools locatable by the given module finder.
   *
   * @param finder the module finder to query for tool providers
   */
  default void printToolProviders(ModuleFinder finder) {
    ServiceLoader.load(Modules.layer(finder), ToolProvider.class).stream()
        .map(ServiceLoader.Provider::get)
        .map(Print::describe)
        .sorted()
        .forEach(printStream()::println);
  }

  private static String describe(ToolProvider tool) {
    var name = tool.name();
    var module = tool.getClass().getModule();
    var by = Optional.ofNullable(module.getDescriptor())
        .map(ModuleDescriptor::toNameAndVersion)
        .orElse(module.toString());
    var info = switch (name) {
      case "jar" -> "Create an archive for classes and resources, and update or restore resources";
      case "javac" -> "Read Java class and interface definitions and compile them into class files";
      case "javadoc" -> "Generate HTML pages of API documentation from Java source files";
      case "javap" -> "Disassemble one or more class files";
      case "jdeps" -> "Launch the Java class dependency analyzer";
      case "jlink" -> "Assemble and optimize a set of modules into a custom runtime image";
      case "jmod" -> "Create JMOD files and list the content of existing JMOD files";
      case "jpackage" -> "Package a self-contained Java application";
      case "junit" -> "Launch the JUnit Platform";
      default -> tool.toString();
    };
    return "%s (provided by module %s)\n%s".formatted(name, by, info.indent(2));
  }
}
