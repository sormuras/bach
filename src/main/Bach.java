/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

// default package

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Java build tool main program. */
class Bach {

  /** Version is either {@code master} or {@link Runtime.Version#parse(String)}-compatible. */
  static final String VERSION = "master";

  /** Convenient short-cut to {@code "user.home"} as a path. */
  static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  /** Convenient short-cut to {@code "user.dir"} as a path. */
  static final Path USER_PATH = Path.of(System.getProperty("user.dir"));

  /** Main entry-point running all default actions. */
  public static void main(String... args) {
    if (System.getProperty("java.util.logging.config.file") == null) {
      var format = "java.util.logging.SimpleFormatter.format";
      System.setProperty(format, "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
    }
    var bach = new Bach(List.of(args));
    var code = bach.run();
    if (code != 0) {
      throw new Error("Bach.java failed with error code: " + code);
    }
  }

  final List<String> arguments;
  final Path base;
  final System.Logger logger;
  final Variables var;
  final Project project;
  final Utilities utilities;

  Bach() {
    this(List.of());
  }

  Bach(List<String> arguments) {
    this(System.getLogger("Bach.java"), USER_PATH, arguments);
  }

  Bach(System.Logger logger, Path base, List<String> arguments) {
    this.logger = logger;
    this.base = base;
    this.arguments = List.copyOf(arguments);
    this.var = new Variables();
    this.project = new Project();
    this.utilities = new Utilities();
  }

  Path based(Path path) {
    if (path.isAbsolute()) {
      return path;
    }
    if (base.equals(USER_PATH)) {
      return path;
    }
    return base.resolve(path).normalize();
  }

  Path based(String first, String... more) {
    return based(Path.of(first, more));
  }

  Path based(Property property) {
    return based(Path.of(var.get(property)));
  }

  /** Run default actions. */
  int run() {
    var actions = new ArrayList<Action>();
    // always run default actions
    actions.add(Action.Default.BANNER);
    actions.add(Action.Default.CHECK);
    // append more actions
    var strings = new ArrayDeque<>(arguments);
    while (!strings.isEmpty()) {
      var operation = strings.removeFirst();
      if (operation.equalsIgnoreCase("tool")) {
        if (strings.isEmpty()) {
          logger.log(ERROR, "Missing name of tool to run!");
          return 1;
        }
        var tool = new Action.ToolRunner(new Command(strings.removeFirst()).addAll(strings));
        var.out = System.out::println;
        var.err = System.err::println;
        actions.add(tool);
        strings.clear();
        break;
      }
      try {
        var action = Action.Default.valueOf(operation.toUpperCase());
        actions.add(action);
      } catch (IllegalArgumentException e) {
        logger.log(ERROR, "Unsupported action: " + operation);
        return 1;
      }
    }
    return run(actions);
  }

  /** Run supplied actions. */
  int run(Action... actions) {
    return run(List.of(actions));
  }

  /** Run supplied actions. */
  int run(List<Action> actions) {
    if (actions.isEmpty()) {
      logger.log(WARNING, "No actions to run...");
    }
    for (var action : actions) {
      logger.log(DEBUG, "Running action {0}...", action.name());
      var code = action.run(this);
      if (code != 0) {
        logger.log(ERROR, "Action {0} failed with error code: {1}", action.name(), code);
        return code;
      }
      logger.log(DEBUG, "Action {0} succeeded.", action.name());
    }
    return 0;
  }

  /** Constants with default values. */
  enum Property {
    /** Default Maven repository used for artifact resolution. */
    MAVEN_REPOSITORY("https://repo1.maven.org/maven2"),

    /** Offline mode. */
    OFFLINE("false"),

    /** Cache of binary tools. */
    PATH_CACHE_TOOLS(".bach/tools"),

    /** Cache of resolved modules. */
    PATH_CACHE_MODULES(".bach/modules"),

    /** Name of the project. */
    PROJECT_NAME("project"),

    /** Version of the project. */
    PROJECT_VERSION("1.0.0-SNAPSHOT"),

    /** URI to Google Java Format "all-deps" JAR. */
    TOOL_FORMAT_URI(
        "https://github.com/google/google-java-format/releases/download/google-java-format-1.7/google-java-format-1.7-all-deps.jar"),

    /** URI to JUnit Platform Console Standalone JAR. */
    TOOL_JUNIT_URI(
        "http://central.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.4.0/junit-platform-console-standalone-1.4.0.jar"),

    /** Maven URI. */
    TOOL_MAVEN_URI(
        "https://archive.apache.org/dist/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.zip");

    final String key;
    final String defaultValue;

    Property(String defaultValue) {
      this.key = "bach." + name().toLowerCase().replace('_', '.');
      this.defaultValue = defaultValue;
    }
  }

  /** Bach's project object model. */
  class Project {
    final Path target;
    final String name, version;
    final Path modules;
    final Realm main, test;

    Project() {
      this.target = based("bin");
      var defaultName =
          Optional.ofNullable(base.getFileName())
              .map(Object::toString)
              .orElse(Property.PROJECT_NAME.defaultValue);
      this.name = var.get(Property.PROJECT_NAME.key, defaultName);
      this.version = var.get(Property.PROJECT_VERSION);
      this.modules = based(Property.PATH_CACHE_MODULES);
      this.main = new Realm("main", List.of("src/main/java", "src/main", "main", "src"));
      this.test = new Realm("test", List.of("src/test/java", "src/test", "test"));
    }

    int build() {
      try {
        format();
        assemble();
        main.compile();
        test.compile();
        test();
      } catch (Exception e) {
        logger.log(ERROR, "Building project failed: " + e.getMessage(), e);
        return 1;
      }
      return 0;
    }

    void clean() throws Exception {
      utilities.treeDelete(target);
    }

    void format() {
      var roots = Set.of(main.source, test.source);
      var count = new Command("count **/*.java").addAllJavaFiles(roots);
      if (count.arguments.isEmpty()) {
        logger.log(INFO, "No Java source files present.");
        return;
      }
      var code = run(new Bach.Tool.GoogleJavaFormat(false, roots));
      if (code != 0) {
        throw new IllegalStateException("Format violation(s) detected!");
      }
    }

    void assemble() throws Exception {
      //      var.get("bach.project.modules.uris", "", ",")
      //              .map(URI::create)
      //              .peek(uri -> log.debug("Loading %s", uri))
      //              .forEach(uri -> new Tool.Download(uri, modules).apply(Bach.this));
      var roots =
          Set.of(main.source, test.source).stream()
              .filter(Files::isDirectory)
              .collect(Collectors.toSet());
      var externals = ModuleInfo.findExternalModuleNames(roots);
      logger.log(DEBUG, "External module names: {0}", externals);
      if (externals.isEmpty()) {
        return;
      }
      var moduleMaven =
          var.load(
              utilities.download(
                  modules,
                  URI.create(
                      "https://raw.githubusercontent.com/jodastephen/jpms-module-names/master/generated/module-maven.properties")));
      var moduleVersion =
          var.load(
              utilities.download(
                  modules,
                  URI.create(
                      "https://raw.githubusercontent.com/jodastephen/jpms-module-names/master/generated/module-version.properties")));
      var uris = new ArrayList<URI>();
      for (var external : externals) {
        var mavenGA = moduleMaven.getProperty(external);
        if (mavenGA == null) {
          logger.log(WARNING, "External module not mapped: {0}", external);
          continue;
        }
        var group = mavenGA.substring(0, mavenGA.indexOf(':'));
        var artifact = mavenGA.substring(group.length() + 1);
        var version = moduleVersion.getProperty(external);
        uris.add(maven(group, artifact, version));
      }
      var paths = utilities.download(modules, uris);
      for (var path : paths) {
        logger.log(DEBUG, "Resolved {0}", path);
      }
    }

    /** Create URI for supplied Maven coordinates. */
    URI maven(String group, String artifact, String version) {
      // TODO Try local repository first?
      // Path.of(System.getProperty("user.home"), ".m2", "repository").toUri().toString();
      var repo = var.get(Property.MAVEN_REPOSITORY);
      var file = artifact + "-" + version + ".jar";
      return URI.create(String.join("/", repo, group.replace('.', '/'), artifact, version, file));
    }

    /** Return list of child directories directly present in {@code root} path. */
    List<Path> findDirectories(Path root) {
      if (Files.notExists(root)) {
        return List.of();
      }
      try (var paths = Files.find(root, 1, (path, attr) -> Files.isDirectory(path))) {
        return paths.filter(path -> !root.equals(path)).collect(Collectors.toList());
      } catch (Exception e) {
        throw new Error("findDirectories failed for root: " + root, e);
      }
    }

    /** Return list of child directory names directly present in {@code root} path. */
    List<String> findDirectoryNames(Path root) {
      return findDirectories(root).stream()
          .map(root::relativize)
          .map(Path::toString)
          .collect(Collectors.toList());
    }

    /** Return patch map using two collections of paths. */
    Map<String, Set<Path>> findPatchMap(Collection<Path> bases, Collection<Path> patches) {
      var map = new TreeMap<String, Set<Path>>();
      for (var base : bases) {
        for (var name : findDirectoryNames(base)) {
          for (var patch : patches) {
            var candidate = patch.resolve(name);
            if (Files.isDirectory(candidate)) {
              map.computeIfAbsent(name, __ -> new TreeSet<>()).add(candidate);
            }
          }
        }
      }
      return map;
    }

    int test() throws Exception {
      if (Files.notExists(test.target)) {
        logger.log(INFO, "No test realm target available, no tests.");
        return 0;
      }
      logger.log(INFO, "Launching JUnit Platform...");
      var java = new Command("java");
      java.add("--module-path").add(List.of(test.target, main.target, modules));

      // java.add("--add-modules").add("ALL-MODULE-PATH,ALL-DEFAULT");
      java.add("--add-modules").add(String.join(",", findDirectoryNames(test.target)));

      // java.add("--module").add("org.junit.platform.console");
      java.add("--class-path").add(Tool.JUnit.install(Bach.this));
      java.add("org.junit.platform.console.ConsoleLauncher");

      java.add("--reports-dir");
      java.add(target.resolve("test-reports"));

      java.add("--scan-modules");
      return java.run(Bach.this);
    }

    /** Building block, source set, scope, directory, named context: {@code main}, {@code test}. */
    class Realm {
      final String name;
      final Path source;
      final Path target;

      Realm(String name, List<String> sources) {
        this.name = name;
        this.source =
            sources.stream()
                .map(Bach.this::based)
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(based(sources.get(0)));
        this.target = Project.this.target.resolve(Path.of("compiled", name));
      }

      void compile() {
        logger.log(DEBUG, "Compiling realm: " + name);
        if (Files.notExists(source)) {
          logger.log(INFO, "Skip compile for {0}! No source path exists: {1}", name, source);
          return;
        }
        var javac = new Command("javac");
        javac.add("-d").add(target);
        javac.add("--module-path").add(modules);
        javac.add("--module-source-path").add(source);
        // get("bach.project.realms[" + name + "].compile.options", "", ",").forEach(javac::add);

        for (var entry : findPatchMap(List.of(test.source), List.of(main.source)).entrySet()) {
          var module = entry.getKey();
          var paths = entry.getValue();
          if (paths.isEmpty()) {
            throw new Error("expected at least one patch path entry for " + module);
          }
          var patches =
              paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
          javac.add("--patch-module");
          javac.add(module + "=" + patches);
        }

        // javac.mark(99);
        javac.addAllJavaFiles(Set.of(source));
        if (javac.run(Bach.this) != 0) {
          throw new RuntimeException(name + ".compile() failed!");
        }
      }
    }
  }

  /** Variable state holder. */
  class Variables {

    /** Managed properties loaded from {@code ${base}/bach.properties} file. */
    final Properties properties = load(base.resolve("bach.properties"));

    /** Offline mode. */
    boolean offline = Boolean.parseBoolean(get(Property.OFFLINE));

    /** Standard output line consumer. */
    Consumer<String> out = line -> logger.log(DEBUG, line);

    /** Error output line consumer. */
    Consumer<String> err = line -> logger.log(ERROR, line);

    /** Get value for the supplied property, using its key and default value. */
    String get(Property property) {
      return get(property.key, property.defaultValue);
    }

    /** Get value for the supplied property key. */
    String get(String key, String defaultValue) {
      var value = System.getProperty(key);
      if (value != null) {
        return value;
      }
      return properties.getProperty(key, defaultValue);
    }

    /** Load from properties from path. */
    Properties load(Path path) {
      var properties = new Properties();
      if (Files.exists(path)) {
        try (var stream = Files.newInputStream(path)) {
          properties.load(stream);
        } catch (Exception e) {
          throw new Error("Loading properties failed: " + path, e);
        }
      }
      return properties;
    }
  }

  /** Utilities. */
  class Utilities {

    /** Unzip. */
    Path extract(Path zip) throws Exception {
      var jar = ToolProvider.findFirst("jar").orElseThrow();
      var listing = new StringWriter();
      var printWriter = new PrintWriter(listing);
      jar.run(printWriter, printWriter, "--list", "--file", zip.toString());
      // TODO Find better way to extract root folder name...
      var root = Path.of(listing.toString().split("\\R")[0]);
      var destination = zip.getParent();
      var home = destination.resolve(root);
      if (Files.notExists(home)) {
        if (base.equals(USER_PATH)) {
          jar.run(System.out, System.err, "--extract", "--file", zip.toString());
        } else {
          new ProcessBuilder("jar", "--extract", "--file", zip.toString())
              .directory(destination.toFile())
              .start()
              .waitFor();
        }
      }
      return home.normalize().toAbsolutePath();
    }

    /** Extract path last element from the supplied uri. */
    String fileName(URI uri) {
      var urlString = uri.getPath();
      var begin = urlString.lastIndexOf('/') + 1;
      return urlString.substring(begin).split("\\?")[0].split("#")[0];
    }

    /** Download files from supplied uris to specified destination directory. */
    List<Path> download(Path destination, List<URI> uris) {
      logger.log(DEBUG, "Downloading {0} file(s) to {1}...", uris.size(), destination);
      try {
        var paths = new ArrayList<Path>();
        for (var uri : uris) {
          paths.add(download(destination, uri));
        }
        return paths;
      } catch (Exception e) {
        throw new Error("Download failed: " + e.getMessage(), e);
      }
    }

    /** Download file from supplied uri to specified destination directory. */
    Path download(Path destination, URI uri) throws Exception {
      logger.log(DEBUG, "Downloading {0}...", uri);
      var fileName = fileName(uri);
      var target = destination.resolve(fileName);
      if (var.offline) {
        if (Files.exists(target)) {
          logger.log(DEBUG, "Offline mode is active and target already exists.");
          return target;
        }
        throw new IllegalStateException("Target is missing and being offline: " + target);
      }
      Files.createDirectories(destination);
      var connection = uri.toURL().openConnection();
      try (var sourceStream = connection.getInputStream()) {
        var urlLastModifiedMillis = connection.getLastModified();
        var urlLastModifiedTime = FileTime.fromMillis(urlLastModifiedMillis);
        if (Files.exists(target)) {
          logger.log(DEBUG, "Local file exists. Comparing attributes to remote file...");
          var unknownTime = urlLastModifiedMillis == 0L;
          if (Files.getLastModifiedTime(target).equals(urlLastModifiedTime) || unknownTime) {
            var localFileSize = Files.size(target);
            var contentLength = connection.getContentLengthLong();
            if (localFileSize == contentLength) {
              logger.log(DEBUG, "Local and remote file attributes seem to match.");
              return target;
            }
          }
          logger.log(DEBUG, "Local file differs from remote -- replacing it...");
        }
        logger.log(DEBUG, "Transferring {0}...", uri);
        try (var targetStream = Files.newOutputStream(target)) {
          sourceStream.transferTo(targetStream);
        }
        if (urlLastModifiedMillis != 0L) {
          Files.setLastModifiedTime(target, urlLastModifiedTime);
        }
        logger.log(INFO, "Downloaded {0} successfully.", fileName);
        logger.log(DEBUG, " o Size -> {0} bytes", Files.size(target));
        logger.log(DEBUG, " o Last Modified -> {0}", urlLastModifiedTime);
      }
      return target;
    }

    /** Copy all files and directories from source to target directory. */
    void treeCopy(Path source, Path target) throws Exception {
      treeCopy(source, target, __ -> true);
    }

    /** Copy selected files and directories from source to target directory. */
    void treeCopy(Path source, Path target, Predicate<Path> filter) throws Exception {
      // debug("treeCopy(source:`%s`, target:`%s`)%n", source, target);
      if (!Files.exists(source)) {
        throw new IllegalArgumentException("source must exist: " + source);
      }
      if (!Files.isDirectory(source)) {
        throw new IllegalArgumentException("source must be a directory: " + source);
      }
      if (Files.exists(target)) {
        if (!Files.isDirectory(target)) {
          throw new IllegalArgumentException("target must be a directory: " + target);
        }
        if (target.equals(source)) {
          return;
        }
        if (target.startsWith(source)) {
          // copy "a/" to "a/b/"...
          throw new IllegalArgumentException("target must not a child of source");
        }
      }
      try (var stream = Files.walk(source).sorted()) {
        int counter = 0;
        var paths = stream.collect(Collectors.toList());
        for (var path : paths) {
          var destination = target.resolve(source.relativize(path));
          if (Files.isDirectory(path)) {
            Files.createDirectories(destination);
            continue;
          }
          if (filter.test(path)) {
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            counter++;
          }
        }
        logger.log(DEBUG, "Copied {0} file(s) of {1} elements.", counter, paths.size());
      }
    }

    /** Delete all files and directories from the root directory. */
    void treeDelete(Path root) throws Exception {
      treeDelete(root, __ -> true);
    }

    /** Delete selected files and directories from the root directory. */
    void treeDelete(Path root, Predicate<Path> filter) throws Exception {
      // trivial case: delete existing empty directory or single file
      try {
        Files.deleteIfExists(root);
        return;
      } catch (DirectoryNotEmptyException ignored) {
        // fall-through
      }
      // default case: walk the tree...
      try (var stream = Files.walk(root)) {
        var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (var path : selected.collect(Collectors.toList())) {
          Files.deleteIfExists(path);
        }
      }
    }

    /** Walk directory tree structure. */
    List<String> treeWalk(Path root) {
      var lines = new ArrayList<String>();
      treeWalk(root, lines::add);
      return lines;
    }

    /** Walk directory tree structure. */
    void treeWalk(Path root, Consumer<String> out) {
      if (Files.exists(root)) {
        out.accept(root.toString());
      }
      try (var stream = Files.walk(root)) {
        stream
            .map(root::relativize)
            .map(path -> path.toString().replace('\\', '/'))
            .sorted()
            .forEach(string -> out.accept("." + (string.isEmpty() ? "" : "/") + string));
      } catch (Exception e) {
        throw new Error("Walking tree failed: " + root, e);
      }
    }
  }

  /** Action running on Bach instances. */
  @FunctionalInterface
  interface Action {
    /** Human-readable name of this action. */
    default String name() {
      return getClass().getSimpleName();
    }

    /** Run this action and return zero on success. */
    int run(Bach bach);

    /** Visible in help's output by default. */
    default boolean visible() {
      return true;
    }

    /** No-arg default action. */
    enum Default implements Action {
      BANNER("Display a fancy ASCII art banner") {
        @Override
        public int run(Bach bach) {
          bach.logger.log(
              INFO,
              "\n"
                  + "    ___      ___      ___      ___   \n"
                  + "   /\\  \\    /\\  \\    /\\  \\    /\\__\\\n"
                  + "  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_\n"
                  + " /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\\n"
                  + " \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /\n"
                  + "  \\::/  /   /:/  /  \\:\\__\\    /:/  /\n"
                  + "   \\/__/    \\/__/    \\/__/    \\/__/"
                  + " "
                  + VERSION);
          return 0;
        }

        @Override
        public boolean visible() {
          return false;
        }
      },

      BUILD("Build project in base directory.") {
        @Override
        public int run(Bach bach) {
          return bach.project.build();
        }
      },

      CHECK("Check preconditions.") {
        @Override
        public int run(Bach bach) {
          // First, log all settings for debugging.
          bach.logger.log(DEBUG, "Main");
          bach.logger.log(DEBUG, "  base = {0}", bach.base);
          bach.logger.log(DEBUG, "  logger = {0}", bach.logger.getName());
          bach.logger.log(DEBUG, "Arguments");
          if (bach.arguments.isEmpty()) {
            bach.logger.log(DEBUG, "  <none>");
          } else {
            var index = 0;
            for (var argument : bach.arguments) {
              bach.logger.log(DEBUG, "  [{0}] = {1}", index++, argument);
            }
          }
          bach.logger.log(DEBUG, "Variables");
          bach.logger.log(DEBUG, "  offline = {0}", bach.var.offline);
          bach.logger.log(DEBUG, "  out = {0}", bach.var.out);
          bach.logger.log(DEBUG, "  err = {0}", bach.var.err);
          bach.logger.log(DEBUG, "  properties = {0}", bach.var.properties);
          bach.logger.log(DEBUG, "Properties");
          for (var property : Property.values()) {
            bach.logger.log(DEBUG, "  {0} = {1}", property.key, bach.var.get(property));
          }
          // Now perform checks.
          if (bach.base.getNameCount() == 0) {
            bach.logger.log(ERROR, "Base path has zero elements!");
            return 1;
          }
          return 0;
        }

        @Override
        public boolean visible() {
          return false;
        }
      },

      CLEAN("Delete all generated assets - but keep caches intact.") {
        @Override
        public int run(Bach bach) {
          try {
            bach.project.clean();
          } catch (Exception e) {
            bach.logger.log(ERROR, "Deleting cache directory failed!", e);
            return 1;
          }
          return 0;
        }
      },

      ERASE("Delete all generated assets - and also delete caches.") {
        @Override
        public int run(Bach bach) {
          try {
            CLEAN.run(bach);
            bach.utilities.treeDelete(bach.based(".bach"));
            // TODO bach.utilities.treeDelete(USER_HOME.resolve(".bach"));
          } catch (Exception e) {
            bach.logger.log(ERROR, "Deleting cache directories failed!", e);
            return 1;
          }
          return 0;
        }
      },

      HELP("Print this help screen on standard out... F1, F1, F1!") {
        @Override
        public int run(Bach bach) {
          System.out.println();
          for (var action : Action.Default.values()) {
            if (!action.visible()) {
              continue;
            }
            var name = action.name().toLowerCase();
            System.out.println(String.format(" %-9s -> %s", name, action.description));
          }
          System.out.println();
          return 0;
        }
      },

      SCAFFOLD("Create modular Java sample project in base directory.") {
        @Override
        public int run(Bach bach) {
          // TODO Check for effectively empty base directory.
          try {
            var uri = "https://github.com/sormuras/bach/raw/" + VERSION + "/demo/scaffold.zip";
            var zip = bach.utilities.download(bach.base, URI.create(uri));
            bach.utilities.extract(zip);
            Files.delete(zip);
          } catch (Exception e) {
            bach.logger.log(ERROR, "Scaffolding project failed!", e);
            return 1;
          }
          return 0;
        }
      },

      TOOL("Execute named tool consuming all remaining arguments.") {
        @Override
        public int run(Bach bach) {
          throw new UnsupportedOperationException("Action.ToolRunner should have handled this!");
        }
      };

      final String description;

      Default(String... description) {
        this.description = String.join("", description);
      }
    }

    /** Tool runner action. */
    class ToolRunner implements Action {

      class Gobbler extends StringWriter implements Runnable {

        final Consumer<String> consumer;
        final InputStream stream;

        Gobbler(Consumer<String> consumer) {
          this(consumer, InputStream.nullInputStream());
        }

        Gobbler(Consumer<String> consumer, InputStream stream) {
          this.consumer = consumer;
          this.stream = stream;
        }

        @Override
        public void flush() {
          toString().lines().forEach(consumer);
          getBuffer().setLength(0);
        }

        @Override
        public void run() {
          new BufferedReader(new InputStreamReader(stream)).lines().forEach(consumer);
        }
      }

      final String name;
      final List<String> args;

      ToolRunner(Command command) {
        this.name = command.name;
        this.args = command.arguments;
      }

      ToolRunner(String name, String... args) {
        this.name = name;
        this.args = List.of(args);
      }

      @Override
      public int run(Bach bach) {
        bach.logger.log(INFO, "Running tool: {0} {1}", name, String.join(" ", args));
        // ToolProvider SPI
        var provider = ToolProvider.findFirst(name);
        if (provider.isPresent()) {
          var out = new PrintWriter(new Gobbler(bach.var.out), true);
          var err = new PrintWriter(new Gobbler(bach.var.err), true);
          return provider.get().run(out, err, args.toArray(String[]::new));
        }
        // Delegate to process builder.
        // TODO Find foundation tool executable in "${JDK}/bin" folder.
        var command = new ArrayList<String>();
        try {
          switch (name) {
            case "format":
              var format = new Bach.Tool.GoogleJavaFormat(args).toCommand(bach);
              command.add(format.name);
              command.addAll(format.arguments);
              break;
            case "junit":
              var junit = new Bach.Tool.JUnit(args).toCommand(bach);
              command.add(junit.name);
              command.addAll(junit.arguments);
              break;
            case "maven":
            case "mvn":
              var maven = new Bach.Tool.Maven(args).toCommand(bach);
              command.add(maven.name);
              command.addAll(maven.arguments);
              break;
            default:
              command.add(name);
              command.addAll(args);
          }
        } catch (Exception e) {
          bach.logger.log(ERROR, "Creating command for tool failed: " + e.getMessage(), e);
          return 1;
        }
        var executor = Executors.newFixedThreadPool(2);
        try {
          var process = new ProcessBuilder(command).start();
          executor.submit(new Gobbler(bach.var.out, process.getInputStream()));
          executor.submit(new Gobbler(bach.var.err, process.getErrorStream()));
          return process.waitFor();
        } catch (Exception e) {
          bach.logger.log(ERROR, "Running tool failed: " + e.getMessage(), e);
          return 1;
        } finally {
          executor.shutdownNow();
        }
      }
    }
  }

  /** External program. */
  interface Tool extends Action {

    Command toCommand(Bach bach) throws Exception;

    static Path tools(Bach bach) {
      // return bach.based(Property.PATH_CACHE_TOOLS); // each project got its own tool
      return USER_HOME.resolve(bach.var.get(Property.PATH_CACHE_TOOLS)); // share tools
    }

    @Override
    default int run(Bach bach) {
      try {
        return toCommand(bach).run(bach);
      } catch (Exception e) {
        bach.logger.log(ERROR, "Running failed: " + e.getMessage(), e);
        return 1;
      }
    }

    class GoogleJavaFormat implements Tool {

      static Path install(Bach bach) throws Exception {
        var name = "google-java-format";
        var destination = tools(bach).resolve(name);
        var uri = URI.create(bach.var.get(Property.TOOL_FORMAT_URI));
        return bach.utilities.download(destination, uri);
      }

      final List<?> arguments;
      final Collection<Path> roots;

      GoogleJavaFormat(List<?> arguments) {
        this.arguments = arguments;
        this.roots = Set.of();
      }

      GoogleJavaFormat(boolean replace, Collection<Path> roots) {
        this.arguments =
            replace ? List.of("--replace") : List.of("--dry-run", "--set-exit-if-changed");
        this.roots = roots;
      }

      @Override
      public Command toCommand(Bach bach) throws Exception {
        var jar = install(bach);
        var command = new Command("java");
        command.add("-jar");
        command.add(jar);
        command.addAll(arguments);
        // command.mark(10);
        command.addAllJavaFiles(roots);
        return command;
      }
    }

    class JUnit implements Tool {

      static Path install(Bach bach) throws Exception {
        var art = "junit-platform-console-standalone";
        var dir = tools(bach).resolve(art);
        var uri = URI.create(bach.var.get(Property.TOOL_JUNIT_URI));
        return bach.utilities.download(dir, uri);
      }

      final List<?> arguments;

      JUnit(List<?> arguments) {
        this.arguments = arguments;
      }

      @Override
      public Command toCommand(Bach bach) throws Exception {
        var junit = install(bach);
        var command = new Command("java");
        command.add("-ea");
        command.add("-jar").add(junit);
        command.addAll(arguments);
        return command;
      }
    }

    /** Maven. */
    class Maven implements Tool {

      final List<?> arguments;

      Maven(List<?> arguments) {
        this.arguments = arguments;
      }

      @Override
      public Command toCommand(Bach bach) throws Exception {
        var uri = URI.create(bach.var.get(Property.TOOL_MAVEN_URI));
        var zip = bach.utilities.download(tools(bach), uri);
        var home = bach.utilities.extract(zip);
        var win = System.getProperty("os.name").toLowerCase().contains("win");
        var name = "mvn" + (win ? ".cmd" : "");
        var executable = home.resolve("bin").resolve(name);
        executable.toFile().setExecutable(true);
        return new Command(executable.toString()).addAll(arguments);
      }
    }
  }

  /** Command line builder. */
  static class Command {

    /** Test supplied path for pointing to a Java source unit file. */
    static boolean isJavaFile(Path path) {
      if (Files.isRegularFile(path)) {
        var name = path.getFileName().toString();
        if (name.endsWith(".java")) {
          return name.indexOf('.') == name.length() - 5; // single dot in filename
        }
      }
      return false;
    }

    final String name;
    final List<String> arguments;

    Command(String name) {
      this.name = name;
      this.arguments = new ArrayList<>();
    }

    /** Add single non-null argument. */
    Command add(Object argument) {
      arguments.add(argument.toString());
      return this;
    }

    /** Add single argument composed of joined path names using {@link File#pathSeparator}. */
    Command add(Collection<Path> paths) {
      return add(paths.stream(), File.pathSeparator);
    }

    /** Add single argument composed of all stream elements joined by specified separator. */
    Command add(Stream<?> stream, String separator) {
      return add(stream.map(Object::toString).collect(Collectors.joining(separator)));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addAll(Object... arguments) {
      for (var argument : arguments) {
        add(argument);
      }
      return this;
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addAll(Iterable<?> arguments) {
      arguments.forEach(this::add);
      return this;
    }

    /** Add all files visited by walking specified root path recursively. */
    Command addAll(Path root, Predicate<Path> predicate) {
      try (var stream = Files.walk(root).filter(predicate)) {
        stream.forEach(this::add);
      } catch (Exception e) {
        throw new Error("walking path `" + root + "` failed", e);
      }
      return this;
    }

    /** Add all files visited by walking specified root paths recursively. */
    Command addAll(Collection<Path> roots, Predicate<Path> predicate) {
      for (var root : roots) {
        if (Files.notExists(root)) {
          continue;
        }
        addAll(root, predicate);
      }
      return this;
    }

    /** Add all {@code .java} source files by walking specified root paths recursively. */
    Command addAllJavaFiles(Collection<Path> roots) {
      return addAll(roots, Command::isJavaFile);
    }

    /** Dump command executables and arguments using the provided string consumer. */
    Command dump(Consumer<String> consumer) {
      var iterator = arguments.listIterator();
      consumer.accept(name);
      while (iterator.hasNext()) {
        var argument = iterator.next();
        var indent = argument.startsWith("-") ? "" : "  ";
        consumer.accept(indent + argument);
      }
      return this;
    }

    int run(Bach bach) {
      return bach.run(new Action.ToolRunner(this));
    }
  }

  /** Simple module information collector. */
  static class ModuleInfo {

    private static final Pattern NAME = Pattern.compile("(module)\\s+(.+)\\s*\\{.*");

    private static final Pattern REQUIRES = Pattern.compile("requires (.+?);", Pattern.DOTALL);

    static ModuleInfo of(Path path) {
      if (Files.isDirectory(path)) {
        path = path.resolve("module-info.java");
      }
      try {
        return of(Files.readString(path));
      } catch (Exception e) {
        throw new RuntimeException("reading '" + path + "' failed", e);
      }
    }

    static ModuleInfo of(String source) {
      // extract module name
      var nameMatcher = NAME.matcher(source);
      if (!nameMatcher.find()) {
        throw new IllegalArgumentException(
            "expected java module descriptor unit, but got: " + source);
      }
      var name = nameMatcher.group(2).trim();

      // extract required module names
      var requiresMatcher = REQUIRES.matcher(source);
      var requires = new TreeSet<String>();
      while (requiresMatcher.find()) {
        var split = requiresMatcher.group(1).trim().split("\\s+");
        requires.add(split[split.length - 1]);
      }
      return new ModuleInfo(name, requires);
    }

    /** Enumerate all system module names. */
    static Set<String> findSystemModuleNames() {
      return ModuleFinder.ofSystem().findAll().stream()
          .map(reference -> reference.descriptor().name())
          .collect(Collectors.toSet());
    }

    /** Calculate external module names. */
    static Set<String> findExternalModuleNames(Set<Path> roots) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeSet<String>();
      var paths = new ArrayList<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root)) {
          stream.filter(path -> path.endsWith("module-info.java")).forEach(paths::add);
        } catch (Exception e) {
          throw new RuntimeException("walking path failed for: " + root, e);
        }
      }
      for (var path : paths) {
        var info = ModuleInfo.of(path);
        declaredModules.add(info.name);
        requiredModules.addAll(info.requires);
      }
      var externalModules = new TreeSet<>(requiredModules);
      externalModules.removeAll(declaredModules);
      externalModules.removeAll(findSystemModuleNames()); // "java.base", "java.logging", ...
      return externalModules;
    }

    final String name;
    final Set<String> requires;

    private ModuleInfo(String name, Set<String> requires) {
      this.name = name;
      this.requires = Set.copyOf(requires);
    }
  }
}
