/*
 * Bach - Java Shell Builder
 * Copyright (C) 2018 Christian Stein
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

import static java.lang.System.Logger.Level;

import java.io.*;
import java.lang.annotation.*;
import java.lang.module.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.spi.*;
import java.util.stream.*;

/**
 * Java Shell Builder.
 *
 * @see <a href="https://github.com/sormuras/bach">https://github.com/sormuras/bach</a>
 */
class Bach {

  final Util util;
  final Variables vars;

  Bach() {
    this.vars = new Variables();
    this.util = new Util();

    try {
      Files.createDirectories(vars.temporary);
    } catch (IOException e) {
      throw new UncheckedIOException("creating directories failed: " + vars.temporary, e);
    }
  }

  /** Create command for executable name and any arguments. */
  Command command(String executable, Object... arguments) {
    return new Command(executable).addAll(arguments);
  }

  /** Return {@code true} if debugging is enabled. */
  boolean debug() {
    return vars.level.getSeverity() <= Level.DEBUG.getSeverity();
  }

  /** Log debug level message. */
  void debug(String format, Object... arguments) {
    vars.logger.accept(Level.DEBUG, String.format(format, arguments));
  }

  /** Log info level message. */
  void info(String format, Object... arguments) {
    vars.logger.accept(Level.INFO, String.format(format, arguments));
  }

  /** Run named executable with given arguments. */
  int run(String executable, Object... arguments) {
    info("[run] %s %s", executable, List.of(arguments));
    return command(executable, arguments).get();
  }

  /** Run function. */
  int run(Function<Bach, Integer> function) {
    return run(function.getClass().getSimpleName(), () -> function.apply(this));
  }

  /** Run tasks in parallel. */
  @SafeVarargs
  final int run(String caption, Supplier<Integer>... tasks) {
    return run(caption, Stream.of(tasks).parallel());
  }

  /** Run stream of tasks. */
  int run(String caption, Stream<Supplier<Integer>> stream) {
    info("[run] %s...", caption);
    var results = stream.map(CompletableFuture::supplyAsync).map(CompletableFuture::join);
    var result = results.reduce(0, Integer::sum);
    info("[run] %s done.", caption);
    if (result != 0) {
      throw new IllegalStateException("0 expected, but got: " + result);
    }
    return result;
  }

  /** Command option annotation, aka its "name". */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface Option {
    String value();
  }
  /** Indicate that an option may be used multiple times. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface Repeatable {}

  /** Command line program and in-process tool abstraction. */
  class Command implements Supplier<Integer> {

    /** Type-safe helper for adding common options. */
    class Helper {

      @SuppressWarnings("unused")
      void addModules(List<String> addModules) {
        if (addModules.isEmpty()) {
          return;
        }
        add("--add-modules");
        add(String.join(",", addModules));
      }

      @SuppressWarnings("unused")
      void patchModule(Map<String, List<Path>> patchModule) {
        patchModule.forEach(this::addPatchModule);
      }

      private void addPatchModule(String module, List<Path> paths) {
        if (paths.isEmpty()) {
          throw new AssertionError("expected at least one patch path entry for " + module);
        }
        var patches =
            paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        add("--patch-module");
        add(module + "=" + patches);
      }
    }

    final String executable;
    final List<String> arguments = new ArrayList<>();
    private final Helper helper = new Helper();
    private int dumpLimit = Integer.MAX_VALUE;
    private int dumpOffset = Integer.MAX_VALUE;

    private Map<String, ToolProvider> tools = Collections.emptyMap();
    private boolean executableSupportsArgumentFile = false;
    private UnaryOperator<String> executableToProgramOperator = UnaryOperator.identity();
    private Path temporaryDirectory = Bach.this.vars.temporary;

    /** Initialize this command instance. */
    Command(String executable) {
      this.executable = executable;
    }

    /** Add single argument composed of joined path names using {@link File#pathSeparator}. */
    Command add(Collection<Path> paths) {
      return add(paths.stream(), File.pathSeparator);
    }

    /** Add single non-null argument. */
    Command add(Object argument) {
      arguments.add(argument.toString());
      return this;
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
      } catch (IOException e) {
        throw new UncheckedIOException("walking path `" + root + "` failed", e);
      }
      return this;
    }

    /** Add all files visited by walking specified root paths recursively. */
    Command addAll(Collection<Path> roots, Predicate<Path> predicate) {
      roots.forEach(root -> addAll(root, predicate));
      return this;
    }

    /** Add all .java source files by walking specified root paths recursively. */
    Command addAllJavaFiles(List<Path> roots) {
      return addAll(roots, util::isJavaFile);
    }

    /** Add all reflected options. */
    Command addAllOptions(Object options) {
      return addAllOptions(options, UnaryOperator.identity());
    }

