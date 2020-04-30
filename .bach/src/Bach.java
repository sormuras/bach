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
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class Bach {
  public static final Version VERSION = Version.parse("11.0-ea");
  public static void main(String... args) {
    Main.main(args);
  }
  private final Supplier<HttpClient> httpClient;
  public Bach() {
    this(HttpClient.newBuilder()::build);
  }
  public Bach(Supplier<HttpClient> httpClient) {
    this.httpClient = Functions.memoize(httpClient);
  }
  public HttpClient getHttpClient() {
    return httpClient.get();
  }
  public String toString() {
    return "Bach.java " + VERSION;
  }
  static class Main {
    public static void main(String... args) {
      System.out.println("Bach.java " + Bach.VERSION);
    }
  }
  public static class Functions {
    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
      Objects.requireNonNull(supplier, "supplier");
      class CachingSupplier implements Supplier<T> {
        Supplier<T> delegate = this::initialize;
        boolean initialized = false;
        public T get() {
          return delegate.get();
        }
        private synchronized T initialize() {
          if (initialized) return delegate.get();
          T value = supplier.get();
          delegate = () -> value;
          initialized = true;
          return value;
        }
      }
      return new CachingSupplier();
    }
    private Functions() {}
  }
  public static class Modules {
    interface Patterns {
      Pattern NAME =
          Pattern.compile(
              "(?:module)" // key word
                  + "\\s+([\\w.]+)" // module name
                  + "(?:\\s*/\\*.*\\*/\\s*)?" // optional multi-line comment
                  + "\\s*\\{"); // end marker
      Pattern REQUIRES =
          Pattern.compile(
              "(?:requires)" // key word
                  + "(?:\\s+[\\w.]+)?" // optional modifiers
                  + "\\s+([\\w.]+)" // module name
                  + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                  + "\\s*;"); // end marker
    }
    public static Optional<String> findMainClass(Path info, String module) {
      var main = Path.of(module.replace('.', '/'), "Main.java");
      var exists = Files.isRegularFile(info.resolveSibling(main));
      return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
    }
    public static Optional<String> findMainModule(Stream<ModuleDescriptor> descriptors) {
      var mains = descriptors.filter(d -> d.mainClass().isPresent()).collect(Collectors.toList());
      return mains.size() == 1 ? Optional.of(mains.get(0).name()) : Optional.empty();
    }
    public static ModuleDescriptor describe(Path info) {
      try {
        var module = describe(Files.readString(info));
        var temporary = module.build();
        findMainClass(info, temporary.name()).ifPresent(module::mainClass);
        return module.build();
      } catch (IOException e) {
        throw new UncheckedIOException("Describe failed", e);
      }
    }
    public static ModuleDescriptor.Builder describe(String source) {
      var nameMatcher = Patterns.NAME.matcher(source);
      if (!nameMatcher.find())
        throw new IllegalArgumentException("Expected Java module source unit, but got: " + source);
      var name = nameMatcher.group(1).trim();
      var builder = ModuleDescriptor.newModule(name);
      var requiresMatcher = Patterns.REQUIRES.matcher(source);
      while (requiresMatcher.find()) {
        var requiredName = requiresMatcher.group(1);
        Optional.ofNullable(requiresMatcher.group(2))
            .ifPresentOrElse(
                version -> builder.requires(Set.of(), requiredName, Version.parse(version)),
                () -> builder.requires(requiredName));
      }
      return builder;
    }
    public static Set<String> declared(ModuleFinder finder) {
      return declared(finder.findAll().stream().map(ModuleReference::descriptor));
    }
    public static Set<String> declared(Stream<ModuleDescriptor> descriptors) {
      return descriptors.map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
    }
    public static Set<String> required(ModuleFinder finder) {
      return required(finder.findAll().stream().map(ModuleReference::descriptor));
    }
    public static Set<String> required(Stream<ModuleDescriptor> descriptors) {
      return descriptors
          .map(ModuleDescriptor::requires)
          .flatMap(Set::stream)
          .filter(requires -> !requires.modifiers().contains(Requires.Modifier.MANDATED))
          .filter(requires -> !requires.modifiers().contains(Requires.Modifier.SYNTHETIC))
          .map(Requires::name)
          .collect(Collectors.toCollection(TreeSet::new));
    }
    private Modules() {}
  }
  public static class ModulesResolver {
    private final Path[] paths;
    private final Set<String> declared;
    private final Consumer<Set<String>> transporter;
    private final Set<String> system;
    public ModulesResolver(Path[] paths, Set<String> declared, Consumer<Set<String>> transporter) {
      this.paths = paths;
      this.declared = new TreeSet<>(declared);
      this.transporter = transporter;
      this.system = Modules.declared(ModuleFinder.ofSystem());
    }
    public void resolve(Set<String> required) {
      resolveModules(required);
      resolveLibraryModules();
    }
    public void resolveModules(Set<String> required) {
      var missing = missing(required);
      if (missing.isEmpty()) return;
      transporter.accept(missing);
      var unresolved = missing(required);
      if (unresolved.isEmpty()) return;
      throw new IllegalStateException("Unresolved modules: " + unresolved);
    }
    public void resolveLibraryModules() {
      do {
        var missing = missing(Modules.required(ModuleFinder.of(paths)));
        if (missing.isEmpty()) return;
        resolveModules(missing);
      } while (true);
    }
    Set<String> missing(Set<String> required) {
      var missing = new TreeSet<>(required);
      missing.removeAll(declared);
      if (required.isEmpty()) return Set.of();
      missing.removeAll(system);
      if (required.isEmpty()) return Set.of();
      var library = Modules.declared(ModuleFinder.of(paths));
      missing.removeAll(library);
      return missing;
    }
  }
  public static class Paths {
    public static boolean isEmpty(Path path) {
      try {
        if (Files.isRegularFile(path)) return Files.size(path) == 0L;
        try (var stream = Files.list(path)) {
          return stream.findAny().isEmpty();
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    public static void delete(Path directory, Predicate<Path> filter) throws IOException {
      try {
        Files.deleteIfExists(directory);
        return;
      } catch (DirectoryNotEmptyException __) {
      }
      try (var stream = Files.walk(directory)) {
        var paths = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
      }
    }
    public static Path assertFileAttributes(Path file, Map<String, String> attributes) {
      if (attributes.isEmpty()) return file;
      var map = new HashMap<>(attributes);
      var size = map.remove("size");
      if (size != null) {
        var expectedSize = Long.parseLong(size);
        try {
          long fileSize = Files.size(file);
          if (expectedSize != fileSize) {
            var details = "expected " + expectedSize + " bytes\n\tactual " + fileSize + " bytes";
            throw new AssertionError("File size mismatch: " + file + "\n\t" + details);
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      map.remove("module");
      map.remove("version");
      if (map.isEmpty()) return file;
      try {
        var bytes = Files.readAllBytes(file);
        for (var expectedDigest : map.entrySet()) {
          var actual = digest(expectedDigest.getKey(), bytes);
          var expected = expectedDigest.getValue();
          if (expected.equalsIgnoreCase(actual)) continue;
          var details = "expected " + expected + ", but got " + actual;
          throw new AssertionError("File digest mismatch: " + file + "\n\t" + details);
        }
      } catch (Exception e) {
        throw new AssertionError("File digest check failed: " + file, e);
      }
      return file;
    }
    public static String digest(String algorithm, byte[] bytes) throws Exception {
      var md = MessageDigest.getInstance(algorithm);
      md.update(bytes);
      return Strings.hex(md.digest());
    }
    private Paths() {}
  }
  public static class Resources {
    private final HttpClient client;
    public Resources(HttpClient client) {
      this.client = client;
    }
    public HttpResponse<Void> head(URI uri, int timeout) throws Exception {
      var nobody = HttpRequest.BodyPublishers.noBody();
      var duration = Duration.ofSeconds(timeout);
      var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).timeout(duration).build();
      return client.send(request, BodyHandlers.discarding());
    }
    public Path copy(URI uri, Path file) throws Exception {
      return copy(uri, file, StandardCopyOption.COPY_ATTRIBUTES);
    }
    public Path copy(URI uri, Path file, CopyOption... options) throws Exception {
      var request = HttpRequest.newBuilder(uri).GET();
      if (Files.exists(file) && Files.getFileStore(file).supportsFileAttributeView("user")) {
        var etagBytes = (byte[]) Files.getAttribute(file, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        request.setHeader("If-None-Match", etag);
      }
      var directory = file.getParent();
      if (directory != null) Files.createDirectories(directory);
      var handler = BodyHandlers.ofFile(file);
      var response = client.send(request.build(), handler);
      if (response.statusCode() == 200) {
        if (Set.of(options).contains(StandardCopyOption.COPY_ATTRIBUTES)) {
          var etagHeader = response.headers().firstValue("etag");
          if (etagHeader.isPresent() && Files.getFileStore(file).supportsFileAttributeView("user")) {
            var etag = StandardCharsets.UTF_8.encode(etagHeader.get());
            Files.setAttribute(file, "user:etag", etag);
          }
          var lastModifiedHeader = response.headers().firstValue("last-modified");
          if (lastModifiedHeader.isPresent()) {
            @SuppressWarnings("SpellCheckingInspection")
            var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            var current = System.currentTimeMillis();
            var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
            var fileTime = FileTime.fromMillis(millis == 0 ? current : millis);
            Files.setLastModifiedTime(file, fileTime);
          }
        }
        return file;
      }
      if (response.statusCode() == 304 /*Not Modified*/) return file;
      Files.deleteIfExists(file);
      throw new IllegalStateException("copy " + uri + " failed: response=" + response);
    }
    public String read(URI uri) throws Exception {
      var request = HttpRequest.newBuilder(uri).GET();
      return client.send(request.build(), BodyHandlers.ofString()).body();
    }
  }
  public static class Strings {
    public static List<String> list(String tool, String... args) {
      return list(tool, List.of(args));
    }
    public static List<String> list(String tool, List<String> args) {
      if (args.isEmpty()) return List.of(tool);
      if (args.size() == 1) return List.of(tool + ' ' + args.get(0));
      var strings = new ArrayList<String>();
      strings.add(tool + " with " + args.size() + " arguments:");
      var simple = true;
      for (String arg : args) {
        var minus = arg.startsWith("-");
        strings.add((simple | minus ? "\t" : "\t\t") + arg);
        simple = !minus;
      }
      return List.copyOf(strings);
    }
    public static String text(String... lines) {
      return String.join(System.lineSeparator(), lines);
    }
    public static String text(Iterable<String> lines) {
      return String.join(System.lineSeparator(), lines);
    }
    public static String text(Stream<String> lines) {
      return String.join(System.lineSeparator(), lines.collect(Collectors.toList()));
    }
    public static String textIndent(String indent, String... strings) {
      return indent + String.join(System.lineSeparator() + indent, strings);
    }
    public static String textIndent(String indent, Iterable<String> strings) {
      return indent + String.join(System.lineSeparator() + indent, strings);
    }
    public static String textIndent(String indent, Stream<String> strings) {
      return text(strings.map(string -> indent + string));
    }
    public static String toString(Duration duration) {
      return duration
          .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
          .toString()
          .substring(2)
          .replaceAll("(\\d[HMS])(?!$)", "$1 ")
          .toLowerCase();
    }
    public static String toString(Collection<Path> paths) {
      return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }
    private static final char[] HEX_TABLE = "0123456789abcdef".toCharArray();
    public static String hex(byte[] bytes) {
      var chars = new char[bytes.length * 2];
      for (int i = 0; i < bytes.length; i++) {
        int value = bytes[i] & 0xFF;
        chars[i * 2] = HEX_TABLE[value >>> 4];
        chars[i * 2 + 1] = HEX_TABLE[value & 0x0F];
      }
      return new String(chars);
    }
    private Strings() {}
  }
}
