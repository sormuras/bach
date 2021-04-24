package com.github.sormuras.bach.core;

import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import java.lang.module.ModuleFinder;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public record ToolProviders(ModuleFinder finder) {

  public static ToolProviders of(ModuleFinder finder) {
    return new ToolProviders(finder);
  }

  public static String nameAndModule(ToolProvider provider) {
    var module = provider.getClass().getModule();
    var descriptor = module.getDescriptor();
    var container = descriptor == null ? module : descriptor.toNameAndVersion();
    return "%-20s (%s)".formatted(provider.name(), container);
  }

  public static String describe(ToolProvider provider) {
    var name = provider.name();
    var info =
        switch (name) {
          case "bach" -> """
                Builds (on(ly)) Java Modules
                https://github.com/sormuras/bach""";
          case "google-java-format" -> "Reformat Java sources to comply with Google Java Style";
          case "jar" -> "Create an archive for classes and resources, and update or restore them";
          case "javac" -> "Read Java compilation units (*.java) and compile them into classes";
          case "javadoc" -> "Generate HTML pages of API documentation from Java source files";
          case "javap" -> "Disassemble one or more class files";
          case "jdeps" -> "Launch the Java class dependency analyzer";
          case "jlink" -> "Assemble and optimize a set of modules into a custom runtime image";
          case "jmod" -> "Create JMOD files and list the content of existing JMOD files";
          case "jpackage" -> "Package a self-contained Java application";
          case "junit" -> "Launch the JUnit Platform";
          default -> provider.toString();
        };
    return """
       %s
       %s""".formatted(nameAndModule(provider), info.indent(4).stripTrailing());
  }

  public Optional<ToolProvider> find(String name) {
    return stream().filter(it -> it.name().equals(name)).findFirst();
  }

  public Stream<ToolProvider> stream(String... roots) {
    return stream(false,roots);
  }

  public Stream<ToolProvider> stream(boolean assertions, String... roots) {
    var layer =
        new ModuleLayerBuilder()
            .before(finder)
            .after(ModuleFinder.ofSystem()) // resolve modules that are not in the module graph
            .roots(Set.of(roots))
            .build();
    Stream.of(roots).map(layer::findLoader).forEach(l -> l.setDefaultAssertionStatus(assertions));
    return ServiceLoader.load(layer, ToolProvider.class).stream().map(ServiceLoader.Provider::get);
  }
}
