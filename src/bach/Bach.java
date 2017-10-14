/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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
import java.util.function.*;
import java.util.regex.*;
import java.util.spi.*;
import java.util.stream.*;

/**
 * Java Shell Builder.
 *
 * @see <a href="https://github.com/sormuras/bach">https://github.com/sormuras/bach</a>
 */
interface Bach {

  /** Log instance. */
  Log log = new Log();

  /**
   * Run executable tool by its name and add all arguments as single elements.
   *
   * @throws AssertionError if the execution result is not zero
   */
  static void run(String executable, Object... arguments) {
    new Command(executable).addAll(List.of(arguments)).run();
  }

  /** Default Bach settings. */
  class Default {
    static boolean VERBOSE = Boolean.getBoolean("bach.verbose");
    static Path BACH_PATH = Paths.get(sys("bach.path", ".bach"));
    static Path RESOLVE_PATH = Paths.get(sys("bach.resolve.path", BACH_PATH.resolve("resolved")));
    static String RESOLVABLE_VERSION = sys("bach.resolvable.version", "RELEASE");
    static Path JDK_HOME = Basics.resolveJdkHome();
    static List<String> JDK_TOOL_SUFFIXES = List.of("", ".exe");

    private static String sys(String key, Object def) {
      return System.getProperty(key, def.toString());
    }
  }

  /** Command builder and tool executor support. */
  class Command {

    interface Visitor extends Consumer<Command> {}

    static Visitor visit(Consumer<Command> consumer) {
      return consumer::accept;
    }

    /** Command option annotation. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Option {
      String value();
    }

    /** Type-safe helper for adding common options. */
    class Helper {

      @SuppressWarnings("unused")
      void patchModule(Map<String, List<Path>> patchModule) {
        patchModule.forEach(this::addPatchModule);
      }

      private void addPatchModule(String module, List<Path> paths) {
        if (paths.isEmpty()) {
          throw new AssertionError("expected at least one patch path entry for " + module);
        }
        List<String> names = paths.stream().map(Path::toString).collect(Collectors.toList());
        add("--patch-module");
        add(module + "=" + String.join(File.pathSeparator, names));
      }
    }

    final String executable;
    final List<String> arguments = new ArrayList<>();
    private final Helper helper = new Helper();
    private int dumpLimit = Integer.MAX_VALUE;
    private int dumpOffset = Integer.MAX_VALUE;
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private Map<String, ToolProvider> tools = Collections.emptyMap();
    private boolean executableSupportsArgumentFile = false;

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
      if (argument instanceof Visitor) {
        ((Visitor) argument).accept(this);
        return this;
      }
      arguments.add(argument.toString());
      return this;
    }

    /** Add single argument composed of all stream elements joined by specified separator. */
    Command add(Stream<?> stream, String separator) {
      return add(stream.map(Object::toString).collect(Collectors.joining(separator)));
    }