    /** Add all reflected options after a custom stream operator did its work. */
    Command addAllOptions(Object options, UnaryOperator<Stream<Field>> operator) {
      var stream =
          Arrays.stream(options.getClass().getDeclaredFields())
              .filter(field -> !field.isSynthetic())
              .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
              .filter(field -> !java.lang.reflect.Modifier.isPrivate(field.getModifiers()))
              .filter(field -> !java.lang.reflect.Modifier.isTransient(field.getModifiers()));
      stream = operator.apply(stream);
      stream.forEach(field -> addOptionUnchecked(options, field));
      return this;
    }

    private void addOption(Object options, Field field) throws ReflectiveOperationException {
      // custom option visitor method declared?
      try {
        options.getClass().getDeclaredMethod(field.getName(), Command.class).invoke(options, this);
        return;
      } catch (NoSuchMethodException e) {
        // fall-through
      }
      // get the field's value
      var value = field.get(options);
      // skip null field value
      if (value == null) {
        return;
      }
      // skip empty collections
      if (value instanceof Collection && ((Collection) value).isEmpty()) {
        return;
      }
      // common add helper available?
      try {
        Helper.class.getDeclaredMethod(field.getName(), field.getType()).invoke(helper, value);
        return;
      } catch (NoSuchMethodException e) {
        // fall-through
      }
      // get or generate option name
      var optional = Optional.ofNullable(field.getAnnotation(Option.class));
      var optionName = optional.map(Option::value).orElse(getOptionName(field.getName()));
      // is it an omissible boolean flag?
      if (field.getType() == boolean.class) {
        if (field.getBoolean(options)) {
          add(optionName);
        }
        return;
      }
      // add option name only if it is not empty
      if (!optionName.isEmpty()) {
        add(optionName);
      }
      // is value a collection?
      if (value instanceof Collection) {
        var iterator = ((Collection) value).iterator();
        var head = iterator.next();
        if (field.isAnnotationPresent(Repeatable.class)) {
          add(head);
          while (iterator.hasNext()) {
            add(optionName);
            add(iterator.next());
          }
          return;
        }
        if (head instanceof Path) {
          @SuppressWarnings("unchecked")
          var path = (Collection<Path>) value;
          add(path);
          return;
        }
      }
      // is value a charset?
      if (value instanceof Charset) {
        add(((Charset) value).name());
        return;
      }
      // finally, add string representation of the value
      add(value.toString());
    }

    private void addOptionUnchecked(Object options, Field field) {
      try {
        addOption(options, field);
      } catch (ReflectiveOperationException e) {
        throw new Error("reflecting option from field '" + field + "' failed for " + options, e);
      }
    }

    private String getOptionName(String fieldName) {
      var hasUppercase = !fieldName.equals(fieldName.toLowerCase());
      var defaultName = new StringBuilder();
      if (hasUppercase) {
        defaultName.append("--");
        fieldName
            .chars()
            .forEach(
                i -> {
                  if (Character.isUpperCase(i)) {
                    defaultName.append('-');
                    defaultName.append((char) Character.toLowerCase(i));
                  } else {
                    defaultName.append((char) i);
                  }
                });
      } else {
        defaultName.append('-');
        defaultName.append(fieldName.replace('_', '-'));
      }
      return defaultName.toString();
    }

    /** Dump command executables and arguments using the provided string consumer. */
    Command dump(Consumer<String> consumer) {
      var iterator = arguments.listIterator();
      consumer.accept(executable);
      while (iterator.hasNext()) {
        var argument = iterator.next();
        var nextIndex = iterator.nextIndex();
        var indent = nextIndex > dumpOffset || argument.startsWith("-") ? "" : "  ";
        consumer.accept(indent + argument);
        if (nextIndex > dumpLimit) {
          var last = arguments.size() - 1;
          var diff = last - nextIndex;
          if (diff > 1) {
            consumer.accept(indent + "... [omitted " + diff + " arguments]");
          }
          consumer.accept(indent + arguments.get(last));
          break;
        }
      }
      return this;
    }

    /** Set dump offset and limit. */
    Command mark(int limit) {
      if (limit < 0) {
        throw new IllegalArgumentException("limit must be greater then zero: " + limit);
      }
      this.dumpOffset = arguments.size();
      this.dumpLimit = arguments.size() + limit;
      return this;
    }

    /** Set argument file support. */
    Command setExecutableSupportsArgumentFile(boolean executableSupportsArgumentFile) {
      this.executableSupportsArgumentFile = executableSupportsArgumentFile;
      return this;
    }

    /** Put the tool into the internal map of tools. */
    Command setToolProvider(ToolProvider tool) {
      if (tools == Collections.EMPTY_MAP) {
        tools = new TreeMap<>();
      }
      tools.put(tool.name(), tool);
      return this;
    }

    Command setTemporaryDirectory(Path temporaryDirectory) {
      this.temporaryDirectory = temporaryDirectory;
      return this;
    }

    Command setExecutableToProgramOperator(UnaryOperator<String> executableToProgramOperator) {
      this.executableToProgramOperator = executableToProgramOperator;
      return this;
    }

    /** Create new argument array based on this command's arguments. */
    String[] toArgumentsArray() {
      return arguments.toArray(new String[0]);
    }

