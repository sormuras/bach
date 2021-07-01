package com.github.sormuras.bach.internal;

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.lang.model.element.ModuleElement;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** {@link ModuleDescriptor}-related utilities. */
public class ModuleDescriptors {

  private static final TreeSet<String> SYSTEM_MODULE_NAMES =
      ModuleFinder.ofSystem().findAll().stream()
          .map(ModuleReference::descriptor)
          .map(ModuleDescriptor::name)
          .collect(Collectors.toCollection(TreeSet::new));

  /**
   * Reads the source form of a module declaration from a file as a module descriptor.
   *
   * @param info the path to a {@code module-info.java} file to parse
   * @return the module descriptor
   * @implNote For the time being, only the {@code name} of a module and its {@code requires}
   *     directives are parsed.
   */
  public static ModuleDescriptor parse(Path info) {
    if (!Path.of("module-info.java").equals(info.getFileName()))
      throw new IllegalArgumentException("Path must end with 'module-info.java': " + info);

    var compiler = ToolProvider.getSystemJavaCompiler();
    var writer = new PrintWriter(Writer.nullWriter());
    var fileManager = compiler.getStandardFileManager(null, null, null);
    var units = List.of(new ModuleInfoFileObject(info));
    var javacTask = (JavacTask) compiler.getTask(writer, fileManager, null, null, null, units);

    var elements = javacTask.getElements();
    elements.getModuleElement("java.base");
    for (var module : elements.getAllModuleElements()) {
      if (SYSTEM_MODULE_NAMES.contains(module.getQualifiedName().toString())) continue;
      return describe(module);
    }
    throw new IllegalArgumentException("Module element not found for " + info);
  }

  static class ModuleInfoFileObject extends SimpleJavaFileObject {
    ModuleInfoFileObject(Path path) {
      super(path.toUri(), Kind.SOURCE);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
      return Files.readString(Path.of(uri));
    }
  }

  static ModuleDescriptor describe(ModuleElement element) {
    var name = element.getQualifiedName().toString();
    var module = ModuleDescriptor.newModule(name);
    for (var directive : element.getDirectives()) {
      if (directive instanceof ModuleElement.RequiresDirective requires) {
        var requiresModuleName = requires.getDependency().toString();
        module.requires(requiresModuleName);
      }
    }
    return module.build();
  }

  /** Hidden default constructor. */
  private ModuleDescriptors() {}
}