    /** Add all files visited by walking specified root paths recursively. */
    Command addAll(Collection<Path> roots, Predicate<Path> predicate) {
      roots.forEach(root -> addAll(root, predicate));
      return this;
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addAll(Iterable<?> arguments) {
      arguments.forEach(this::add);
      return this;
    }

    /** Add all files visited by walking specified root path recursively. */
    Command addAll(Path root, Predicate<Path> predicate) {
      try (Stream<Path> stream = Files.walk(root).filter(predicate)) {
        stream.forEach(this::add);
      } catch (IOException e) {
        throw new UncheckedIOException("walking path `" + root + "` failed", e);
      }
      return this;
    }

    /** Add all .java source files by walking specified root path recursively. */
    Command addAllJavaFiles(Path root) {
      return addAll(root, Basics::isJavaFile);
    }

    /** Add all reflected options. */
    Command addAllOptions(Object options) {
      return addAllOptions(options, UnaryOperator.identity());
    }

    /** Add all reflected options after a custom stream operator did its work. */
    Command addAllOptions(Object options, UnaryOperator<Stream<Field>> operator) {
      Stream<Field> stream =
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
      Object value = field.get(options);
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
      Optional<Option> optional = Optional.ofNullable(field.getAnnotation(Option.class));
      String optionName = optional.map(Option::value).orElse(getOptionName(field.getName()));
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
      // is value a collection of paths?
      if (value instanceof Collection) {
        try {
          @SuppressWarnings("unchecked")
          Collection<Path> path = (Collection<Path>) value;
          add(path);
          return;
        } catch (ClassCastException e) {
          // fall-through
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
      boolean hasUppercase = !fieldName.equals(fieldName.toLowerCase());
      StringBuilder defaultName = new StringBuilder();
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
      ListIterator<String> iterator = arguments.listIterator();
      consumer.accept(executable);
      while (iterator.hasNext()) {
        String argument = iterator.next();
        int nextIndex = iterator.nextIndex();
        String indent = nextIndex > dumpOffset || argument.startsWith("-") ? "" : "  ";
        consumer.accept(indent + argument);
        if (nextIndex > dumpLimit) {
          int last = arguments.size() - 1;
          int diff = last - nextIndex;
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

    /** Set standard output and error streams. */
    Command setStandardStreams(PrintStream out, PrintStream err) {
      this.out = out;
      this.err = err;
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

    /** Create new argument array based on this command's arguments. */
    String[] toArgumentsArray() {
      return arguments.toArray(new String[arguments.size()]);
    }

    /** Create new {@link ProcessBuilder} instance based on this command setup. */
    ProcessBuilder toProcessBuilder() {
      List<String> strings = new ArrayList<>(1 + arguments.size());
      strings.add(Basics.resolveJdkTool(executable).map(Path::toString).orElse(executable));
      strings.addAll(arguments);
      int commandLineLength = String.join(" ", strings).length();
      if (commandLineLength > 32000) {
        if (executableSupportsArgumentFile) {
          String timestamp = Instant.now().toString().replace("-", "").replace(":", "");
          String prefix = executable + "-arguments-" + timestamp + "-";
          try {
            Path tempFile = Files.createTempFile(prefix, ".txt");
            strings = List.of(executable, "@" + Files.write(tempFile, arguments));
          } catch (IOException e) {
            throw new UncheckedIOException("creating temporary arguments file failed", e);
          }
        } else {
          err.println(
              String.format(
                  "large command line (%s) detected, but %s does not support @argument file",
                  commandLineLength, executable));
        }
      }
      ProcessBuilder processBuilder = new ProcessBuilder(strings);
      processBuilder.redirectErrorStream(true);
      return processBuilder;
    }

    /**
     * Run this command.
     *
     * @throws AssertionError if the execution result is not zero
     */
    void run() {
      int result = run(UnaryOperator.identity(), this::toProcessBuilder);
      boolean successful = result == 0;
      if (successful) {
        return;
      }
      throw new AssertionError("expected an exit code of zero, but got: " + result);
    }

    /**
     * Runs an instance of the tool, returning zero for a successful run.
     *
     * @return the result of executing the tool. A return value of 0 means the tool did not
     *     encounter any errors; any other value indicates that at least one error occurred during
     *     execution.
     */
    int run(UnaryOperator<ToolProvider> operator, Supplier<ProcessBuilder> supplier) {
      if (log.isEnabled()) {
        List<String> lines = new ArrayList<>();
        dump(lines::add);
        log.info("running %s with %d argument(s)", executable, arguments.size());
        log.verbose("%s", String.join("\n", lines));
      }
      ToolProvider systemTool = ToolProvider.findFirst(executable).orElse(null);
      ToolProvider tool = tools.getOrDefault(executable, systemTool);
      if (tool != null) {
        return operator.apply(tool).run(out, err, toArgumentsArray());
      }
      ProcessBuilder processBuilder = supplier.get();
      if (log.isEnabled()) {
        String actual = processBuilder.command().get(0);
        if (!executable.equals(actual)) {
          log.info("replaced %s with %s", executable, actual);
        }
      }
      try {
        Process process = processBuilder.start();
        process.getInputStream().transferTo(out);
        return process.waitFor();
      } catch (IOException | InterruptedException e) {
        throw new Error("executing `" + executable + "` failed", e);
      }
    }
  }

  /** Common utilities and helpers. */
  interface Basics {

    /** Download the resource specified by its URI to the target directory. */
    static Path download(URI uri, Path targetDirectory) throws IOException {
      return download(uri, targetDirectory, getFileName(uri), path -> true);
    }

    /** Download the resource from URI to the target directory using the provided file name. */
    static Path download(URI uri, Path directory, String fileName, Predicate<Path> skip)
        throws IOException {
      log.info("download(uri:%s, directory:%s, fileName:%s)", uri, directory, fileName);
      URL url = uri.toURL();
      Files.createDirectories(directory);
      Path target = directory.resolve(fileName);
      if (Boolean.getBoolean("bach.offline")) {
        if (Files.exists(target)) {
          return target;
        }
        throw new Error("offline mode is active -- missing file " + target);
      }
      URLConnection urlConnection = url.openConnection();
      FileTime urlLastModifiedTime = FileTime.fromMillis(urlConnection.getLastModified());
      if (urlLastModifiedTime.toMillis() == 0) {
        throw new IOException("last-modified header field not available");
      }
      if (Files.exists(target)) {
        log.verbose("compare last modified time [%s] of local file...", urlLastModifiedTime);
        if (Files.getLastModifiedTime(target).equals(urlLastModifiedTime)) {
          if (Files.size(target) == urlConnection.getContentLengthLong()) {
            if (skip.test(target)) {
              log.verbose("skipped, using `%s`", target);
              return target;
            }
          }
        }
        Files.delete(target);
      }
      log.verbose("transferring `%s`...", uri);
      try (InputStream sourceStream = url.openStream();
          OutputStream targetStream = Files.newOutputStream(target)) {
        sourceStream.transferTo(targetStream);
      }
      Files.setLastModifiedTime(target, urlLastModifiedTime);
      log.verbose("stored `%s` [%s]", target, urlLastModifiedTime);
      return target;
    }

    static List<Path> findDirectories(Path root) {
      if (Files.notExists(root)) {
        return Collections.emptyList();
      }
      try (Stream<Path> paths = Files.find(root, 1, (path, attr) -> Files.isDirectory(path))) {
        return paths.filter(path -> !root.equals(path)).collect(Collectors.toList());
      } catch (IOException e) {
        throw new UncheckedIOException("findDirectories failed for root: " + root, e);
      }
    }

    static List<String> findDirectoryNames(Path root) {
      return findDirectories(root)
          .stream()
          .map(root::relativize)
          .map(Path::toString)
          .collect(Collectors.toList());
    }

    static Set<String> findExternalModuleNames(Path... roots) {
      Set<String> declaredModules = new TreeSet<>();
      Set<String> requiredModules = new TreeSet<>();
      List<Path> paths = new ArrayList<>();
      for (Path root : roots) {
        try (Stream<Path> stream = Files.walk(root)) {
          stream.filter(path -> path.endsWith("module-info.java")).forEach(paths::add);
        } catch (IOException e) {
          throw new UncheckedIOException("findExternalModuleNames failed for root: " + root, e);
        }
      }
      for (Path path : paths) {
        Bach.Basics.ModuleInfo info = Bach.Basics.ModuleInfo.of(path);
        declaredModules.add(info.getName());
        requiredModules.addAll(info.getRequires());
      }
      Set<String> externalModules = new TreeSet<>(requiredModules);
      externalModules.removeAll(declaredModules);
      return externalModules;
    }

    /** Return path to JDK installation directory. */
    static Path resolveJdkHome() {
      Path executable = ProcessHandle.current().info().command().map(Paths::get).orElse(null);
      if (executable != null && executable.getNameCount() > 2) {
        // noinspection ConstantConditions -- count is 3 or higher: "<JDK_HOME>/bin/java[.exe]"
        return executable.getParent().getParent().toAbsolutePath();
      }
      String jdkHome = System.getenv("JDK_HOME");
      if (jdkHome != null) {
        return Paths.get(jdkHome).toAbsolutePath();
      }
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome != null) {
        return Paths.get(javaHome).toAbsolutePath();
      }
      Path fallback = Paths.get("jdk-" + Runtime.version().major()).toAbsolutePath();
      log.info("JDK home path not found, using: `%s`", fallback);
      return fallback;
    }

    static Optional<Path> resolveJdkTool(String name) {
      Path bin = Default.JDK_HOME.resolve("bin");
      for (String suffix : Default.JDK_TOOL_SUFFIXES) {
        Path tool = bin.resolve(name + suffix);
        if (Files.isExecutable(tool)) {
          return Optional.of(tool);
        }
      }
      return Optional.empty();
    }

    /** Extract the file name from the uri. */
    static String getFileName(URI uri) {
      String urlString = uri.getPath();
      int begin = urlString.lastIndexOf('/') + 1;
      return urlString.substring(begin).split("\\?")[0].split("#")[0];
    }

    static Path getPath(ModuleReference moduleReference) {
      return Paths.get(moduleReference.location().orElseThrow(AssertionError::new));
    }

    static List<Path> getClassPath(List<Path> modulePaths, List<Path> depsPaths) {
      List<Path> classPath = new ArrayList<>();
      for (Path path : modulePaths) {
        ModuleFinder.of(path).findAll().stream().map(Basics::getPath).forEach(classPath::add);
      }
      for (Path path : depsPaths) {
        try (Stream<Path> paths = Files.walk(path, 1)) {
          paths.filter(Basics::isJarFile).forEach(classPath::add);
        } catch (IOException e) {
          throw new UncheckedIOException("failed adding jars from " + path + " to classpath", e);
        }
      }
      return classPath;
    }

    static Map<String, List<Path>> getPatchMap(List<Path> basePaths, List<Path> patchPaths) {
      Map<String, List<Path>> map = new TreeMap<>();
      for (Path basePath : basePaths) {
        for (String baseName : findDirectoryNames(basePath)) {
          for (Path patchPath : patchPaths) {
            Path candidate = patchPath.resolve(baseName);
            if (Files.isDirectory(candidate)) {
              map.computeIfAbsent(baseName, key -> new ArrayList<>()).add(candidate);
            }
          }
        }
      }
      return map;
    }

    /** Return {@code true} if the path points to a canonical Java archive file. */
    static boolean isJarFile(Path path) {
      if (Files.isRegularFile(path)) {
        return path.getFileName().toString().endsWith(".jar");
      }
      return false;
    }

    /** Return {@code true} if the path points to a canonical Java compilation unit file. */
    static boolean isJavaFile(Path path) {
      if (Files.isRegularFile(path)) {
        String unit = path.getFileName().toString();
        if (unit.endsWith(".java")) {
          return unit.indexOf('.') == unit.length() - 5; // single dot in filename
        }
      }
      return false;
    }

    /** Resolve maven jar artifact. */
    static Path resolve(String group, String artifact, String version) {
      log.info("resolve(group:%s, artifact:%s, version:%s)", group, artifact, version);
      Path destination = Default.RESOLVE_PATH;
      List<String> repositories = Resolvable.REPOSITORIES;
      return new Resolvable(group, artifact, version).resolve(destination, repositories);
    }

    /** Extract substring between begin and end tags. */
    static String substring(String string, String beginTag, String endTag) {
      int initialIndex = string.indexOf(beginTag);
      if (initialIndex < 0) {
        throw new NoSuchElementException("no '" + beginTag + "' in: " + string);
      }
      int beginIndex = initialIndex + beginTag.length();
      int endIndex = string.indexOf(endTag, beginIndex);
      return string.substring(beginIndex, endIndex).trim();
    }

    /** Copy source directory to target directory. */
    static void treeCopy(Path source, Path target) throws IOException {
      treeCopy(source, target, __ -> true);
    }

    /** Copy source directory to target directory. */
    static void treeCopy(Path source, Path target, Predicate<Path> filter) throws IOException {
      log.verbose("treeCopy(source:`%s`, target:`%s`)%n", source, target);
      if (!Files.exists(source)) {
        return;
      }
      if (!Files.isDirectory(source)) {
        throw new IllegalArgumentException("source must be a directory: " + source);
      }
      if (Files.exists(target)) {
        if (Files.isSameFile(source, target)) {
          return;
        }
        if (!Files.isDirectory(target)) {
          throw new IllegalArgumentException("target must be a directory: " + target);
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
        log.verbose("copied %d file(s) of %d elements...%n", counter, paths.size());
      } catch (IOException e) {
        throw new UncheckedIOException("treeCopy failed", e);
      }
    }

    /** Delete directory. */
    static void treeDelete(Path root) throws IOException {
      treeDelete(root, path -> true);
    }

    /** Delete selected files and directories from the root directory. */
    static void treeDelete(Path root, Predicate<Path> filter) throws IOException {
      log.info("treeDelete(root:'%s')", root);
      if (Files.notExists(root)) {
        return;
      }
      try (Stream<Path> stream = Files.walk(root)) {
        Stream<Path> selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (Path path : selected.collect(Collectors.toList())) {
          Files.deleteIfExists(path);
        }
      }
    }

    /** Dump directory tree structure. */
    static void treeDump(Path root, Consumer<String> out) {
      if (Files.notExists(root)) {
        out.accept("dumpTree failed: path '" + root + "' does not exist");
        return;
      }
      out.accept(root.toString());
      try (Stream<Path> stream = Files.walk(root).sorted()) {
        for (Path path : stream.collect(Collectors.toList())) {
          String string = root.relativize(path).toString();
          String prefix = string.isEmpty() ? "" : File.separator;
          out.accept("." + prefix + string);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("treeDump failed", e);
      }
    }

    class ModuleInfo {

      static Pattern namePattern = Pattern.compile("module (.+)\\{", Pattern.DOTALL);
      static Pattern requiresPattern = Pattern.compile("requires (.+?);", Pattern.DOTALL);

      static ModuleInfo of(Path path) {
        if (Files.isDirectory(path)) {
          path = path.resolve("module-info.java");
        }
        if (Files.notExists(path)) {
          throw new IllegalArgumentException("expected module-info.java file, but got: " + path);
        }
        try {
          return of(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
        } catch (IOException e) {
          throw new UncheckedIOException("reading '" + path + "' failed", e);
        }
      }

      static ModuleInfo of(List<String> lines) {
        return of(String.join("\n", lines));
      }

      static ModuleInfo of(String source) {
        // extract module name
        Matcher nameMatcher = namePattern.matcher(source);
        if (!nameMatcher.find()) {
          throw new AssertionError("expected java module descriptor unit, but got: " + source);
        }
        String name = nameMatcher.group(1).trim();
        // extract required module names
        Set<String> requires = new TreeSet<>();
        Matcher requiresMatcher = requiresPattern.matcher(source);
        while (requiresMatcher.find()) {
          String[] split = requiresMatcher.group(1).trim().split("\\s+");
          requires.add(split[split.length - 1]);
        }
        return new ModuleInfo(name, requires);
      }

      final String name;
      final Set<String> requires;

      ModuleInfo(String name, Set<String> requires) {
        this.name = name;
        this.requires = Set.of(requires.toArray(new String[requires.size()]));
      }

      String getName() {
        return name;
      }

      Set<String> getRequires() {
        return requires;
      }
    }

    class Resolvable {

      static final List<String> REPOSITORIES =
          List.of(
              "http://repo1.maven.org/maven2",
              "https://oss.sonatype.org/content/repositories/snapshots",
              "https://jcenter.bintray.com",
              "https://jitpack.io");

      static Resolvable of(String source) {
        String[] split = source.split(":");
        if (split.length == 2) {
          return new Resolvable(split[0].trim(), split[1].trim(), Default.RESOLVABLE_VERSION);
        }
        if (split.length == 3) {
          return new Resolvable(split[0].trim(), split[1].trim(), split[2].trim());
        }
        throw new IllegalArgumentException("expected 1 or 2 ':' in source, but got: " + source);
      }

      final String group;
      final String artifact;
      final String version;
      final String classifier;
      final String kind;
      final String file;

      Resolvable(String group, String artifact, String version) {
        this.group = group.replace('.', '/');
        this.artifact = artifact;
        this.version = version;
        this.classifier = "";
        this.kind = "jar";
        // assemble file name
        String versifier = classifier.isEmpty() ? version : version + '-' + classifier;
        this.file = artifact + '-' + versifier + '.' + kind;
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

      Path resolve(Path targetDirectory, List<String> repositories) {
        for (String repository : repositories) {
          try {
            return resolve(targetDirectory, repository);
          } catch (IOException e) {
            log.verbose("resolve(repository:'%s') failed: %s", repository, e);
          }
        }
        throw new Error("could not resolve: " + this);
      }

      Path resolve(Path targetDirectory, String repository) throws IOException {
        URI uri = resolveUri(repository);
        String fileName = getFileName(uri);
        // revert local filename with constant version attribute
        if (isSnapshot()) {
          fileName = this.file;
        }
        return download(uri, targetDirectory, fileName, path -> true);
      }

      Optional<String> resolveFromMetaData(URI xml, UnaryOperator<String> operator) {
        log.verbose("resolving version from " + xml);
        try (InputStream input = xml.toURL().openStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream()) {
          input.transferTo(output);
          String meta = output.toString("UTF-8");
          return Optional.of(operator.apply(meta));
        } catch (Exception exception) {
          return Optional.empty();
        }
      }

      /** Create uri for specified maven coordinates. */
      URI resolveUri(String repository) {
        URI base = URI.create(repository + '/' + group + '/' + artifact + '/');
        if (isSnapshot()) {
          String replacement =
              resolveFromMetaData(
                      base.resolve(version + "/" + "maven-metadata.xml"),
                      meta -> {
                        String timestamp = substring(meta, "<timestamp>", "<");
                        String buildNumber = substring(meta, "<buildNumber>", "<");
                        return timestamp + '-' + buildNumber;
                      })
                  .orElse(version);
          log.verbose("resolved SNAPSHOT as: " + replacement);
          return base.resolve(version + "/" + file.replace("SNAPSHOT", replacement));
        }
        if (isLatest()) {
          String replacement =
              resolveFromMetaData(
                      base.resolve("maven-metadata.xml"), meta -> substring(meta, "<latest>", "<"))
                  .orElse(version);
          log.verbose("resolved LATEST as: " + replacement);
          return base.resolve(replacement + "/" + file.replace("LATEST", replacement));
        }
        if (isRelease()) {
          String replacement =
              resolveFromMetaData(
                      base.resolve("maven-metadata.xml"), meta -> substring(meta, "<release>", "<"))
                  .orElse(version);
          log.verbose("resolved RELEASE as: " + replacement);
          return base.resolve(replacement + "/" + file.replace("RELEASE", replacement));
        }
        return base.resolve(version + "/" + file);
      }

      @Override
      public String toString() {
        return String.format("Resolvable{%s %s %s}", group.replace('/', '.'), artifact, version);
      }
    }
  }

  /** Simple logger facility. */
  class Log {
    enum Level {
      OFF,
      INFO,
      VERBOSE
    }

    Level level = Default.VERBOSE ? Level.VERBOSE : Level.INFO;
    Locale locale = Locale.getDefault();
    Consumer<String> out = System.out::println;

    boolean isEnabled() {
      return level != Level.OFF;
    }

    boolean isDisabled() {
      return level == Level.OFF;
    }

    void log(Level level, String format, Object... args) {
      if (this.level.ordinal() < level.ordinal()) {
        return;
      }
      out.accept(String.format(locale, format, args));
    }

    void info(String format, Object... args) {
      log(Level.INFO, format, args);
    }

    void verbose(String format, Object... args) {
      log(Level.VERBOSE, format, args);
    }
  }

  /**
   * You can use the foundation JDK tools and commands to create and build applications.
   *
   * @see <a
   *     href="https://docs.oracle.com/javase/9/tools/main-tools-create-and-build-applications.htm">Main
   *     Tools to Create and Build Applications</a>
   */
  interface JdkTool {
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
      @Command.Option("--source-path")
      transient List<Path> classSourcePath = List.of();

      /** Generates all debugging information, including local variables. */
      @Command.Option("-g")
      boolean generateAllDebuggingInformation = false;

      /** Output source locations where deprecated APIs are used. */
      boolean deprecation = true;

      /** The destination directory for class files. */
      @Command.Option("-d")
      Path destination = null;

      /** Specify character encoding used by source files. */
      Charset encoding = StandardCharsets.UTF_8;

      /** Terminate compilation if warnings occur. */
      @Command.Option("-Werror")
      boolean failOnWarnings = true;

      /** Overrides or augments a module with classes and resources in JAR files or directories. */
      Map<String, List<Path>> patchModule = Map.of();

      /** Specify where to find application modules. */
      List<Path> modulePath = List.of();

      /** Where to find input source files for multiple modules. */
      List<Path> moduleSourcePath = List.of();

      /** Compiles only the specified module and checks timestamps. */
      @Command.Option("--module")
      String module = null;

      /** Generate metadata for reflection on method parameters. */
      boolean parameters = true;

      /** Output messages about what the compiler is doing. */
      boolean verbose = false;

      /** Create javac command with options and source files added. */
      @Override
      public Command toCommand() {
        Command command = JdkTool.super.toCommand();
        command.mark(10);
        command.addAll(classSourcePath, Basics::isJavaFile);
        if (module == null) {
          command.addAll(moduleSourcePath, Basics::isJavaFile);
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

      /** Add modules. */
      List<String> addModules = List.of();

      /** Initial module to resolve and the name of the main class to execute. */
      @Command.Option("--module")
      String module = null;

      /** Arguments passed to the main entry-point. */
      transient List<Object> args = List.of();

      /** Create java command with options and source files added. */
      @Override
      public Command toCommand() {
        Command command = JdkTool.super.toCommand();
        command.setExecutableSupportsArgumentFile(true);
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
      /** Shuts off messages so that only the warnings and errors appear. */
      boolean quiet = true;
    }

    /**
     * You can use the jar command to create an archive for classes and resources, and manipulate or
     * restore individual classes or resources from an archive.
     *
     * @see <a href="https://docs.oracle.com/javase/9/tools/jar.htm">jar</a>
     */
    class Jar implements JdkTool {
      /** Specify the operation mode for the jar command. */
      @Command.Option("")
      String mode = "--create";

      /** Specifies the archive file name. */
      @Command.Option("--file")
      Path file = Paths.get("out.jar");

      /** Specifies the application entry point for stand-alone applications. */
      String mainClass = null;

      /** Specifies the module version, when creating a modular JAR file. */
      String moduleVersion = null;

      /** Stores without using ZIP compression. */
      boolean noCompress = false;

      /** Sends or prints verbose output to standard output. */
      @Command.Option("--verbose")
      boolean verbose = false;

      /** Changes to the specified directory and includes the files at the end of the command. */
      @Command.Option("-C")
      Path path = null;

      @Override
      public Command toCommand() {
        Command command = JdkTool.super.toCommand();
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

      /**
       * Restricts analysis to APIs, like deps from the signature of public and protected members.
       */
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
      @Command.Option("--output")
      Path output = null;
    }

    /** Name of this tool, like {@code javac} or {@code jar}. */
    default String name() {
      return getClass().getSimpleName().toLowerCase();
    }

    /**
     * Execute this tool with all options and arguments applied.
     *
     * @throws AssertionError if the execution result is not zero
     */
    default void run() {
      toCommand().run();
    }

    /** Create command instance based on this tool's options. */
    default Command toCommand() {
      return new Command(name()).addAllOptions(this);
    }
  }
}