    /** Create new {@link ProcessBuilder} instance based on this command setup. */
    ProcessBuilder toProcessBuilder() {
      List<String> strings = new ArrayList<>(1 + arguments.size());
      var program = executableToProgramOperator.apply(executable);
      strings.add(program);
      strings.addAll(arguments);
      var commandLineLength = String.join(" ", strings).length();
      if (commandLineLength > 32000) {
        if (executableSupportsArgumentFile) {
          var timestamp = Instant.now().toString().replace("-", "").replace(":", "");
          var prefix = "bach-" + executable + "-arguments-" + timestamp + "-";
          try {
            var tempFile = Files.createTempFile(temporaryDirectory, prefix, ".txt");
            strings = List.of(program, "@" + Files.write(tempFile, arguments));
          } catch (IOException e) {
            throw new UncheckedIOException("creating temporary arguments file failed", e);
          }
        } else {
          info(
              "large command line (%s) detected, but %s does not support @argument file",
              commandLineLength, executable);
        }
      }
      var processBuilder = new ProcessBuilder(strings);
      processBuilder.redirectErrorStream(true);
      return processBuilder;
    }

    /**
     * Run this command, returning zero for a successful run.
     *
     * @return the result of executing the tool. A return value of 0 means the tool did not
     *     encounter any errors; any other value indicates that at least one error occurred during
     *     execution.
     */
    @Override
    public Integer get() {
      return run(UnaryOperator.identity(), this::toProcessBuilder);
    }

    /**
     * Run this command.
     *
     * @throws AssertionError if the execution result is not zero
     */
    void run() {
      var result = get();
      var successful = result == 0;
      if (successful) {
        return;
      }
      throw new AssertionError("expected an exit code of zero, but got: " + result);
    }

