package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.project.ModuleDirectory;
import com.github.sormuras.bach.project.ExternalModule;
import com.github.sormuras.bach.project.ModuleLookup;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolRunner;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** An enviroment used by Bach's Boot Script. */
@SuppressWarnings("unused")
public class ShellEnvironment {

  private static final Consumer<Object> out = System.out::println;
  private static final Consumer<Object> err = System.err::println;

  private static final Bach bach = Bach.ofSystem();
  private static ModuleDirectory moduleDirectory = new ModuleDirectory(Set.of(), Map.of(), List.of());
  private static final ModuleLookup moduleSearcher =
      ModuleLookup.compose(moduleDirectory::lookup, ModuleLookup.ofBestEffort(bach));

  /**
   * Builds a project by delegating to the default build sequence.
   *
   * @param args the array of arguments
   */
  public static void build(String... args) {
    BuildProgram.execute(bach, args);
  }

  /**
   * Prints a module description of the given module.
   *
   * @param module the name of the module to describe
   */
  public static void describeModule(String module) {
    describeModule(module, ModuleFinder.compose(ModuleFinder.ofSystem(), moduleDirectory.finder()));
  }

  /**
   * Prints a description of the given module locatable by the given module finder.
   *
   * @param module the name of the module to describe
   * @param finder the module finder to query for modules
   */
  private static void describeModule(String module, ModuleFinder finder) {
    finder
        .find(module)
        .ifPresentOrElse(
            reference -> out.accept(Modules.describeModule(reference)),
            () -> err.accept("No such module found: " + module));
  }

  /**
   * Prints a listing of all files matching the given glob pattern.
   *
   * @param glob the glob pattern
   */
  public static void find(String glob) {
    Paths.find(Path.of(""), glob, path -> out.accept(Paths.slashed(path)));
  }

  /**
   * Links the given module name to the specified target, usually a URI of a remote JAR file.
   *
   * @param module the name of the module to link
   * @param target the target of the link, usually a URI of a remote JAR file
   * @return the possibly expanded target of the link
   */
  public static String linkModule(String module, String target) {
    var link = ExternalModule.link(module).to(target);
    moduleDirectory = linkModule(link);
    return link.uri();
  }

  private static ModuleDirectory linkModule(ExternalModule link, ExternalModule... more) {
    var copy = new HashMap<>(moduleDirectory.links());
    copy.put(link.module(), link);
    Arrays.stream(more).forEach(next -> copy.put(next.module(), next));
    return new ModuleDirectory(moduleDirectory.requires(), Map.copyOf(copy), moduleDirectory.lookups());
  }

  /** Prints a list of all loaded modules. */
  public static void listLoadedModules() {
    printModules(moduleDirectory.finder());
  }

  /** Prints all missing modules. */
  public static void listMissingModules() {
    moduleDirectory.missing().forEach(out);
  }

  /** Prints all module links. */
  public static void listModuleLinks() {
    moduleDirectory.stream().sorted().forEach(out);
  }

  /** Prints a list of all system modules. */
  public static void listSystemModules() {
    printModules(ModuleFinder.ofSystem());
  }

  /**
   * Prints all available tool provider implementations.
   *
   * @see ToolProvider
   */
  public static void listToolProviders() {
    printToolProviders(moduleDirectory.finder());
  }

  /**
   * Loads the given module.
   *
   * @param module the name of the module to load
   */
  public static void loadModule(String module) {
    bach.loadModule(moduleDirectory, moduleSearcher, module);
  }

  /** Loads all missing modules. */
  public static void loadMissingModules() {
    bach.loadMissingModules(moduleDirectory, moduleSearcher);
  }

  /**
   * Run the named tool passing the given arguments.
   *
   * @param tool the name of the tool to run
   * @param args the array of args to be passed to the tool as strings
   */
  public static void run(String tool, Object... args) {
    var call = Command.of(tool, args);
    var response = new ToolRunner(moduleDirectory.finder()).run(call);
    if (!response.out().isEmpty()) out.accept(response.out());
    if (!response.err().isEmpty()) err.accept(response.err());
    if (response.isError()) throw new Error(tool + " returned " + response.code());
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
    var s = all.size() == 1 ? "" : "s";
    out.accept(String.format("-> %d module%s", all.size(), s));
  }

  /**
   * Print a sorted list of all provided tools locatable by the given module finder.
   *
   * @param finder the module finder to query for tool providers
   */
  private static void printToolProviders(ModuleFinder finder) {
    ServiceLoader.load(Modules.layer(finder), ToolProvider.class).stream()
        .map(ServiceLoader.Provider::get)
        .map(ShellEnvironment::describe)
        .sorted()
        .forEach(out);
  }

  private static String describe(ToolProvider tool) {
    var name = tool.name();
    var module = tool.getClass().getModule();
    var by =
        Optional.ofNullable(module.getDescriptor())
            .map(ModuleDescriptor::toNameAndVersion)
            .orElse(module.toString());
    var info =
        switch (name) {
          case "jar" -> "Create an archive for classes and resources, and update or restore them";
          case "javac" -> "Read Java class and interface definitions and compile them into classes";
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

  /** Hidden default constructor. */
  private ShellEnvironment() {}
}
