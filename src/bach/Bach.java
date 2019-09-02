// THIS FILE WAS GENERATED ON 2019-09-02T03:42:29.499222500Z
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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;

public class Bach {

  public static String VERSION = "2-ea";

  /**
   * Create new Bach instance with default configuration.
   *
   * @return new default Bach instance
   */
  public static Bach of() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return new Bach(out, err, Configuration.of());
  }

  /**
   * Main entry-point.
   *
   * @param args List of API method or tool names.
   */
  public static void main(String... args) {
    var bach = Bach.of();
    bach.main(args.length == 0 ? List.of("build") : List.of(args));
  }

  /** Text-output writer. */
  final PrintWriter out, err;

  /** Configuration. */
  final Configuration configuration;

  public Bach(PrintWriter out, PrintWriter err, Configuration configuration) {
    this.out = Util.requireNonNull(out, "out");
    this.err = Util.requireNonNull(err, "err");
    this.configuration = Util.requireNonNull(configuration, "configuration");
  }

  void main(List<String> args) {
    var arguments = new ArrayDeque<>(args);
    while (!arguments.isEmpty()) {
      var argument = arguments.pop();
      try {
        switch (argument) {
          case "build":
            build();
            continue;
          case "validate":
            validate();
            continue;
        }
        // Try Bach API method w/o parameter -- single argument is consumed
        var method = Util.findApiMethod(getClass(), argument);
        if (method.isPresent()) {
          method.get().invoke(this);
          continue;
        }
        // Try provided tool -- all remaining arguments are consumed
        var tool = ToolProvider.findFirst(argument);
        if (tool.isPresent()) {
          var code = tool.get().run(out, err, arguments.toArray(String[]::new));
          if (code != 0) {
            throw new RuntimeException("Tool " + argument + " returned: " + code);
          }
          return;
        }
      } catch (ReflectiveOperationException e) {
        throw new Error("Reflective operation failed for: " + argument, e);
      }
      throw new IllegalArgumentException("Unsupported argument: " + argument);
    }
  }

  String getBanner() {
    var module = getClass().getModule();
    try (var stream = module.getResourceAsStream("de/sormuras/bach/banner.txt")) {
      if (stream == null) {
        return String.format("Bach.java %s (member of %s)", VERSION, module);
      }
      var lines = new BufferedReader(new InputStreamReader(stream)).lines();
      var banner = lines.collect(Collectors.joining(System.lineSeparator()));
      return banner + " " + VERSION;
    } catch (IOException e) {
      throw new UncheckedIOException("loading banner resource failed", e);
    }
  }

  public void help() {
    out.println("F1! F1! F1!");
    out.println("Method API");
    Arrays.stream(getClass().getMethods())
        .filter(Util::isApiMethod)
        .map(m -> "  " + m.getName() + " (" + m.getDeclaringClass().getSimpleName() + ")")
        .sorted()
        .forEach(out::println);
    out.println("Provided tools");
    ServiceLoader.load(ToolProvider.class).stream()
        .map(provider -> "  " + provider.get().name())
        .sorted()
        .forEach(out::println);
  }

  public void build() {
    info();
    validate();
    resolve();
  }

  public void clean() {
    Util.treeDelete(configuration.getWorkspaceDirectory());
  }

  public void info() {
    out.printf("Bach (%s)%n", VERSION);
    Configuration.toStrings(configuration).forEach(line -> out.println("  " + line));
  }

  public void validate() {
    Configuration.validate(configuration);
  }

  public void resolve() {
    Resolver.resolve(this);
  }

  public void version() {
    out.println(getBanner());
  }

  public interface Configuration {

    default Path getHomeDirectory() {
      return Path.of("");
    }

    default Path getWorkspaceDirectory() {
      return Path.of("bin");
    }

    default Path getLibraryDirectory() {
      return getLibraryPaths().get(0);
    }

    default List<Path> getLibraryPaths() {
      return List.of(Path.of("lib"));
    }

    default List<Path> getSourceDirectories() {
      return List.of(Path.of("src"));
    }

    default Path resolve(Path path, String name) {
      return Configuration.resolve(getHomeDirectory(), path, name);
    }

    default List<Path> resolve(List<Path> paths, String name) {
      return Configuration.resolve(getHomeDirectory(), paths, name);
    }

    static Configuration of() {
      return of(Path.of(""));
    }

    static Configuration of(Path home) {
      validateDirectory(Util.requireNonNull(home, "home directory"));
      var ccc = compileCustomConfiguration(home);
      return new DefaultConfiguration(
          home,
          resolve(home, ccc.getWorkspaceDirectory(), "workspace directory"),
          resolve(home, ccc.getLibraryPaths(), "library paths"),
          resolve(home, ccc.getSourceDirectories(), "source directories"));
    }

    static Path resolve(Path home, Path path, String name) {
      return Util.requireNonNull(path, name).isAbsolute() ? path : home.resolve(path);
    }

    static List<Path> resolve(Path home, List<Path> paths, String name) {
      return List.of(
          Util.requireNonNull(paths, name).stream()
              .map(path -> resolve(home, path, "element of " + name))
              .toArray(Path[]::new));
    }

    private static Configuration compileCustomConfiguration(Path home) {
      class ConfigurationInvocationHandler implements Configuration, InvocationHandler {

        private final Object that;

        private ConfigurationInvocationHandler(Object that) {
          this.that = that;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          try {
            return that.getClass().getMethod(method.getName()).invoke(that);
          } catch (NoSuchMethodException e) {
            return this.getClass().getMethod(method.getName()).invoke(this);
          }
        }
      }

      var dot = home.resolve(".bach");
      if (Files.isDirectory(dot)) {
        var bin = Path.of("bin/.bach");
        var name = "Configuration";
        var configurationJava = dot.resolve(name + ".java");
        if (Files.exists(configurationJava)) {
          var javac = ToolProvider.findFirst("javac").orElseThrow();
          javac.run(System.out, System.err, "-d", bin.toString(), configurationJava.toString());
        }
        try {
          var parent = Configuration.class.getClassLoader();
          var loader = URLClassLoader.newInstance(new URL[] {bin.toUri().toURL()}, parent);
          var configuration = loader.loadClass(name).getConstructor().newInstance();
          if (configuration instanceof Configuration) {
            return (Configuration) configuration;
          }
          var interfaces = new Class[] {Configuration.class};
          var handler = new ConfigurationInvocationHandler(configuration);
          return (Configuration) Proxy.newProxyInstance(loader, interfaces, handler);
        } catch (ClassNotFoundException e) {
          // ignore "missing" custom configuration class
        } catch (Exception e) {
          throw new Error("Loading custom configuration failed: " + configurationJava.toUri(), e);
        }
      }
      return new Configuration() {};
    }

    class DefaultConfiguration implements Configuration {

      private final Path homeDirectory;
      private final Path workspaceDirectory;
      private final List<Path> libraryPaths;
      private final List<Path> sourceDirectories;

      private DefaultConfiguration(
          Path homeDirectory,
          Path workspaceDirectory,
          List<Path> libraryPaths,
          List<Path> sourceDirectories) {
        this.homeDirectory = homeDirectory;
        this.workspaceDirectory = workspaceDirectory;
        this.libraryPaths = Util.requireNonEmpty(libraryPaths, "library paths");
        this.sourceDirectories = Util.requireNonEmpty(sourceDirectories, "source directories");
      }

      @Override
      public Path getHomeDirectory() {
        return homeDirectory;
      }

      @Override
      public Path getWorkspaceDirectory() {
        return workspaceDirectory;
      }

      @Override
      public List<Path> getLibraryPaths() {
        return libraryPaths;
      }

      @Override
      public List<Path> getSourceDirectories() {
        return sourceDirectories;
      }

      @Override
      public String toString() {
        return "Configuration [" + String.join(", ", toStrings(this)) + "]";
      }
    }

    class ValidationError extends AssertionError {
      private ValidationError(String expected, Object hint) {
        super(String.format("expected that %s: %s", expected, hint));
      }
    }

    static List<String> toStrings(Configuration configuration) {
      var home = configuration.getHomeDirectory();
      return List.of(
          String.format("home = '%s' -> %s", home, home.toUri()),
          String.format("workspace = '%s'", configuration.getWorkspaceDirectory()),
          String.format("library paths = %s", configuration.getLibraryPaths()),
          String.format("source directories = %s", configuration.getSourceDirectories()));
    }

    static void validate(Configuration configuration) {
      var home = configuration.getHomeDirectory();
      validateDirectory(home);
      if (Util.list(home, Files::isDirectory).size() == 0)
        throw new ValidationError("home contains a directory", home.toUri());
      var work = configuration.getWorkspaceDirectory();
      if (Files.exists(work)) {
        validateDirectory(work);
        if (!work.toFile().canWrite()) throw new ValidationError("bin is writable: %s", work.toUri());
      } else {
        var parentOfBin = work.toAbsolutePath().getParent();
        if (parentOfBin != null && !parentOfBin.toFile().canWrite())
          throw new ValidationError("parent of work is writable", parentOfBin.toUri());
      }
      validateDirectoryIfExists(configuration.getLibraryDirectory());
      configuration.getSourceDirectories().forEach(Configuration::validateDirectory);
    }

    static void validateDirectoryIfExists(Path path) {
      if (Files.exists(path)) validateDirectory(path);
    }

    static void validateDirectory(Path path) {
      if (!Files.isDirectory(path)) throw new ValidationError("path is a directory", path.toUri());
    }
  }

  /** Command-line program argument list builder. */
  public static class Command {

    final String name;
    final List<String> list = new ArrayList<>();

    /** Initialize Command instance with zero or more arguments. */
    Command(String name, Object... args) {
      this.name = name;
      addEach(args);
    }

    /** Add single argument by invoking {@link Object#toString()} on the given argument. */
    Command add(Object argument) {
      list.add(argument.toString());
      return this;
    }

    /** Add two arguments by invoking {@link #add(Object)} for the key and value elements. */
    Command add(Object key, Object value) {
      return add(key).add(value);
    }

    /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths) {
      return add(key, paths, UnaryOperator.identity());
    }

    /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths, UnaryOperator<String> operator) {
      var stream = paths.stream().filter(Files::exists).map(Object::toString);
      var value = stream.collect(Collectors.joining(File.pathSeparator));
      if (value.isEmpty()) {
        return this;
      }
      return add(key, operator.apply(value));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addEach(Object... arguments) {
      return addEach(List.of(arguments));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addEach(Iterable<?> arguments) {
      arguments.forEach(this::add);
      return this;
    }

    /** Add a single argument iff the conditions is {@code true}. */
    Command addIff(boolean condition, Object argument) {
      return condition ? add(argument) : this;
    }

    /** Add two arguments iff the conditions is {@code true}. */
    Command addIff(boolean condition, Object key, Object value) {
      return condition ? add(key, value) : this;
    }

    /** Add two arguments iff the given optional value is present. */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Command addIff(Object key, Optional<?> optionalValue) {
      return optionalValue.isPresent() ? add(key, optionalValue.get()) : this;
    }

    /** Let the consumer visit, usually modify, this instance iff the conditions is {@code true}. */
    Command addIff(boolean condition, Consumer<Command> visitor) {
      if (condition) {
        visitor.accept(this);
      }
      return this;
    }

    @Override
    public String toString() {
      var args = list.isEmpty() ? "<empty>" : "'" + String.join("', '", list) + "'";
      return "Command{name='" + name + "', list=[" + args + "]}";
    }

    /** Returns an array of {@link String} containing all of the collected arguments. */
    String[] toStringArray() {
      return list.toArray(String[]::new);
    }
  }

  /** Resolves required modules. */
  static class Resolver {

    private static final Pattern MODULE_NAME_PATTERN = Pattern.compile("(?:module)\\s+([\\w.]+)");
    private static final Pattern MODULE_REQUIRES_PATTERN =
        Pattern.compile(
            "(?:requires)" // key word
                + "(?:\\s+[\\w.]+)?" // optional modifiers
                + "\\s+([\\w.]+)" // module name
                + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                + ";"); // end marker

    static void resolve(Bach bach) {
      var entries = bach.configuration.getLibraryPaths().toArray(Path[]::new);
      var library = of(ModuleFinder.of(entries));
      bach.out.println("Library of -> " + bach.configuration.getLibraryPaths());
      bach.out.println("  modules  -> " + library.modules);
      bach.out.println("  requires -> " + library.requires);

      var sources = of(bach.configuration.getSourceDirectories());
      bach.out.println("Sources of -> " + bach.configuration.getSourceDirectories());
      bach.out.println("  modules  -> " + sources.modules);
      bach.out.println("  requires -> " + sources.requires);

      var systems = of(ModuleFinder.ofSystem());

      var missing = new TreeMap<String, Set<Version>>();
      missing.putAll(sources.requires);
      missing.putAll(library.requires);
      sources.getDeclaredModules().forEach(missing::remove);
      library.getDeclaredModules().forEach(missing::remove);
      systems.getDeclaredModules().forEach(missing::remove);
      if (missing.isEmpty()) {
        return;
      }

      var transfer = new Transfer(bach.out, bach.err);
      var worker = new Worker(transfer, bach.configuration.getLibraryDirectory());
      do {
        bach.out.println("Loading missing modules: " + missing);
        var items = new ArrayList<Transfer.Item>();
        for (var entry : missing.entrySet()) {
          var module = entry.getKey();
          var versions = entry.getValue();
          items.add(worker.toTransferItem(module, versions));
        }
        var lib = bach.configuration.getLibraryDirectory();
        transfer.getFiles(lib, items);
        library = of(ModuleFinder.of(entries));
        missing = new TreeMap<>(library.requires);
        library.getDeclaredModules().forEach(missing::remove);
        systems.getDeclaredModules().forEach(missing::remove);
      } while (!missing.isEmpty());
    }

    /** Command-line argument factory. */
    static Resolver of(Collection<String> declaredModules, Iterable<String> requires) {
      var map = new TreeMap<String, Set<Version>>();
      for (var string : requires) {
        var versionMarkerIndex = string.indexOf('@');
        var any = versionMarkerIndex == -1;
        var module = any ? string : string.substring(0, versionMarkerIndex);
        var version = any ? null : Version.parse(string.substring(versionMarkerIndex + 1));
        map.merge(module, any ? Set.of() : Set.of(version), Util::concat);
      }
      return new Resolver(new TreeSet<>(declaredModules), map);
    }

    static Resolver of(ModuleFinder finder) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeMap<String, Set<Version>>();
      finder.findAll().stream()
          .map(ModuleReference::descriptor)
          .peek(descriptor -> declaredModules.add(descriptor.name()))
          .map(ModuleDescriptor::requires)
          .flatMap(Set::stream)
          .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.MANDATED))
          .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC))
          .distinct()
          .forEach(
              requires ->
                  requiredModules.merge(
                      requires.name(),
                      requires.compiledVersion().map(Set::of).orElse(Set.of()),
                      Util::concat));
      return new Resolver(declaredModules, requiredModules);
    }

    static Resolver of(String... sources) {
      var declaredModules = new TreeSet<String>();
      var map = new TreeMap<String, Set<Version>>();
      for (var source : sources) {
        var nameMatcher = MODULE_NAME_PATTERN.matcher(source);
        if (!nameMatcher.find()) {
          throw new IllegalArgumentException("Expected module-info.java source, but got: " + source);
        }
        declaredModules.add(nameMatcher.group(1).trim());
        var requiresMatcher = MODULE_REQUIRES_PATTERN.matcher(source);
        while (requiresMatcher.find()) {
          var name = requiresMatcher.group(1);
          var version = requiresMatcher.group(2);
          map.merge(name, version == null ? Set.of() : Set.of(Version.parse(version)), Util::concat);
        }
      }
      return new Resolver(declaredModules, map);
    }

    static Resolver of(Collection<Path> sourcePaths) {
      var sources = new ArrayList<String>();
      for (var sourcePath : sourcePaths) {
        try (var stream = Files.find(sourcePath, 9, (p, __) -> Util.isModuleInfo(p))) {
          for (var moduleInfo : stream.collect(Collectors.toSet())) {
            sources.add(Files.readString(moduleInfo));
          }
        } catch (IOException e) {
          throw new UncheckedIOException("find or read failed: " + sourcePath, e);
        }
      }
      return of(sources.toArray(new String[0]));
    }

    private final Set<String> modules;
    private final Map<String, Set<Version>> requires;

    Resolver(Set<String> modules, Map<String, Set<Version>> requires) {
      this.modules = modules;
      this.requires = requires;
    }

    Set<String> getDeclaredModules() {
      return modules;
    }

    Set<String> getRequiredModules() {
      return requires.keySet();
    }

    Optional<Version> getRequiredVersion(String requiredModule) {
      var versions = requires.get(requiredModule);
      if (versions == null) {
        throw new NoSuchElementException("Module " + requiredModule + " is not mapped");
      }
      if (versions.size() > 1) {
        throw new IllegalStateException("Multiple versions: " + requiredModule + " -> " + versions);
      }
      return versions.stream().findFirst();
    }

    static class Worker {

      class Lookup {

        final String name;
        final Properties properties;
        final Set<Pattern> patterns;

        Lookup(Transfer transfer, Path lib, String name) {
          this.name = name;
          var uri = "https://github.com/sormuras/modules/raw/master/" + name;
          var modules = Path.of(System.getProperty("user.home")).resolve(".bach/modules");
          try {
            Files.createDirectories(modules);
          } catch (IOException e) {
            throw new UncheckedIOException("Creating directories failed: " + modules, e);
          }
          var defaultModules = transfer.getFile(URI.create(uri), modules.resolve(name));
          var defaults = Util.load(new Properties(), defaultModules);
          this.properties = Util.load(new Properties(defaults), lib.resolve(name));
          this.patterns =
              properties.keySet().stream()
                  .map(Object::toString)
                  .filter(key -> !SourceVersion.isName(key))
                  .map(Pattern::compile)
                  .collect(Collectors.toSet());
        }

        String get(String key) {
          var value = properties.getProperty(key);
          if (value != null) {
            return value;
          }
          for (var pattern : patterns) {
            if (pattern.matcher(key).matches()) {
              return properties.getProperty(pattern.pattern());
            }
          }
          throw new IllegalStateException("No lookup value mapped for: " + key);
        }

        @Override
        public String toString() {
          var size = properties.size();
          var names = properties.stringPropertyNames().size();
          return String.format(
              "module properties {name: %s, size: %d, names: %d}", name, size, names);
        }
      }

      final Properties moduleUri;
      final Lookup moduleMaven, moduleVersion;

      Worker(Transfer transfer, Path lib) {
        this.moduleUri = Util.load(new Properties(), lib.resolve("module-uri.properties"));
        this.moduleMaven = new Lookup(transfer, lib, "module-maven.properties");
        this.moduleVersion = new Lookup(transfer, lib, "module-version.properties");
      }

      private Transfer.Item toTransferItem(String module, Set<Version> set) {
        var userProvidedUri = moduleUri.getProperty(module);
        if (userProvidedUri != null) {
          var uri = URI.create(userProvidedUri);
          var file = Util.findFileName(uri);
          var version = Util.findVersion(file.orElse(""));
          return Transfer.Item.of(uri, module + version.map(v -> '-' + v).orElse("") + ".jar");
        }
        var maven = moduleMaven.get(module).split(":");
        var group = maven[0];
        var artifact = maven[1];
        var version = Util.singleton(set).map(Object::toString).orElse(moduleVersion.get(module));
        return Transfer.Item.of(toUri(group, artifact, version), module + '-' + version + ".jar");
      }

      private URI toUri(String group, String artifact, String version) {
        var host = "https://repo1.maven.org/maven2";
        var file = artifact + '-' + version + ".jar";
        var uri = String.join("/", host, group.replace('.', '/'), artifact, version, file);
        return URI.create(uri);
      }
    }
  }

  /** File transfer. */
  static class Transfer {

    static class Item {

      static Item of(URI uri, String file) {
        return new Item(uri, file);
      }

      private final URI uri;
      private final String file;

      private Item(URI uri, String file) {
        this.uri = uri;
        this.file = file;
      }
    }

    private final PrintWriter out, err;
    private final HttpClient client;

    Transfer(PrintWriter out, PrintWriter err) {
      this(out, err, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    private Transfer(PrintWriter out, PrintWriter err, HttpClient client) {
      this.out = out;
      this.err = err;
      this.client = client;
    }

    void getFiles(Path path, Collection<Item> items) {
      Util.treeCreate(path);
      items.stream()
          .parallel()
          .map(item -> getFile(item.uri, path.resolve(item.file)))
          .collect(Collectors.toSet());
    }

    Path getFile(URI uri, Path path) {
      var request = HttpRequest.newBuilder(uri).GET();
      if (Files.exists(path)) {
        try {
          var etagBytes = (byte[]) Files.getAttribute(path, "user:etag");
          var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
          request.setHeader("If-None-Match", etag);
        } catch (Exception e) {
          err.println("Couldn't get 'user:etag' file attribute: " + e);
        }
      }
      try {
        var handler = HttpResponse.BodyHandlers.ofFile(path);
        var response = client.send(request.build(), handler);
        if (response.statusCode() == 200) {
          var etagHeader = response.headers().firstValue("etag");
          if (etagHeader.isPresent()) {
            try {
              var etag = etagHeader.get();
              Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
            } catch (Exception e) {
              err.println("Couldn't set 'user:etag' file attribute: " + e);
            }
          }
          var lastModifiedHeader = response.headers().firstValue("last-modified");
          if (lastModifiedHeader.isPresent()) {
            try {
              var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
              var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
              var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
              Files.setLastModifiedTime(path, fileTime);
            } catch (Exception e) {
              err.println("Couldn't set last modified file attribute: " + e);
            }
          }
          synchronized (out) {
            out.println(path + " <- " + uri);
          }
        }
      } catch (IOException | InterruptedException e) {
        err.println("Failed to load: " + uri + " -> " + e);
        e.printStackTrace(err);
      }
      return path;
    }
  }

  /** Static helpers. */
  public static class Util {

    static <E extends Comparable<E>> Set<E> concat(Set<E> one, Set<E> two) {
      return Stream.concat(one.stream(), two.stream()).collect(Collectors.toCollection(TreeSet::new));
    }

    static Optional<Method> findApiMethod(Class<?> container, String name) {
      try {
        var method = container.getMethod(name);
        return isApiMethod(method) ? Optional.of(method) : Optional.empty();
      } catch (NoSuchMethodException e) {
        return Optional.empty();
      }
    }

    static boolean isApiMethod(Method method) {
      if (method.getDeclaringClass().equals(Object.class)) return false;
      if (Modifier.isStatic(method.getModifiers())) return false;
      return method.getParameterCount() == 0;
    }

    static boolean isModuleInfo(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
    }

    static List<Path> list(Path directory) {
      return list(directory, __ -> true);
    }

    static List<Path> list(Path directory, Predicate<Path> filter) {
      try {
        return Files.list(directory).filter(filter).sorted().collect(Collectors.toList());
      } catch (IOException e) {
        throw new UncheckedIOException("list directory failed: " + directory, e);
      }
    }

    static Properties load(Properties properties, Path path) {
      if (Files.isRegularFile(path)) {
        try (var reader = Files.newBufferedReader(path)) {
          properties.load(reader);
        } catch (IOException e) {
          throw new UncheckedIOException("Reading properties failed: " + path, e);
        }
      }
      return properties;
    }

    /** Extract last path element from the supplied uri. */
    static Optional<String> findFileName(URI uri) {
      var path = uri.getPath();
      return path == null ? Optional.empty() : Optional.of(path.substring(path.lastIndexOf('/') + 1));
    }

    static Optional<String> findVersion(String jarFileName) {
      if (!jarFileName.endsWith(".jar")) return Optional.empty();
      var name = jarFileName.substring(0, jarFileName.length() - 4);
      var matcher = Pattern.compile("-(\\d+(\\.|$))").matcher(name);
      return (matcher.find()) ? Optional.of(name.substring(matcher.start() + 1)) : Optional.empty();
    }

    static <C extends Collection<?>> C requireNonEmpty(C collection, String name) {
      if (requireNonNull(collection, name + " must not be null").isEmpty()) {
        throw new IllegalArgumentException(name + " must not be empty");
      }
      return collection;
    }

    static <T> T requireNonNull(T object, String name) {
      return Objects.requireNonNull(object, name + " must not be null");
    }

    static <T> Optional<T> singleton(Collection<T> collection) {
      if (collection.isEmpty()) {
        return Optional.empty();
      }
      if (collection.size() != 1) {
        throw new IllegalStateException("Too many elements: " + collection);
      }
      return Optional.of(collection.iterator().next());
    }
    /** @see Files#createDirectories(Path, FileAttribute[])  */
    static Path treeCreate(Path path) {
      try {
        return Files.createDirectories(path);
      } catch (IOException e) {
        throw new UncheckedIOException("create directories failed: " + path, e);
      }
    }

    /** Delete all files and directories from and including the root directory. */
    static void treeDelete(Path root) {
      treeDelete(root, __ -> true);
    }

    /** Delete selected files and directories from and including the root directory. */
    static void treeDelete(Path root, Predicate<Path> filter) {
      if (filter.test(root)) { // trivial case: delete existing empty directory or single file
        try {
          Files.deleteIfExists(root);
          return;
        } catch (IOException ignored) {
          // fall-through
        }
      }
      try (var stream = Files.walk(root)) { // default case: walk the tree...
        var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (var path : selected.collect(Collectors.toList())) {
          Files.deleteIfExists(path);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("tree delete failed: " + root, e);
      }
    }
  }
}