    /**
     * Run this command, returning zero for a successful run.
     *
     * @return the result of executing the tool. A return value of 0 means the tool did not
     *     encounter any errors; any other value indicates that at least one error occurred during
     *     execution.
     */
    int run(UnaryOperator<ToolProvider> operator, Supplier<ProcessBuilder> supplier) {
      if (debug()) {
        List<String> lines = new ArrayList<>();
        dump(lines::add);
        debug("running %s with %d argument(s)", executable, arguments.size());
        debug("%s", String.join("\n", lines));
      }
      var out = vars.printStreamOut;
      var err = vars.printStreamErr;
      var foundationTool = ToolProvider.findFirst(executable).orElse(null);
      var tool = tools.getOrDefault(executable, foundationTool);
      if (tool != null) {
        return operator.apply(tool).run(out, err, toArgumentsArray());
      }
      var processBuilder = supplier.get();
      if (debug()) {
        var program = processBuilder.command().get(0);
        if (!executable.equals(program)) {
          debug("replaced executable `%s` with program `%s`", executable, program);
        }
      }
      try {
        var process = processBuilder.start();
        process.getInputStream().transferTo(out);
        return process.waitFor();
      } catch (IOException | InterruptedException e) {
        throw new Error("executing `" + executable + "` failed", e);
      }
    }
  }

  /** Mutable runtime properties. */
  class Variables {

    private String defaultFormat = System.getProperty("bach.format", "[%s] %s%n");

    private Path defaultTemporary = Paths.get(System.getProperty("java.io.tmpdir"), ".bach");

    PrintStream printStreamOut = System.out;

    PrintStream printStreamErr = System.err;

    /** Logger level. */
    Level level = Level.valueOf(System.getProperty("bach.level", "ALL"));

    /** Logger function. */
    BiConsumer<Level, String> logger =
        (level, text) -> {
          var severity = level.getSeverity();
          if (severity >= Variables.this.level.getSeverity()) {
            var stream = severity >= Level.ERROR.getSeverity() ? printStreamErr : printStreamOut;
            stream.printf(defaultFormat, level, text);
          }
        };

    /** Offline mode switch. */
    boolean offline = Boolean.getBoolean("bach.offline");

    /** Temporary path. */
    Path temporary = Paths.get(System.getProperty("bach.temporary", defaultTemporary.toString()));

    /** List of repositories used for resolving dependencies. */
    List<URI> repositories =
        List.of(
            URI.create("http://central.maven.org/maven2"),
            URI.create("https://jcenter.bintray.com"),
            URI.create("https://oss.sonatype.org/content/repositories/snapshots"),
            URI.create("https://jitpack.io"));
  }

  static class Resolvable {

    static ResolvableBuilder builder() {
      return new ResolvableBuilder();
    }

    private String group;
    private String artifact;
    private String version;
    private String classifier = "";
    private String kind = "jar";
    private String file;

    private Resolvable() {}

    String getGroup() {
      return group;
    }

    String getArtifact() {
      return artifact;
    }

    String getVersion() {
      return version;
    }

    String getClassifier() {
      return classifier;
    }

    String getKind() {
      return kind;
    }

    String getFile() {
      return file;
    }

    boolean isLatest() {
      return version.equals("LATEST");
    }

    boolean isRelease() {
      return version.equals("RELEASE");
    }

    boolean isSnapshot() {
      return version.endsWith("SNAPSHOT");
    }

    String toPathString() {
      return Paths.get(getGroup().replace('.', '/'), getArtifact(), getVersion(), getFile())
          .toString()
          .replace('\\', '/');
    }
  }

  static class ResolvableBuilder {

    private Resolvable resolvable = new Resolvable();

    Resolvable build() {
      var result = resolvable;
      resolvable = new Resolvable();
      // assemble file name
      var versifier = result.version;
      if (!result.classifier.isEmpty()) {
        versifier = result.version + '-' + result.classifier;
      }
      result.file = result.artifact + '-' + versifier + '.' + result.kind;
      return result;
    }

    ResolvableBuilder group(String group) {
      resolvable.group = group;
      return this;
    }

    ResolvableBuilder artifact(String artifact) {
      resolvable.artifact = artifact;
      return this;
    }

    ResolvableBuilder version(String version) {
      resolvable.version = version;
      return this;
    }

    ResolvableBuilder classifier(String classifier) {
      resolvable.classifier = classifier;
      return this;
    }

    ResolvableBuilder kind(String kind) {
      resolvable.kind = kind;
      return this;
    }
  }

  /** Overlay. */
  class Util {

    /** Simple module information collector. */
    class ModuleInfo {
      private final String name;
      private final Set<String> requires;

      private ModuleInfo(String name, Set<String> requires) {
        this.name = name;
        this.requires = Set.copyOf(requires);
      }

      String getName() {
        return name;
      }

      Set<String> getRequires() {
        return requires;
      }
    }

    ModuleInfo moduleInfo(Path path) {
      if (Files.isDirectory(path)) {
        path = path.resolve("module-info.java");
      }
      try {
        return moduleInfo(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new UncheckedIOException("reading '" + path + "' failed", e);
      }
    }

    ModuleInfo moduleInfo(List<String> lines) {
      return moduleInfo(String.join("\n", lines));
    }

    ModuleInfo moduleInfo(String source) {
      // extract module name
      var namePattern = Pattern.compile("module (.+)\\{", Pattern.DOTALL);
      var nameMatcher = namePattern.matcher(source);
      if (!nameMatcher.find()) {
        throw new IllegalArgumentException(
            "expected java module descriptor unit, but got: " + source);
      }
      var name = nameMatcher.group(1).trim();

      // extract required module names
      var requiresPattern = Pattern.compile("requires (.+?);", Pattern.DOTALL);
      var requiresMatcher = requiresPattern.matcher(source);
      var requires = new TreeSet<String>();
      while (requiresMatcher.find()) {
        var split = requiresMatcher.group(1).trim().split("\\s+");
        requires.add(split[split.length - 1]);
      }
      return new ModuleInfo(name, requires);
    }

    /** Download the resource specified by its URI to the target directory. */
    Path download(URI uri, Path directory) throws IOException {
      return download(uri, directory, fileName(uri));
    }

    /** Download the resource from URI to the target directory using the provided file name. */
    Path download(URI uri, Path directory, String fileName) throws IOException {
      debug("download(uri:%s, directory:%s, fileName:%s)", uri, directory, fileName);
      var target = directory.resolve(fileName);
      if (vars.offline) {
        if (Files.exists(target)) {
          return target;
        }
        throw new Error("offline mode is active -- missing file " + target);
      }
      Files.createDirectories(directory);
      var connection = uri.toURL().openConnection();
      try (var sourceStream = connection.getInputStream()) {
        var urlLastModifiedMillis = connection.getLastModified();
        var urlLastModifiedTime = FileTime.fromMillis(urlLastModifiedMillis);
        if (Files.exists(target)) {
          debug("local file already exists -- comparing properties to remote file...");
          var unknownTime = urlLastModifiedMillis == 0L;
          if (Files.getLastModifiedTime(target).equals(urlLastModifiedTime) || unknownTime) {
            if (Files.size(target) == connection.getContentLengthLong()) {
              debug("local and remote file properties seem to match, using `%s`", target);
              return target;
            }
          }
          debug("local file `%s` differs from remote one -- replacing it", target);
        }
        debug("transferring `%s`...", uri);
        try (var targetStream = Files.newOutputStream(target)) {
          sourceStream.transferTo(targetStream);
        }
        if (urlLastModifiedMillis != 0L) {
          Files.setLastModifiedTime(target, urlLastModifiedTime);
        }
        info("`%s` downloaded [%d|%s]", fileName, Files.size(target), urlLastModifiedTime);
      }
      return target;
    }

    /** Return the file name of the uri. */
    String fileName(URI uri) {
      var urlString = uri.getPath();
      var begin = urlString.lastIndexOf('/') + 1;
      return urlString.substring(begin).split("\\?")[0].split("#")[0];
    }

    /** Return {@code true} if the path points to a canonical Java archive file. */
    boolean isJarFile(Path path) {
      if (Files.isRegularFile(path)) {
        return path.getFileName().toString().endsWith(".jar");
      }
      return false;
    }

    /** Return {@code true} if the path points to a canonical Java compilation unit file. */
    boolean isJavaFile(Path path) {
      if (Files.isRegularFile(path)) {
        var name = path.getFileName().toString();
        if (name.endsWith(".java")) {
          return name.indexOf('.') == name.length() - 5; // single dot in filename
        }
      }
      return false;
    }

    /** Return list of child directories directly present in {@code root} path. */
    List<Path> findDirectories(Path root) {
      if (Files.notExists(root)) {
        return Collections.emptyList();
      }
      try (var paths = Files.find(root, 1, (path, attr) -> Files.isDirectory(path))) {
        return paths.filter(path -> !root.equals(path)).collect(Collectors.toList());
      } catch (IOException e) {
        throw new UncheckedIOException("findDirectories failed for root: " + root, e);
      }
    }

    /** Return list of child directory names directly present in {@code root} path. */
    List<String> findDirectoryNames(Path root) {
      return findDirectories(root)
          .stream()
          .map(root::relativize)
          .map(Path::toString)
          .collect(Collectors.toList());
    }

    /** Return the location path of the module reference. */
    Path getPath(ModuleReference moduleReference) {
      return Paths.get(moduleReference.location().orElseThrow());
    }

    /** Collect all Java archives ({@code .jar} files) into a list. */
    List<Path> getClassPath(List<Path> modulePath, List<Path> depsPath) {
      List<Path> classPath = new ArrayList<>();
      for (var path : modulePath) {
        ModuleFinder.of(path).findAll().stream().map(this::getPath).forEach(classPath::add);
      }
      for (var path : depsPath) {
        try (Stream<Path> paths = Files.walk(path, 1)) {
          paths.filter(this::isJarFile).forEach(classPath::add);
        } catch (IOException e) {
          throw new UncheckedIOException("failed adding jars from " + path + " to classpath", e);
        }
      }
      return classPath;
    }

    /** Return patch map using two lists of paths. */
    Map<String, List<Path>> getPatchMap(List<Path> basePath, List<Path> patchPath) {
      var map = new TreeMap<String, List<Path>>();
      for (var base : basePath) {
        for (var name : findDirectoryNames(base)) {
          for (var patch : patchPath) {
            var candidate = patch.resolve(name);
            if (Files.isDirectory(candidate)) {
              map.computeIfAbsent(name, __ -> new ArrayList<>()).add(candidate);
            }
          }
        }
      }
      return map;
    }

    /** Find foundation JDK command by its name. */
    Optional<Path> findJdkCommandPath(String name) {
      var bin = Paths.get(System.getProperty("java.home")).toAbsolutePath().resolve("bin");
      for (var suffix : List.of("", ".exe")) {
        var tool = bin.resolve(name + suffix);
        if (Files.isExecutable(tool)) {
          return Optional.of(tool);
        }
      }
      return Optional.empty();
    }

    /** Find foundation JDK command by its name. */
    String getJdkCommand(String name) {
      return findJdkCommandPath(name).map(Object::toString).orElseThrow();
    }

    /** Calculate external module names. */
    Set<String> getExternalModuleNames(Path... roots) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeSet<String>();
      var paths = new ArrayList<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root)) {
          stream.filter(path -> path.endsWith("module-info.java")).forEach(paths::add);
        } catch (IOException e) {
          throw new UncheckedIOException("walking path failed for: " + root, e);
        }
      }
      for (var path : paths) {
        var info = moduleInfo(path);
        declaredModules.add(info.getName());
        requiredModules.addAll(info.getRequires());
      }
      var externalModules = new TreeSet<>(requiredModules);
      externalModules.removeAll(declaredModules);
      return externalModules;
    }

    /** Delete directory. */
    void removeTree(Path root) {
      removeTree(root, path -> true);
    }

    /** Delete selected files and directories from the root directory. */
    void removeTree(Path root, Predicate<Path> filter) {
      // trivial case: delete existing single file or empty directory right away
      try {
        if (Files.deleteIfExists(root)) {
          return;
        }
      } catch (IOException ignored) {
        // fall-through
      }
      // default case: walk the tree...
      try (var stream = Files.walk(root)) {
        var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (var path : selected.collect(Collectors.toList())) {
          Files.deleteIfExists(path);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("removing tree failed: " + root, e);
      }
    }

    /** Dump directory tree structure. */
    void dumpTree(Path root, Consumer<String> out) {
      if (Files.exists(root)) {
        out.accept(root.toString());
      }
      try (Stream<Path> stream = Files.walk(root).sorted()) {
        for (Path path : stream.collect(Collectors.toList())) {
          String string = root.relativize(path).toString();
          String prefix = string.isEmpty() ? "" : File.separator;
          out.accept("." + prefix + string);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("dumping tree failed: " + root, e);
      }
    }

    /** Copy source directory to target directory. */
    void copyTree(Path source, Path target) {
      copyTree(source, target, __ -> true);
    }

    /** Copy source directory to target directory. */
    void copyTree(Path source, Path target, Predicate<Path> filter) {
      debug("treeCopy(source:`%s`, target:`%s`)%n", source, target);
      if (!Files.exists(source)) {
        return;
      }
      if (!Files.isDirectory(source)) {
        throw new IllegalArgumentException("source must be a directory: " + source);
      }
      if (Files.exists(target)) {
        if (!Files.isDirectory(target)) {
          throw new IllegalArgumentException("target must be a directory: " + target);
        }
        try {
          if (Files.isSameFile(source, target)) {
            return;
          }
        } catch (IOException e) {
          throw new UncheckedIOException("copyTree failed", e);
        }
      }
      try (Stream<Path> stream = Files.walk(source).sorted()) {
        int counter = 0;
        List<Path> paths = stream.collect(Collectors.toList());
        for (Path path : paths) {
          Path destination = target.resolve(source.relativize(path));
          if (Files.isDirectory(path)) {
            Files.createDirectories(destination);
            continue;
          }
          if (filter.test(path)) {
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            counter++;
          }
        }
        debug("copied %d file(s) of %d elements...%n", counter, paths.size());
      } catch (IOException e) {
        throw new UncheckedIOException("copyTree failed", e);
      }
    }

    Path resolve(Resolvable resolvable, Path targetDirectory, URI repository) throws Exception {
      var uri = URI.create(repository.toString() + '/' + resolvable.toPathString());
      var fileName = fileName(uri);
      // revert local file name with constant version attribute
      if (resolvable.isSnapshot()) {
        fileName = resolvable.getFile();
      }
      return download(uri, targetDirectory, fileName);
    }

    Optional<Path> resolve(Resolvable resolvable, Path targetDirectory) {
      return resolve(resolvable, targetDirectory, vars.repositories);
    }

    Optional<Path> resolve(Resolvable resolvable, Path targetDirectory, List<URI> repositories) {
      for (var repository : repositories) {
        try {
          return Optional.of(resolve(resolvable, targetDirectory, repository));
        } catch (Exception e) {
          debug("resolve(repository:'%s') failed: %s", repository, e);
        }
      }
      return Optional.empty();
    }
  }
}

