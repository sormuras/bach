package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.api.ModuleOrigin;
import com.github.sormuras.bach.tool.ExternalToolCall;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public record ToolProviders(ModuleFinder before, ModuleFinder after, Path tools) {

  public static ToolProviders of(ModuleFinder finder, Path externalTools) {
    return new ToolProviders(finder, ModuleFinder.ofSystem(), externalTools);
  }

  public static String nameAndModule(ToolProvider provider) {
    if (provider instanceof ExternalToolCall tool) {
      return "%-20s (%s)".formatted(provider.name(), tool.jar());
    }
    var module = provider.getClass().getModule();
    var realm = ModuleOrigin.of(module);
    var descriptor = module.getDescriptor();
    var container = descriptor == null ? module : descriptor.toNameAndVersion();
    return "%-20s (%s, %s)".formatted(provider.name(), realm, container);
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
       %s"""
        .formatted(nameAndModule(provider), info.indent(4).stripTrailing());
  }

  public static Stream<ToolProvider> streamExternalTools(Path root) {
    if (Files.notExists(root)) return Stream.empty();
    var directories = Paths.list(root, Files::isDirectory);
    var tools = new ArrayList<ToolProvider>();
    for (var directory : directories) {
      var jars = Paths.list(directory, Paths::isJarFile);
      if (jars.size() != 1) continue;
      tools.add(new ExternalToolCall(Strings.name(directory), jars.get(0), List.of()));
    }
    return tools.stream();
  }

  public Optional<ToolProvider> find(String name) {
    return stream().filter(it -> it.name().equals(name)).findFirst();
  }

  public Stream<ToolProvider> stream(String... roots) {
    return stream(false, roots);
  }

  public Stream<ToolProvider> stream(boolean assertions, String... roots) {
    var layer = new ModuleLayerBuilder().before(before).after(after).roots(Set.of(roots)).build();
    Stream.of(roots).map(layer::findLoader).forEach(l -> l.setDefaultAssertionStatus(assertions));
    var services = ServiceLoader.load(layer, ToolProvider.class).stream().map(ServiceLoader.Provider::get);
    var executables = streamExternalTools(tools);
    return Stream.concat(services, executables);
  }
}
