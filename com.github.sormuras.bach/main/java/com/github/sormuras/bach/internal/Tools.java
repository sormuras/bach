package com.github.sormuras.bach.internal;

import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolutionException;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** Tool-related utilities. */
public class Tools {

  /**
   * Returns a description of the passed tool provider instance.
   *
   * @param tool the tool to describe.
   * @return a description of the passed tool
   */
  public static String describe(ToolProvider tool) {
    return switch (tool.name()) {
      case "jar" -> "Create an archive for classes and resources, and update or restore resources";
      case "javac" -> "Read Java class and interface definitions and compile them into class files";
      case "javadoc" -> "Generate HTML pages of API documentation from Java source files";
      case "javap" -> "Disassemble one or more class files";
      case "jdeps" -> "Launch the Java class dependency analyzer";
      case "jlink" -> "Assemble and optimize a set of modules into a custom runtime image";
      case "jmod" -> "Create JMOD files and list the content of existing JMOD files";
      case "jpackage" -> "Package a self-contained Java application";
      default -> tool.toString();
    };
  }

  public static List<ToolProvider> find(ModuleFinder finder) {
    try {
      var layer = Modules.layer(finder);
      var services = ServiceLoader.load(layer, ToolProvider.class);
      return services.stream()
          .map(ServiceLoader.Provider::get)
          .collect(Collectors.toUnmodifiableList());
    } catch (FindException | ResolutionException exception) {
      var message = new StringJoiner(System.lineSeparator());
      message.add(exception.getMessage());
      message.add("Finder finds module(s):");
      finder.findAll().stream()
          .sorted(Comparator.comparing(ModuleReference::descriptor))
          .forEach(reference -> message.add("\t" + reference));
      message.add("");
      throw new RuntimeException(message.toString(), exception);
    }
  }

  private Tools() {}
}