/**
 * You can use the foundation JDK tools and commands to create and build applications.
 *
 * @see <a
 *     href="https://docs.oracle.com/javase/9/tools/main-tools-create-and-build-applications.htm">Main
 *     Tools to Create and Build Applications</a>
 */
interface JdkTool extends Function<Bach, Integer> {
  /**
   * You can use the javac tool and its options to read Java class and interface definitions and
   * compile them into bytecode and class files.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/javac.htm">javac</a>
   */
  class Javac implements JdkTool {
    /** (Legacy) class path. */
    List<Path> classPath = List.of();

    /** (Legacy) locations where to find Java source files. */
    @Bach.Option("--source-path")
    transient List<Path> classSourcePath = List.of();

    /** Generates all debugging information, including local variables. */
    @Bach.Option("-g")
    boolean generateAllDebuggingInformation = false;

    /** Output source locations where deprecated APIs are used. */
    boolean deprecation = true;

    /** The destination directory for class files. */
    @Bach.Option("-d")
    Path destination = null;

    /** Specify character encoding used by source files. */
    Charset encoding = StandardCharsets.UTF_8;

    /** Terminate compilation if warnings occur. */
    @Bach.Option("-Werror")
    boolean failOnWarnings = true;

    /** Overrides or augments a module with classes and resources in JAR files or directories. */
    Map<String, List<Path>> patchModule = Map.of();

