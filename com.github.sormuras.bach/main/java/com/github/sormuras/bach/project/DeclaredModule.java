package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** A module declaration with its possibly targeted folders. */
public record DeclaredModule(
    ModuleDescriptor descriptor, Path info, Optional<String> mainClass, Folders folders)
    implements Comparable<DeclaredModule> {

  @FunctionalInterface
  public interface Operator extends UnaryOperator<DeclaredModule> {}

  public record Tweak(ProjectSpace space, DeclaredModule module, Operator operator) {}

  public String name() {
    return descriptor.name();
  }

  public static DeclaredModule of(Path pathOfModuleInfoJavaOrItsParentDirectory) {
    return DeclaredModule.of(pathOfModuleInfoJavaOrItsParentDirectory, 0, FolderType.SOURCES);
  }

  public static DeclaredModule of(
      Path pathOfModuleInfoJavaOrItsParentDirectory, int version, FolderType... types) {
    var path = pathOfModuleInfoJavaOrItsParentDirectory.normalize();
    if (Files.notExists(path)) throw new IllegalArgumentException("Path must exist: " + path);
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    if (Files.notExists(info)) throw new IllegalArgumentException("No module-info in: " + path);
    var descriptor = ModuleDescriptorSupport.parse(info);
    var folders = Folders.of(new Folder(info.getParent(), version, FolderTypes.of(types)));
    return new DeclaredModule(descriptor, info, Optional.empty(), folders);
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
    var folder = new Folder(directory.normalize(), release, FolderTypes.of(types));
    return new DeclaredModule(descriptor, info, mainClass, folders.add(folder));
  }
}
