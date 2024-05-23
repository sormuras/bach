/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.internal;

import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
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
    if (Files.notExists(info))
      throw new IllegalArgumentException("Module not found: " + info.toAbsolutePath().toUri());

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

  record ModuleReferenceFinder(Collection<? extends ModuleReference> references)
      implements ModuleFinder {
    @Override
    public Optional<ModuleReference> find(String name) {
      return references.stream()
          .filter(reference -> reference.descriptor().name().equals(name))
          .map(ModuleReference.class::cast)
          .findFirst();
    }

    @Override
    public Set<ModuleReference> findAll() {
      return Set.copyOf(references);
    }
  }

  class ModuleInfoReference extends ModuleReference {
    private final Path info;

    public ModuleInfoReference(Path info, ModuleDescriptor descriptor) {
      super(descriptor, info.toUri());
      this.info = info;
    }

    @Override
    public boolean equals(Object object) {
      return this == object || object instanceof ModuleInfoReference ref && info.equals(ref.info);
    }

    @Override
    public int hashCode() {
      return info.hashCode();
    }

    public Path info() {
      return info;
    }

    public String name() {
      return descriptor().name();
    }

    @Override
    public ModuleReader open() {
      return new NullModuleReader();
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "[info=" + info + ']';
    }
  }

  class NullModuleReader implements ModuleReader {
    @Override
    public Optional<URI> find(String name) {
      return Optional.empty();
    }

    @Override
    public Stream<String> list() {
      return Stream.empty();
    }

    @Override
    public void close() {}
  }
}
