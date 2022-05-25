package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record DeclaredModule(
    Path content, // content root of the entire module
    Path info, // "module-info.java"
    ModuleDescriptor descriptor, // descriptor.name()
    Folders base, // base sources and resources
    Map<Integer, Folders> targeted)
    implements Comparable<DeclaredModule> {

  public static DeclaredModule of(Path root, Path moduleInfoJavaFileOrItsParentDirectory) {
    var info =
        moduleInfoJavaFileOrItsParentDirectory.endsWith("module-info.java")
            ? moduleInfoJavaFileOrItsParentDirectory
            : moduleInfoJavaFileOrItsParentDirectory.resolve("module-info.java");
    var descriptor = ModuleDescriptorSupport.parse(info.normalize());

    // trivial case: "module-info.java" resides directly in content root directory
    if (root.resolve("module-info.java").toUri().equals(info.toUri())) {
      return new DeclaredModule(root, info, descriptor, Folders.of(root), Map.of());
    }

    // try to find module name in path elements
    var name = descriptor.name();

    var parent = info.getParent();
    while (parent != null
        && !parent.equals(root)
        && !parent.getFileName().toString().equals(name)) {
      parent = parent.getParent();
    }

    if (parent == null || parent.equals(root))
      throw new UnsupportedOperationException("Module name not in path: " + info);
    var content = parent;

    return of(content, info, descriptor);
  }

  static DeclaredModule of(Path content, Path info, ModuleDescriptor descriptor) {
    // "module-info.java" resides in a subdirectory, usually named "java" or "java-module"
    var parent = info.getParent();
    if (parent == null) throw new UnsupportedOperationException("No parent of: " + info);
    var container = parent.getParent();
    if (container == null) throw new UnsupportedOperationException("No container of: " + parent);
    // find base siblings
    var base = Folders.of(parent).withSiblings(container);
    // find targeted siblings
    var targeted = new TreeMap<Integer, Folders>();
    for (int release = 9; release <= Runtime.version().feature(); release++) {
      var folders = Folders.of().withSiblings(container, release);
      if (folders.isEmpty()) continue;
      targeted.put(release, folders);
    }
    return new DeclaredModule(content, info, descriptor, base, Map.copyOf(targeted));
  }

  public String name() {
    return descriptor.name();
  }

  public List<Path> baseSourcePaths() {
    return base.sources();
  }

  @Override
  public int compareTo(DeclaredModule other) {
    return name().compareTo(other.name());
  }
}