    /** Specify where to find application modules. */
    List<Path> modulePath = List.of();

    /** Where to find input source files for multiple modules. */
    List<Path> moduleSourcePath = List.of();

    /** Specifies root modules to resolve in addition to the initial modules. */
    List<String> addModules = List.of();

    /** Compiles only the specified module and checks timestamps. */
    @Bach.Option("--module")
    String module = null;

    /** Generate metadata for reflection on method parameters. */
    boolean parameters = true;

    /** Output messages about what the compiler is doing. */
    boolean verbose = false;

    /** Create javac command with options and source files added. */
    @Override
    public Bach.Command toCommand(Bach bach, Object... extras) {
      var command = JdkTool.super.toCommand(bach, extras);
      command.mark(10);
      command.addAll(classSourcePath, bach.util::isJavaFile);
      if (module == null) {
        command.addAll(moduleSourcePath, bach.util::isJavaFile);
      }
      command.setExecutableSupportsArgumentFile(true);
      return command;
    }
  }

  /**
   * You can use the java command to launch a Java application.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/java.htm">java</a>
   */
  class Java implements JdkTool {
    /** (Legacy) class path. */
    List<Path> classPath = List.of();

    /**
     * Creates the VM but doesn't execute the main method.
     *
     * <p>This {@code --dry-run} option may be useful for validating the command-line options such
     * as the module system configuration.
     */
    boolean dryRun = false;

