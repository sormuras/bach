package run.bach.internal;

import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import javax.tools.SimpleJavaFileObject;

/** Static utility methods for operating on instances of {@link ModuleDescriptor}. */
public interface ModuleDescriptorSupport {

  /**
   * Reads the source form of a module declaration from a file as a module descriptor.
   *
   * @param info the path to a {@code module-info.java} file to parse
   * @return the module descriptor
   * @implNote For the time being, only the {@code kind}, the {@code name} and its {@code requires}
   *     directives are parsed.
   */
  static ModuleDescriptor parse(Path info) {
    if (!Path.of("module-info.java").equals(info.getFileName()))
      throw new IllegalArgumentException("Path must end with 'module-info.java': " + info);

    var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
    var writer = new PrintWriter(Writer.nullWriter());
    var fileManager = compiler.getStandardFileManager(null, null, null);
    var units = List.of(new ModuleInfoFileObject(info));
    var javacTask = (JavacTask) compiler.getTask(writer, fileManager, null, null, null, units);

    try {
      for (var tree : javacTask.parse()) {
        var module = tree.getModule();
        if (module == null) throw new AssertionError("No module tree?! -> " + info);
        return parse(module);
      }
    } catch (Exception e) {
      throw new RuntimeException("Parse failed for " + info, e);
    }
    throw new IllegalArgumentException("Module tree not found in " + info);
  }

  private static ModuleDescriptor parse(ModuleTree moduleTree) {
    var moduleName = moduleTree.getName().toString();
    var moduleModifiers =
        moduleTree.getModuleType().equals(ModuleTree.ModuleKind.OPEN)
            ? EnumSet.of(ModuleDescriptor.Modifier.OPEN)
            : EnumSet.noneOf(ModuleDescriptor.Modifier.class);
    var moduleBuilder = ModuleDescriptor.newModule(moduleName, moduleModifiers);
    for (var directive : moduleTree.getDirectives()) {
      if (directive instanceof RequiresTree requires) {
        var requiresModuleName = requires.getModuleName().toString();
        moduleBuilder.requires(requiresModuleName);
      }
    }
    return moduleBuilder.build();
  }

  class ModuleInfoFileObject extends SimpleJavaFileObject {
    ModuleInfoFileObject(Path path) {
      super(path.toUri(), Kind.SOURCE);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
      return Files.readString(Path.of(uri));
    }
  }
}
