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
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
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
  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
  public static class Folder {
    public static Folder of(Path path) {
      var release = Convention.javaReleaseFeatureNumber(String.valueOf(path.getFileName()));
      return new Folder(path, release);
    }
    private final Path path;
    private final int release;
    public Folder(Path path, int release) {
      this.path = path;
      this.release = release;
    }
    public Path path() {
      return path;
    }
    public int release() {
      return release;
    }
    @Override
    public String toString() {
      return new StringJoiner(", ", Folder.class.getSimpleName() + "[", "]")
          .add("path=" + path)
          .add("release=" + release)
          .toString();
    }
  }
  public static class Unit {
    public static Unit of(String name, Folder... folders) {
      var descriptor = ModuleDescriptor.newModule(name).build();
      return new Unit(descriptor, List.of(folders));
    }
    private final ModuleDescriptor descriptor;
    private final List<Folder> folders;
    public Unit(ModuleDescriptor descriptor, List<Folder> folders) {
      this.descriptor = descriptor;
      this.folders = folders;
    }
    public ModuleDescriptor descriptor() {
      return descriptor;
    }
    public List<Folder> folders() {
      return folders;
    }
    @Override
    public String toString() {
      return new StringJoiner(", ", Unit.class.getSimpleName() + "[", "]")
          .add("descriptor=" + descriptor)
          .add("folders=" + folders)
          .toString();
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
      if (string.endsWith("-module")) return 9;
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
