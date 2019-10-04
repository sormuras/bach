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

package de.sormuras.bach;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*BODY*/
/** Static helpers. */
/*STATIC*/ class Util {

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

  static List<Path> findExisting(Collection<Path> paths) {
    return paths.stream().filter(Files::exists).collect(Collectors.toList());
  }

  static List<Path> findExistingDirectories(Collection<Path> directories) {
    return directories.stream().filter(Files::isDirectory).collect(Collectors.toList());
  }

  static boolean isApiMethod(Method method) {
    if (method.getDeclaringClass().equals(Object.class)) return false;
    if (Modifier.isStatic(method.getModifiers())) return false;
    return method.getParameterCount() == 0;
  }

  /** List all paths matching the given filter starting at given root paths. */
  static List<Path> find(Collection<Path> roots, Predicate<Path> filter) {
    var files = new ArrayList<Path>();
    for (var root : roots) {
      try (var stream = Files.walk(root)) {
        stream.filter(filter).forEach(files::add);
      } catch (Exception e) {
        throw new Error("Walking directory '" + root + "' failed: " + e, e);
      }
    }
    return List.copyOf(files);
  }

  /** Test supplied path for pointing to a Java source compilation unit. */
  static boolean isJavaFile(Path path) {
    if (Files.isRegularFile(path)) {
      var name = path.getFileName().toString();
      if (name.endsWith(".java")) {
        return name.indexOf('.') == name.length() - 5; // single dot in filename
      }
    }
    return false;
  }

  /** Test supplied path for pointing to a Java source compilation unit. */
  static boolean isJarFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar");
  }

  static boolean isModuleInfo(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
  }

  static boolean isWindows() {
    return System.getProperty("os.name", "?").toLowerCase().contains("win");
  }

  static List<Path> list(Path directory) {
    return list(directory, __ -> true);
  }

  static List<Path> list(Path directory, Predicate<Path> filter) {
    try (var stream = Files.list(directory)) {
      return stream.filter(filter).sorted().collect(Collectors.toList());
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

  /** Null-safe file name getter. */
  static Optional<String> findFileName(Path path) {
    return Optional.ofNullable(path.toAbsolutePath().getFileName()).map(Path::toString);
  }

  static Optional<String> findVersion(String jarFileName) {
    if (!jarFileName.endsWith(".jar")) return Optional.empty();
    var name = jarFileName.substring(0, jarFileName.length() - 4);
    var matcher = Pattern.compile("-(\\d+(\\.|$))").matcher(name);
    return (matcher.find()) ? Optional.of(name.substring(matcher.start() + 1)) : Optional.empty();
  }

  static Path require(Path path, Predicate<Path> predicate) {
    if (predicate.test(path)) {
      return path;
    }
    throw new IllegalArgumentException("Path failed test: " + path);
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

  /** Sleep and silently clear current thread's interrupted status. */
  static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.interrupted();
    }
  }

  /** @see Files#createDirectories(Path, FileAttribute[]) */
  static Path treeCreate(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new UncheckedIOException("create directories failed: " + path, e);
    }
    return path;
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

  /** File transfer utility. */
  static class Downloader {

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

    Downloader(PrintWriter out, PrintWriter err) {
      this(out, err, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    private Downloader(PrintWriter out, PrintWriter err, HttpClient client) {
      this.out = out;
      this.err = err;
      this.client = client;
    }

    Set<Path> download(Path directory, Collection<Item> items) {
      Util.treeCreate(directory);
      return items.stream()
          .parallel()
          .map(item -> download(item.uri, directory.resolve(item.file)))
          .collect(Collectors.toCollection(TreeSet::new));
    }

    Path download(URI uri, Path path) {
      if ("file".equals(uri.getScheme())) {
        try {
          return Files.copy(Path.of(uri), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
          throw new IllegalArgumentException("copy file failed:" + uri, e);
        }
      }
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
}
