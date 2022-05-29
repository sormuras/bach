package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

    var relativized = root.relativize(info).normalize(); // ensure info is below root
    var descriptor = ModuleDescriptorSupport.parse(info);
    var name = descriptor.name();

    // trivial case: "module-info.java" resides directly in content root directory
    var system = root.getFileSystem();
    if (system.getPathMatcher("glob:module-info.java").matches(relativized)) {
      var base = Folders.of(root);
      return new DeclaredModule(root, info, descriptor, base, Map.of());
    }
    // "java" case: "module-info.java" in direct "java" subdirectory with targeted resources
    if (system.getPathMatcher("glob:java/module-info.java").matches(relativized)) {
      var base = Folders.of().withSiblings(root);
      var targeted = Folders.mapFoldersByJavaFeatureReleaseNumber(root);
      return new DeclaredModule(root, info, descriptor, base, targeted);
    }
    // "<module>" case: "module-info.java" in direct subdirectory with the same name as the module
    if (system.getPathMatcher("glob:" + name + "/module-info.java").matches(relativized)) {
      var content = root.resolve(name);
      var base = Folders.of(content).withSiblings(content);
      var targeted = Folders.mapFoldersByJavaFeatureReleaseNumber(content);
      return new DeclaredModule(content, info, descriptor, base, targeted);
    }

    // try to find module name in path elements
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
    var targeted = Folders.mapFoldersByJavaFeatureReleaseNumber(container);
    return new DeclaredModule(content, info, descriptor, base, targeted);
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
