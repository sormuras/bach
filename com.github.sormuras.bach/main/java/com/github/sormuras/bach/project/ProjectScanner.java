package com.github.sormuras.bach.project;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Configuration;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.internal.PathSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A factory of {@link Project} instances. */
public record ProjectScanner(Bach bach) implements Logbook.Trait {

  private static final Pattern RELEASE_NUMBER_PATTERN = Pattern.compile(".*?(\\d+)$");

  @Override
  public Logbook logbook() {
    return bach.logbook();
  }

  public Project scanProjectInCurrentWorkingDirectory() {
    return scanProject(Path.of(""));
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

    var projectSpaces = parseProjectSpaces(directory);

    return Project.of(projectName, projectVersion).with(projectSpaces);
  }

  public ProjectSpaces parseProjectSpaces(Path directory) {
    var moduleInfoFiles = new ArrayList<>(bach.explorer().findModuleInfoJavaFiles(directory));
    var size = moduleInfoFiles.size();
    log("Found %d module-info.java file%s".formatted(size, size == 1 ? "" : "s"));
    if (size == 0) throw log(new Error("No module-info.java found"));

    var tests = new TreeMap<String, DeclaredModule>();
    var mains = new TreeMap<String, DeclaredModule>();

    var isTestModule = isModuleOf("test", "test");
    var isMainModule = isModuleOf("main", "*");

    var iterator = moduleInfoFiles.listIterator();
    while (iterator.hasNext()) {
      var path = iterator.next();
      if (path.startsWith(".bach")) continue;
      if (isTestModule.test(path)) {
        var declaration = parseDeclaredModule(path);
        tests.put(declaration.name(), declaration);
        iterator.remove();
        continue;
      }
      if (isMainModule.test(path)) {
        var declaration = parseDeclaredModule(path);
        mains.put(declaration.name(), declaration);
        iterator.remove();
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
            Runtime.version().feature(),
            new DeclaredModules(List.copyOf(tests.values())));

    var spaces = new ArrayList<ProjectSpace>();
    if (!mains.isEmpty()) spaces.add(mainSpace);
    if (!tests.isEmpty()) spaces.add(testSpace);
    if (spaces.isEmpty()) throw new IllegalStateException("All spaces are empty?!");

    return new ProjectSpaces(List.copyOf(spaces));
  }

  public DeclaredModule parseDeclaredModule(Path path) {
    // TODO ...
    return DeclaredModule.of(path);
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

  public static TargetedFolder parseTargetedFolder(Path path, FolderType... types) {
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    var version = parseReleaseNumber(PathSupport.name(path));
    return new TargetedFolder(path, version, FolderTypes.of(types));
  }

  static int parseReleaseNumber(String string) {
    if (string == null || string.isEmpty()) return 0;
    var matcher = RELEASE_NUMBER_PATTERN.matcher(string);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }
}
