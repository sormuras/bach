/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class Bach {
  public static final Version VERSION = Version.parse("11.0-ea");
  public static void main(String... args) {
    Main.main(args);
  }
  private final Consumer<String> printer;
  private final boolean debug;
  private final boolean dryRun;
  public Bach() {
    this(
        System.out::println,
        Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug")),
        Boolean.getBoolean("ry-run") || "".equals(System.getProperty("ry-run")));
  }
  public Bach(Consumer<String> printer, boolean debug, boolean dryRun) {
    this.printer = Objects.requireNonNull(printer, "printer");
    this.debug = debug;
    this.dryRun = dryRun;
    print(Level.TRACE, "Bach initialized");
  }
  public String print(String format, Object... args) {
    return print(Level.INFO, format, args);
  }
  public String print(Level level, String format, Object... args) {
    var message = String.format(format, args);
    if (debug || level.getSeverity() >= Level.INFO.getSeverity()) printer.accept(message);
    return message;
  }
  public String toString() {
    return "Bach.java " + VERSION;
  }
  public static class Directory {
    public static Directory of(Path path) {
      var release = Convention.javaReleaseFeatureNumber(String.valueOf(path.getFileName()));
      return new Directory(path, release);
    }
    public static List<Directory> listOf(Path root) {
      if (Files.notExists(root)) return List.of();
      var directories = new ArrayList<Directory>();
      try (var stream = Files.newDirectoryStream(root, Files::isDirectory)) {
        stream.forEach(path -> directories.add(of(path)));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      directories.sort(Comparator.comparingInt(Directory::release));
      return List.copyOf(directories);
    }
    private final Path path;
    private final int release;
    public Directory(Path path, int release) {
      this.path = path;
      this.release = release;
    }
    public Path path() {
      return path;
    }
    public int release() {
      return release;
    }
    public String toString() {
      return new StringJoiner(", ", Directory.class.getSimpleName() + "[", "]")
          .add("path=" + path)
          .add("release=" + release)
          .toString();
    }
  }
  public static class ModuleCollection {
    private final String name;
    private final int release;
    private final boolean preview;
    private final List<ModuleDescription> modules;
    public ModuleCollection(String name, int release, boolean preview, List<ModuleDescription> modules) {
      this.name = name;
      this.release = release;
      this.preview = preview;
      this.modules = modules;
    }
    public String name() {
      return name;
    }
    public int release() {
      return release;
    }
    public boolean preview() {
      return preview;
    }
    public List<ModuleDescription> modules() {
      return modules;
    }
    public String toString() {
      return new StringJoiner(", ", ModuleCollection.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("release=" + release)
          .add("preview=" + preview)
          .add("modules=" + modules)
          .toString();
    }
  }
  public static class ModuleDescription {
    public static ModuleDescription of(String name, Directory... directories) {
      var descriptor = ModuleDescriptor.newModule(name).build();
      return new ModuleDescription(descriptor, List.of(directories));
    }
    private final ModuleDescriptor descriptor;
    private final List<Directory> directories;
    public ModuleDescription(ModuleDescriptor descriptor, List<Directory> directories) {
      this.descriptor = descriptor;
      this.directories = directories;
    }
    public ModuleDescriptor descriptor() {
      return descriptor;
    }
    public List<Directory> directories() {
      return directories;
    }
    public String toString() {
      return new StringJoiner(", ", ModuleDescription.class.getSimpleName() + "[", "]")
          .add("descriptor=" + descriptor)
          .add("directories=" + directories)
          .toString();
    }
    public List<String> toRequiresNames() {
      var names = descriptor.requires().stream().map(ModuleDescriptor.Requires::name);
      return names.sorted().collect(Collectors.toList());
    }
  }
  public static class Project {
    private final String name;
    private final Version version;
    private final Structure structure;
    public Project(String name, Version version, Structure structure) {
      this.name = name;
      this.version = version;
      this.structure = structure;
    }
    public String name() {
      return name;
    }
    public Version version() {
      return version;
    }
    public Structure structure() {
      return structure;
    }
    public String toString() {
      return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
          .add("name='" + name + "'")
          .add("version=" + version)
          .add("structure=" + structure)
          .toString();
    }
    public String toNameAndVersion() {
      return name + ' ' + version;
    }
    public List<String> toStrings() {
      var strings = new ArrayList<String>();
      strings.add("Project " + toNameAndVersion());
      strings.add("\tModule Collections: " + structure.toCollectionNames());
      for (var collection : structure.collections()) {
        strings.add("\t\tModule Collection \"" + collection.name() + '"');
        strings.add("\t\t\trelease=" + collection.release());
        strings.add("\t\t\tpreview=" + collection.preview());
        strings.add("\t\t\tModule Descriptions: [" + collection.modules().size() + ']');
        for (var module : collection.modules()) {
          strings.add("\t\t\t\tModule " + module.descriptor().toNameAndVersion());
          strings.add("\t\t\t\t\tmainClass=" + module.descriptor().mainClass().orElse("<empty>"));
          strings.add("\t\t\t\t\trequires=" + module.toRequiresNames());
          strings.add("\t\t\t\t\tDirectories: [" + module.directories().size() + ']');
          for (var directory : module.directories()) {
            strings.add("\t\t\t\t\t\tpath=" + directory.path());
            strings.add("\t\t\t\t\t\trelease=" + directory.release());
          }
        }
      }
      return List.copyOf(strings);
    }
  }
  public static class Structure {
    private final List<ModuleCollection> collections;
    public Structure(List<ModuleCollection> collections) {
      this.collections = collections;
    }
    public List<ModuleCollection> collections() {
      return collections;
    }
    public String toString() {
      return new StringJoiner(", ", Structure.class.getSimpleName() + "[", "]")
          .add("collections=" + collections)
          .toString();
    }
    public List<String> toCollectionNames() {
      return collections.stream().map(ModuleCollection::name).collect(Collectors.toList());
    }
  }
  public interface Convention {
    static Optional<String> mainClass(Path info, String module) {
      var main = Path.of(module.replace('.', '/'), "Main.java");
      var exists = Files.isRegularFile(info.resolveSibling(main));
      return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
    }
    static Optional<String> mainModule(Stream<ModuleDescriptor> descriptors) {
      var mains = descriptors.filter(d -> d.mainClass().isPresent()).collect(Collectors.toList());
      return mains.size() == 1 ? Optional.of(mains.get(0).name()) : Optional.empty();
    }
    static int javaReleaseFeatureNumber(String string) {
      if (string.endsWith("-module")) return 0;
      if (string.endsWith("-preview")) return Runtime.version().feature();
      if (string.startsWith("java-")) return Integer.parseInt(string.substring(5));
      return 0;
    }
    static IntSummaryStatistics javaReleaseStatistics(Stream<Path> paths) {
      var names = paths.map(Path::getFileName).map(Path::toString);
      return names.collect(Collectors.summarizingInt(Convention::javaReleaseFeatureNumber));
    }
    static void amendJUnitTestEngines(Set<String> modules) {
      if (modules.contains("org.junit.jupiter") || modules.contains("org.junit.jupiter.api"))
        modules.add("org.junit.jupiter.engine");
      if (modules.contains("junit")) {
        modules.add("org.hamcrest");
        modules.add("org.junit.vintage.engine");
      }
    }
    static void amendJUnitPlatformConsole(Set<String> modules) {
      if (modules.contains("org.junit.platform.console")) return;
      var triggers =
          Set.of("org.junit.jupiter.engine", "org.junit.vintage.engine", "org.junit.platform.engine");
      modules.stream()
          .filter(triggers::contains)
          .findAny()
          .ifPresent(__ -> modules.add("org.junit.platform.console"));
    }
  }
  static class Main {
    public static void main(String... args) {
      System.out.println("Bach.java " + Bach.VERSION);
    }
  }
}
