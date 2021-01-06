package com.github.sormuras.bach.internal;

import java.lang.module.ModuleDescriptor;
import java.util.Optional;
import java.util.spi.ToolProvider;

public class ToolProviders {

  public static String describe(ToolProvider provider) {
    var name = provider.name();
    var module = provider.getClass().getModule();
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
          default -> provider.toString();
        };
    return "%s (provided by module %s)\n%s".formatted(name, by, info.indent(2)).trim();
  }

  /** Hidden default constructor. */
  private ToolProviders() {}
}
