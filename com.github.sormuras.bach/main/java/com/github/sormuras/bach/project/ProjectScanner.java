package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Configuration;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.internal.ModuleDescriptorSupport;
import com.github.sormuras.bach.internal.PathSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A directory-scanning factory of {@link Project} instances. */
public record ProjectScanner(Bach bach, boolean treatSourcesAsResources) implements Logbook.Trait {

  public ProjectScanner(Bach bach) {
    this(bach, false);
  }

  private static final Pattern RELEASE_NUMBER_PATTERN = Pattern.compile(".*?(\\d+)$");

  @Override
  public Logbook logbook() {
    return bach.logbook();
  }

  public Project scanProject() {
    return scanProject(bach.path().root());
  }

  public Project scanProject(Path directory) {
    log("Parse directory %s".formatted(directory));
    if (!Files.isDirectory(directory)) throw log(new Error("Not a directory: " + directory));

    var projectName =
        bach.configuration()
            .projectOptions()
            .name()
            .orElse(Configuration.computeDefaultProjectName(directory));
    var projectVersion =
        bach.configuration()
            .projectOptions()
            .version()
            .orElse(Configuration.computeDefaultProjectVersion());
    log("Find all module-info.java files of %s %s...".formatted(projectName, projectVersion));

    var projectSpaces = scanProjectSpaces(directory);

    return Project.of(projectName, projectVersion).with(projectSpaces);
  }

  public ProjectSpaces scanProjectSpaces(Path directory) {
    var moduleInfoFiles = bach.explorer().findModuleInfoJavaFiles(directory);
    var size = moduleInfoFiles.size();
    log("Found %d module-info.java file%s".formatted(size, size == 1 ? "" : "s"));
    if (size == 0) throw log(new Error("No module-info.java found"));

    var tests = new TreeMap<String, DeclaredModule>();
    var mains = new TreeMap<String, DeclaredModule>();

    var isTestModule = isModuleOf("test", "test");
    var isMainModule = isModuleOf("main", "*");

    for (var path : moduleInfoFiles) {
      if (path.startsWith(".bach")) continue;
      var declaration = scanDeclaredModule(path);
      if (isTestModule.test(path)) {
        tests.put(declaration.name(), declaration);
        continue;
      }
      if (isMainModule.test(path)) {
        mains.put(declaration.name(), declaration);
        continue;
      }
      throw new IllegalStateException("Path not handled: " + path);
    }

    var mainSpace =
        new ProjectSpace(
            "main",
            List.of(),
            // TODO bach.configuration().projectOptions().mainRelease().orElse(0)
            0,
            new DeclaredModules(List.copyOf(mains.values())));
    var testSpace =
        new ProjectSpace(
            "test",
            List.of(mainSpace),
            Runtime.version().feature(), // or 0
            new DeclaredModules(List.copyOf(tests.values())));

    var spaces = new ArrayList<ProjectSpace>();
    if (!mains.isEmpty()) spaces.add(mainSpace);
    if (!tests.isEmpty()) spaces.add(testSpace);
    if (spaces.isEmpty()) throw new IllegalStateException("All spaces are empty?!");

    return new ProjectSpaces(List.copyOf(spaces));
  }

  static Predicate<Path> isModuleOf(String space, String... configuration) {
    if (configuration.length == 0) return path -> false;
    if (configuration.length == 1 && configuration[0].equals("*")) {
      if (space.equals("main")) return path -> true;
      else return path -> ProjectScanner.isModuleInfoJavaFileForCodeSpace(path, space);
    }
    return Stream.of(configuration).map(Path::of).collect(Collectors.toSet())::contains;
  }

  /** Test supplied path for pointing to a Java module declaration for a given space name. */
  static boolean isModuleInfoJavaFileForCodeSpace(Path info, String space) {
    return Collections.frequency(deque(info), space) == 1;
  }

  /** Convert path element names of the given unit into a reversed deque. */
  static Deque<String> deque(Path path) {
    var deque = new ArrayDeque<String>();
    path.forEach(name -> deque.addFirst(name.toString()));
    return deque;
  }

  public static DeclaredModule scanDeclaredModule(Path pathOfModuleInfoJavaOrItsParentDirectory) {
    var path = pathOfModuleInfoJavaOrItsParentDirectory.normalize();
    if (Files.notExists(path)) throw new IllegalArgumentException("Path must exist: " + path);
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    if (Files.notExists(info)) throw new IllegalArgumentException("No module-info in: " + path);
    var descriptor = ModuleDescriptorSupport.parse(info);

    // Single module in project's base directory?
    if (info.toString().equals("module-info.java")) {
      return new DeclaredModule(descriptor, info, Optional.empty(), Folders.of());
    }

    // ...

    var parent = info.getParent();
    var directory = parent != null ? parent : Path.of(".");
    var types = FolderTypes.of(FolderType.SOURCES);
    var folders = Folders.of(new Folder(directory, 0, types));
    return new DeclaredModule(descriptor, info, Optional.empty(), folders);
  }

  public static Folder parseTargetedFolder(Path path, FolderType... types) {
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    var version = parseReleaseNumber(PathSupport.name(path));
    return new Folder(path, version, FolderTypes.of(types));
  }

  static int parseReleaseNumber(String string) {
    if (string == null || string.isEmpty()) return 0;
    var matcher = RELEASE_NUMBER_PATTERN.matcher(string);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }
}
