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
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

// Bach.java
/** Common utilities and helpers. */
interface Basics {

  /** Download the resource specified by its URI to the target directory. */
  static Path download(URI uri, Path targetDirectory) throws IOException {
    return download(uri, targetDirectory, fileName(uri), path -> true);
  }

  /** Download the resource from URI to the target directory using the provided file name. */
  static Path download(URI uri, Path directory, String fileName, Predicate<Path> skip)
      throws IOException {
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
    // TODO log("downloading `%s` [%s]...", url, urlLastModifiedTime);
    if (Files.exists(target)) {
      if (Files.getLastModifiedTime(target).equals(urlLastModifiedTime)) {
        if (Files.size(target) == urlConnection.getContentLengthLong()) {
          if (skip.test(target)) {
            // TODO log("skipped, using `%s`", target);
            return target;
          }
        }
      }
      Files.delete(target);
    }
    // TODO log("transferring `%s`...", uri);
    try (InputStream sourceStream = url.openStream();
        OutputStream targetStream = Files.newOutputStream(target)) {
      sourceStream.transferTo(targetStream);
    }
    Files.setLastModifiedTime(target, urlLastModifiedTime);
    // TODO log("stored `%s` [%s]", target, urlLastModifiedTime);
    return target;
  }

  /** Extract the file name from the uri. */
  static String fileName(URI uri) {
    String urlString = uri.getPath();
    int begin = urlString.lastIndexOf('/') + 1;
    return urlString.substring(begin).split("\\?")[0].split("#")[0];
  }

  static Stream<Path> findDirectories(Path root) {
    try {
      return Files.find(root, 1, (path, attr) -> Files.isDirectory(path))
          .filter(path -> !root.equals(path));
    } catch (Exception e) {
      throw new Error("should not happen", e);
    }
  }

  static Stream<String> findDirectoryNames(Path root) {
    return findDirectories(root).map(root::relativize).map(Path::toString);
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
    return new Resolvable(group, artifact, version)
        .resolve(Paths.get(".bach", "resolved"), Resolvable.REPOSITORIES);
  }

  /** Extract substring between begin and end tags. */
  static String substring(String string, String beginTag, String endTag) {
    int beginIndex = string.indexOf(beginTag) + beginTag.length();
    int endIndex = string.indexOf(endTag, beginIndex);
    return string.substring(beginIndex, endIndex).trim();
  }

  /** Copy source directory to target directory. */
  static void treeCopy(Path source, Path target) throws IOException {
    // log.fine("copy `%s` to `%s`%n", source, target);
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
      List<Path> paths = stream.collect(Collectors.toList());
      // log("copying %s elements...%n", paths.size());
      for (Path path : paths) {
        Path destination = target.resolve(source.relativize(path));
        if (Files.isDirectory(path)) {
          Files.createDirectories(destination);
          continue;
        }
        Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new AssertionError("dumpTree failed", e);
    }
  }

  /** Delete directory. */
  static void treeDelete(Path root) throws IOException {
    treeDelete(root, path -> true);
  }

  /** Delete selected files and directories from the root directory. */
  static void treeDelete(Path root, Predicate<Path> filter) throws IOException {
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
      throw new AssertionError("dumpTree failed", e);
    }
  }

  class Resolvable {

    static final List<String> REPOSITORIES =
        List.of(
            "https://oss.sonatype.org/content/repositories/snapshots",
            "http://repo1.maven.org/maven2",
            "https://jcenter.bintray.com",
            "https://jitpack.io");

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

    boolean isSnapshot() {
      return version.endsWith("SNAPSHOT");
    }

    Path resolve(Path targetDirectory, List<String> repositories) {
      for (String repository : repositories) {
        try {
          return resolve(targetDirectory, repository);
        } catch (IOException e) {
          // e.printStackTrace();
        }
      }
      throw new Error("could not resolve: " + this);
    }

    Path resolve(Path targetDirectory, String repository) throws IOException {
      URI uri = resolveUri(repository);
      String fileName = Basics.fileName(uri);
      // revert local filename with constant version attribute
      if (isSnapshot()) {
        fileName = this.file;
      }
      return download(uri, targetDirectory, fileName, path -> true);
    }

    /** Create uri for specified maven coordinates. */
    URI resolveUri(String repository) {
      String base = repository + '/' + group + '/' + artifact + '/' + version + '/';
      String file = this.file;
      if (isSnapshot()) {
        URI xml = URI.create(base + "maven-metadata.xml");
        try (InputStream sourceStream = xml.toURL().openStream();
            ByteArrayOutputStream targetStream = new ByteArrayOutputStream()) {
          sourceStream.transferTo(targetStream);
          String meta = targetStream.toString("UTF-8");
          String timestamp = Basics.substring(meta, "<timestamp>", "<");
          String buildNumber = Basics.substring(meta, "<buildNumber>", "<");
          file = file.replace("SNAPSHOT", timestamp + '-' + buildNumber);
        } catch (Exception exception) {
          // use file name with "SNAPSHOT" literal
        }
      }
      return URI.create(base + file);
    }

    @Override
    public String toString() {
      return String.format("Resolvable{%s %s %s}", group.replace('/', '.'), artifact, version);
    }
  }
}