    /** The name of the Java Archive (JAR) file to be called. */
    Path jar = null;

    /** Overrides or augments a module with classes and resources in JAR files or directories. */
    Map<String, List<Path>> patchModule = Map.of();

    /** Where to find application modules. */
    List<Path> modulePath = List.of();

    /** Specifies root modules to resolve in addition to the initial modules. */
    List<String> addModules = List.of();

    /** Initial module to resolve and the name of the main class to execute. */
    @Bach.Option("--module")
    String module = null;

    /** Arguments passed to the main entry-point. */
    transient List<Object> args = List.of();

    /** Create java command with options and source files added. */
    @Override
    public Bach.Command toCommand(Bach bach, Object... extras) {
      var command = JdkTool.super.toCommand(bach, extras);
      command.setExecutableSupportsArgumentFile(true);
      command.setExecutableToProgramOperator(bach.util::getJdkCommand);
      command.mark(9);
      command.addAll(args);
      return command;
    }
  }

  /**
   * You use the javadoc tool and options to generate HTML pages of API documentation from Java
   * source files.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/javadoc.htm">javadoc</a>
   */
  class Javadoc implements JdkTool {

    enum Visibility {
      /** Shows only public elements. */
      PUBLIC,
      /** Shows public and protected elements. This is the default. */
      PROTECTED,
      /** Shows public, protected, and package elements. */
      PACKAGE,
      /** Shows all elements. */
      PRIVATE
    }

    /** The destination directory for generated files. */
    @Bach.Option("-d")
    Path destination = null;

    /** Shuts off messages so that only the warnings and errors appear. */
    boolean quiet = true;

    /** Generates HTML5 output. */
    boolean html5 = true;

    /** Adds HTML keyword {@code <META>} tags to the generated file for each class. */
    boolean keywords = true;

    /** Creates links to existing documentation of externally referenced classes. */
    @Bach.Repeatable List<String> link = List.of();

    /** Creates an HTML version of each source file. */
    boolean linksource = false;

    /** Enables recommended checks for problems in javadoc comments. */
    String doclint = "";

    void doclint(Bach.Command command) {
      if (doclint == null) {
        return;
      }
      if (doclint.isEmpty()) {
        command.add("-Xdoclint");
        return;
      }
      // Enable or disable specific checks for problems in javadoc
      // comments, where <group> is one of accessibility, html,
      // missing, reference, or syntax.
      command.add("-Xdoclint:" + doclint); // all,-missing,-reference...
    }

    /** Specifies which declarations (fields or methods) are documented. */
    Visibility showMembers = Visibility.PROTECTED;

    void showMembers(Bach.Command command) {
      if (showMembers == Visibility.PROTECTED) {
        return;
      }
      command.add("--show-members").add(showMembers.name().toLowerCase());
    }

    /** Specifies which declarations (interfaces or classes) are documented. */
    Visibility showTypes = Visibility.PROTECTED;

    void showTypes(Bach.Command command) {
      if (showTypes == Visibility.PROTECTED) {
        return;
      }
      command.add("--show-types").add(showTypes.name().toLowerCase());
    }
  }

  /**
   * You can use the jar command to create an archive for classes and resources, and manipulate or
   * restore individual classes or resources from an archive.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/jar.htm">jar</a>
   */
  class Jar implements JdkTool {
    /** Specify the operation mode for the jar command. */
    @Bach.Option("")
    String mode = "--create";

    /** Specifies the archive file name. */
    @Bach.Option("--file")
    Path file = Paths.get("out.jar");

    /** Specifies the application entry point for stand-alone applications. */
    String mainClass = null;

    /** Specifies the module version, when creating a modular JAR file. */
    String moduleVersion = null;

    /** Stores without using ZIP compression. */
    boolean noCompress = false;

    /** Sends or prints verbose output to standard output. */
    @Bach.Option("--verbose")
    boolean verbose = false;

    /** Changes to the specified directory and includes the files at the end of the command. */
    @Bach.Option("-C")
    Path path = null;

    @Override
    public Bach.Command toCommand(Bach bach, Object... extras) {
      var command = JdkTool.super.toCommand(bach, extras);
      if (path != null) {
        command.mark(1);
        command.add(".");
      }
      return command;
    }
  }

  /**
   * You use the jdeps command to launch the Java class dependency analyzer.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/jdeps.htm">jdeps</a>
   */
  class Jdeps implements JdkTool {
    /** Specifies where to find class files. */
    List<Path> classpath = List.of();

    /** Recursively traverses all dependencies. */
    boolean recursive = true;

    /** Finds class-level dependencies in JDK internal APIs. */
    boolean jdkInternals = false;

    /** Shows profile or the file containing a package. */
    boolean profile = false;

    /** Restricts analysis to APIs, like deps from the signature of public and protected members. */
    boolean apionly = false;

    /** Prints dependency summary only. */
    boolean summary = false;

