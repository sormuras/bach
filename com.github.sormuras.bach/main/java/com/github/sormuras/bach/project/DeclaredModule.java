package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** A module declaration with its possibly targeted folders. */
public record DeclaredModule(
    ModuleDescriptor descriptor, Path info, Optional<String> mainClass, TargetedFolders folders)
    implements Comparable<DeclaredModule> {

  @FunctionalInterface
  public interface Operator extends UnaryOperator<DeclaredModule> {}

  public record Tweak(ProjectSpace space, DeclaredModule module, Operator operator) {}

  public static DeclaredModule of(String pathOfModuleInfoJavaFileOrItsParentDirectory) {
    return DeclaredModule.of(Path.of(pathOfModuleInfoJavaFileOrItsParentDirectory));
  }

  public static DeclaredModule of(Path pathOfModuleInfoJavaFileOrItsParentDirectory) {
    var path = pathOfModuleInfoJavaFileOrItsParentDirectory.normalize();
    if (Files.notExists(path)) throw new IllegalArgumentException("Path must exist: " + path);
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    if (Files.notExists(info)) throw new IllegalArgumentException("No module-info in: " + path);
    var descriptor = ModuleDescriptorSupport.parse(info);
    var parent = info.getParent();
    var directory = parent != null ? parent : Path.of(".");
    var types = FolderTypes.of(FolderType.SOURCES);
    var folders = TargetedFolders.of(new TargetedFolder(directory, 0, types));
    return new DeclaredModule(descriptor, info, Optional.empty(), folders);
  }

  public String name() {
    return descriptor.name();
  }

  @Override
  public int compareTo(DeclaredModule other) {
    return name().compareTo(other.name());
  }

  DeclaredModule tweak(Tweak tweak) {
    return this == tweak.module ? tweak.operator.apply(this) : this;
  }

  public DeclaredModule withMainClass(String name) {
    return new DeclaredModule(descriptor, info, Optional.ofNullable(name), folders);
  }

  public DeclaredModule withSourcesFolder(String directory) {
    return withSourcesFolder(directory, 0);
  }

  public DeclaredModule withSourcesFolder(String directory, int release) {
    return withFolder(Path.of(directory), release, FolderType.SOURCES);
  }

  public DeclaredModule withResourcesFolder(String directory) {
    return withResourcesFolder(directory, 0);
  }

  public DeclaredModule withResourcesFolder(String directory, int release) {
    return withFolder(Path.of(directory), release, FolderType.RESOURCES);
  }

  public DeclaredModule withFolder(Path directory, int release, FolderType... types) {
    var folder = new TargetedFolder(directory.normalize(), release, FolderTypes.of(types));
    return new DeclaredModule(descriptor, info, mainClass, folders.add(folder));
  }
}
