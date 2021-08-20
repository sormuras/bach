package com.github.sormuras.bach.internal;

import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;

/** Static utility methods for operating on instances of {@link ModuleDescriptor}. */
public sealed interface ModuleDescriptorSupport permits ConstantInterface {

  /**
   * Reads the source form of a module declaration from a file as a module descriptor.
   *
   * @param info the path to a {@code module-info.java} file to parse
   * @return the module descriptor
   * @implNote For the time being, only the {@code name} of a module and its {@code requires}
   *     directives are parsed.
   */
  static ModuleDescriptor parse(Path info) {
    if (!Path.of("module-info.java").equals(info.getFileName()))
      throw new IllegalArgumentException("Path must end with 'module-info.java': " + info);

    var compiler = ToolProvider.getSystemJavaCompiler();
    var writer = new PrintWriter(Writer.nullWriter());
    var fileManager = compiler.getStandardFileManager(null, null, null);
    var units = List.of(new ModuleInfoJavaFileObject(info));
    var javacTask = (JavacTask) compiler.getTask(writer, fileManager, null, null, null, units);

    try {
      for (var tree : javacTask.parse()) {
        var module = tree.getModule();
        if (module == null) throw new AssertionError("No module tree?! -> " + info);
        return parse(module);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Parse failed for " + info, e);
    }
    throw new IllegalArgumentException("Module tree not found in " + info);
  }

  private static ModuleDescriptor parse(ModuleTree moduleTree) {
    var moduleName = moduleTree.getName().toString();
    var builder = ModuleDescriptor.newModule(moduleName);
    for (var directive : moduleTree.getDirectives()) {
      if (directive instanceof RequiresTree requires) {
        var requiresModuleName = requires.getModuleName().toString();
        builder.requires(requiresModuleName);
      }
    }
    return builder.build();
  }
}
