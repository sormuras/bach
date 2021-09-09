package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;

/** A module declaration with its possibly targeted folders. */
public record DeclaredModule(ModuleDescriptor descriptor, Path info, TargetedFolders folders)
    implements Comparable<DeclaredModule> {

  public static DeclaredModule of(String pathOfModuleInfoJavaFileOrItsParentDirectory) {
    var path = Path.of(pathOfModuleInfoJavaFileOrItsParentDirectory).normalize();
    if (Files.notExists(path)) throw new IllegalArgumentException("Path must exist: " + path);
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    if (Files.notExists(info)) throw new IllegalArgumentException("No module-info in: " + path);
    var descriptor = ModuleDescriptorSupport.parse(info);
    var parent = info.getParent();
    var directory = parent != null ? parent : Path.of(".");
    var types = FolderTypes.of(FolderType.SOURCES);
    var folders = TargetedFolders.of(new TargetedFolder(directory, 0, types));
    return new DeclaredModule(descriptor, info, folders);
  }

  public String name() {
    return descriptor.name();
  }

  @Override
  public int compareTo(DeclaredModule other) {
    return name().compareTo(other.name());
  }

  public DeclaredModule foldersAddSourcesFolder(String directory) {
    return foldersAddSourcesFolder(directory, 0);
  }

  public DeclaredModule foldersAddSourcesFolder(String directory, int release) {
    return foldersAddFolder(Path.of(directory), release, FolderType.SOURCES);
  }

  public DeclaredModule foldersAddResourcesFolder(String directory) {
    return foldersAddResourcesFolder(directory, 0);
  }

  public DeclaredModule foldersAddResourcesFolder(String directory, int release) {
    return foldersAddFolder(Path.of(directory), release, FolderType.RESOURCES);
  }

  public DeclaredModule foldersAddFolder(Path directory, int release, FolderType... types) {
    var folder = new TargetedFolder(directory.normalize(), release, FolderTypes.of(types));
    return new DeclaredModule(descriptor, info, folders.add(folder));
  }
}
