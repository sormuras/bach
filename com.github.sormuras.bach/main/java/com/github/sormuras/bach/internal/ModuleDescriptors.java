package com.github.sormuras.bach.internal;

import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.spi.ToolProvider;

public class ModuleDescriptors {

  public static ModuleDescriptor parse(Path path) {
    if (!Path.of("module-info.java").equals(path.getFileName()))
      throw new IllegalArgumentException("Path must end with 'module-info.java': " + path);

    var javac = ToolProvider.findFirst("javac").orElseThrow();
    var text = new StringWriter();
    var writer = new PrintWriter(text);
    var code = javac.run(writer, writer, "-proc:only", "-Xplugin:" + Parser.NAME, path.toString());
    if (code != 0) throw new RuntimeException("Parse failed: " + path + "\n" + text);
    var descriptor = Parser.descriptors.get(path);
    if (descriptor == null) throw new IllegalStateException("Descriptor not found: " + path);
    return descriptor;
  }

  public record Parser() implements Plugin, TaskListener {

    private static final Map<Path, ModuleDescriptor> descriptors = new ConcurrentHashMap<>();

    public static final String NAME = "ModuleDescriptors.Parser";

    @Override
    public String getName() {
      return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
      task.addTaskListener(this);
    }

    @Override
    public void finished(TaskEvent event) {
      if (event.getKind() != TaskEvent.Kind.PARSE) return;
      var unit = event.getCompilationUnit();
      if (unit == null) return;
      var path = Path.of(unit.getSourceFile().getName());
      for (var declaration : unit.getTypeDecls()) {
        if (declaration instanceof ModuleTree module) descriptors.put(path, describe(module));
      }
    }

    private ModuleDescriptor describe(ModuleTree module) {
      var moduleName = module.getName().toString();
      var builder = ModuleDescriptor.newModule(moduleName);
      for (var directive : module.getDirectives()) {
        if (directive instanceof RequiresTree requires) {
          var requiresModuleName = requires.getModuleName().toString();
          builder.requires(requiresModuleName);
        }
      }
      return builder.build();
    }
  }
}