    /** Prints all class-level dependencies. */
    boolean verbose = false;
  }

  /**
   * You can use the jlink tool to assemble and optimize a set of modules and their dependencies
   * into a custom runtime image.
   *
   * @see <a href="https://docs.oracle.com/javase/9/tools/jlink.htm">jlink</a>
   */
  class Jlink implements JdkTool {
    /** Where to find application modules. */
    List<Path> modulePath = List.of();

    /** The directory that contains the resulting runtime image. */
    @Bach.Option("--output")
    Path output = null;
  }

  /** Execute this tool. */
  @Override
  default Integer apply(Bach bach) {
    return toCommand(bach).get();
  }

  /** Name of this tool, like {@code javac} or {@code jar}. */
  default String name() {
    return getClass().getSimpleName().toLowerCase();
  }

  /** Create command instance based on this tool's options. */
  default Bach.Command toCommand(Bach bach, Object... extras) {
    return bach.command(name()).addAllOptions(this).addAll(extras);
  }
}

/** Project build support. */
class Project {

  static ProjectBuilder builder() {
    return new ProjectBuilder();
  }

  private String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
  private String version = "1.0.0-SNAPSHOT";
  private Path target = Paths.get("target", "bach");
  private Map<String, ModuleGroup> moduleGroups = new TreeMap<>();

  private Project() {}

  String name() {
    return name;
  }

  String version() {
    return version;
  }

  Path target() {
    return target;
  }

  ModuleGroup moduleGroup(String name) {
    return moduleGroups.get(name);
  }

  Collection<ModuleGroup> moduleGroups() {
    return moduleGroups.values();
  }

  /** A mutable project. */
  static class ProjectBuilder {

    private Project project = new Project();

    Project build() {
      var result = project;
      project = new Project();
      return result;
    }

    ProjectBuilder name(String name) {
      project.name = name;
      return this;
    }

    ProjectBuilder version(String version) {
      project.version = version;
      return this;
    }

    ProjectBuilder target(Path target) {
      project.target = target;
      return this;
    }

    ModuleGroupBuilder newModuleGroup(String name) {
      if (project.moduleGroups.containsKey(name)) {
        throw new IllegalArgumentException(name + " already defined");
      }
      return new ModuleGroupBuilder(this, name);
    }
  }

  /** Source set, like {@code main} or {@code test}. */
  static class ModuleGroup {

    private final String name;
    private Path destination;
    private List<Path> modulePath;
    private List<Path> moduleSourcePath;
    private Map<String, List<Path>> patchModule = Map.of();

    private ModuleGroup(String name) {
      this.name = name;
    }

    String name() {
      return name;
    }

    Path destination() {
      return destination;
    }

    List<Path> modulePath() {
      return modulePath;
    }

    List<Path> moduleSourcePath() {
      return moduleSourcePath;
    }

    Map<String, List<Path>> patchModule() {
      return patchModule;
    }
  }

  /** A mutable source set. */
  static class ModuleGroupBuilder {

    private final ProjectBuilder projectBuilder;
    private final ModuleGroup group;

    private ModuleGroupBuilder(ProjectBuilder projectBuilder, String name) {
      this.projectBuilder = projectBuilder;
      this.group = new ModuleGroup(name);
      this.group.destination = projectBuilder.project.target.resolve(Paths.get(name, "modules"));
      this.group.modulePath = List.of();
      this.group.moduleSourcePath = List.of(Paths.get("src", name, "java"));
    }

    ProjectBuilder end() {
      projectBuilder.project.moduleGroups.put(group.name, group);
      return projectBuilder;
    }

    ModuleGroupBuilder destination(Path destination) {
      group.destination = destination;
      return this;
    }

    ModuleGroupBuilder modulePath(List<Path> modulePath) {
      group.modulePath = modulePath;
      return this;
    }

    ModuleGroupBuilder moduleSourcePath(List<Path> moduleSourcePath) {
      group.moduleSourcePath = moduleSourcePath;
      return this;
    }

    ModuleGroupBuilder patchModule(Map<String, List<Path>> patchModule) {
      group.patchModule = patchModule;
      return this;
    }
  }
}

/** A task is a piece of code that can be executed. */
interface Task extends Supplier<Integer> {

  /** Execute {@code javac} for all module groups. */
  class CompilerTask implements Task {
    final Bach bach;
    final Project project;

    CompilerTask(Bach bach, Project project) {
      this.bach = bach;
      this.project = project;
    }

    int compile(Project.ModuleGroup group) {
      bach.info("[compile] %s", group.name());
      var javac = new JdkTool.Javac();
      javac.destination = group.destination();
      javac.moduleSourcePath = group.moduleSourcePath();
      javac.modulePath = group.modulePath();
      javac.patchModule = group.patchModule();
      return javac.toCommand(bach).get();
    }

    @Override
    public Integer get() {
      bach.info("[compiler] %s", project);
      return project.moduleGroups().stream().mapToInt(this::compile).sum();
    }
  }
}
