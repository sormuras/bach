package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PathsTests {

  @ParameterizedTest
  @ValueSource(strings = {"", ".", "a/..", "./a/b/../../."})
  void normalizePathWithOnlyRedundantElementsYieldsPathWithSingleEmptyName(Path path) {
    var normalized = path.normalize();
    assertEquals(1, normalized.getNameCount());
    assertEquals(Path.of(""), normalized.getName(0));
    assertEquals("", normalized.toString());
  }

  @Nested
  class DeleteTests {
    @Test
    void deleteEmptyDirectory() throws Exception {
      var empty = Files.createTempDirectory("deleteEmptyDirectory");
      assertTrue(Files.exists(empty));
      var deleted = Paths.deleteDirectories(empty);
      assertSame(empty, deleted);
      assertTrue(Files.notExists(deleted));
    }

    @Test
    void deleteNonExistingPath() {
      var root = Path.of("does not exist");
      assertDoesNotThrow(() -> Paths.deleteDirectories(root));
    }
  }

  @Nested
  class IsRootTests {
    @TestFactory
    Stream<DynamicTest> fileSystemRootDirectoriesAreRoots() {
      var roots = FileSystems.getDefault().getRootDirectories();
      return StreamSupport.stream(roots.spliterator(), false)
          .map(path -> dynamicTest(path.toString(), () -> assertTrue(PathsTests.isRoot(path))));
    }
  }

  @Nested
  class ListTests {

    @Test
    void listingOfBaseDirectory() {
      var actual = PathsTests.list(Path.of(""), Files::isRegularFile);
      assertLinesMatch(
          List.of(".gitignore", ">> MORE FILES >>", "README.md", ">> more files >>"),
          actual.stream().map(Path::toString).collect(Collectors.toList()));
    }

    @Test
    void listEmptyDirectoryYieldsAnEmptyListOfPaths(@TempDir Path temp) {
      assertEquals(List.of(), PathsTests.list(temp, __ -> true));
      assertEquals(List.of(), PathsTests.list(temp, Files::isRegularFile));
      assertEquals(List.of(), PathsTests.list(temp, Files::isDirectory));
    }
  }

  @Nested
  class DequeTests {

    @Test
    void emptyPathYieldsAnEmptyDeque() {
      assertEquals("[]", PathsTests.deque(Path.of("")).toString());
    }

    @Test
    void reversed() {
      assertEquals("[c, b, a]", PathsTests.deque(Path.of("a", "b", "c")).toString());
    }
  }

  @Nested
  class TreeTests {
    @Test
    void tree() throws Exception {
      Path root = Files.createTempDirectory("tree-root-");
      assertTrue(Files.exists(root));
      assertEquals(1, Files.walk(root).count());
      assertTreeWalkMatches(root);

      createFiles(root, 3);
      assertEquals(1 + 3, Files.walk(root).count());
      assertTreeWalkMatches(root, "file-0", "file-1", "file-2");

      createFiles(Files.createDirectory(root.resolve("a")), 3);
      createFiles(Files.createDirectory(root.resolve("b")), 3);
      createFiles(Files.createDirectory(root.resolve("x")), 4);
      assertTrue(Files.exists(root));
      assertTreeWalkMatches(
          root,
          "a",
          "a/file-0",
          "a/file-1",
          "a/file-2",
          "b",
          "b/file-0",
          "b/file-1",
          "b/file-2",
          "file-0",
          "file-1",
          "file-2",
          "x",
          "x/file-0",
          "x/file-1",
          "x/file-2",
          "x/file-3");

      Paths.deleteDirectories(root, path -> path.startsWith(root.resolve("b")));
      assertTreeWalkMatches(
          root,
          "a",
          "a/file-0",
          "a/file-1",
          "a/file-2",
          "file-0",
          "file-1",
          "file-2",
          "x",
          "x/file-0",
          "x/file-1",
          "x/file-2",
          "x/file-3");

      Paths.deleteDirectories(root, path -> path.endsWith("file-0"));
      assertTreeWalkMatches(
          root,
          "a",
          "a/file-1",
          "a/file-2",
          "file-1",
          "file-2",
          "x",
          "x/file-1",
          "x/file-2",
          "x/file-3");

      copy(root.resolve("x"), root.resolve("a/b/c"));
      assertTreeWalkMatches(
          root,
          "a",
          "a/b",
          "a/b/c",
          "a/b/c/file-1",
          "a/b/c/file-2",
          "a/b/c/file-3",
          "a/file-1",
          "a/file-2",
          "file-1",
          "file-2",
          "x",
          "x/file-1",
          "x/file-2",
          "x/file-3");

      copy(root.resolve("x"), root.resolve("x/y"));
      assertTreeWalkMatches(
          root,
          "a",
          "a/b",
          "a/b/c",
          "a/b/c/file-1",
          "a/b/c/file-2",
          "a/b/c/file-3",
          "a/file-1",
          "a/file-2",
          "file-1",
          "file-2",
          "x",
          "x/file-1",
          "x/file-2",
          "x/file-3",
          "x/y",
          "x/y/file-1",
          "x/y/file-2",
          "x/y/file-3");

      deleteIfExists(root);
      assertTrue(Files.notExists(root));
    }
  }

  static void assertTreeWalkMatches(Path root, String... expected) {
    var actualLines = new ArrayList<String>();
    walk(root, actualLines::add);
    assertLinesMatch(List.of(expected), actualLines);
  }

  static void chmod(Path path, boolean r, boolean w, boolean x) throws Exception {
    if (OS.WINDOWS.isCurrentOs()) {
      var upls = path.getFileSystem().getUserPrincipalLookupService();
      var user = upls.lookupPrincipalByName(System.getProperty("user.name"));
      var builder = AclEntry.newBuilder();
      var permissions =
          EnumSet.of(
              // AclEntryPermission.EXECUTE, // "x"
              // AclEntryPermission.READ_DATA, // "r"
              AclEntryPermission.READ_ATTRIBUTES,
              AclEntryPermission.READ_NAMED_ATTRS,
              // AclEntryPermission.WRITE_DATA, // "w"
              // AclEntryPermission.APPEND_DATA, // "w"
              AclEntryPermission.WRITE_ATTRIBUTES,
              AclEntryPermission.WRITE_NAMED_ATTRS,
              AclEntryPermission.DELETE_CHILD,
              AclEntryPermission.DELETE,
              AclEntryPermission.READ_ACL,
              AclEntryPermission.WRITE_ACL,
              AclEntryPermission.WRITE_OWNER,
              AclEntryPermission.SYNCHRONIZE);
      if (r) {
        permissions.add(AclEntryPermission.READ_DATA); // == LIST_DIRECTORY
      }
      if (w) {
        permissions.add(AclEntryPermission.WRITE_DATA); // == ADD_FILE
        permissions.add(AclEntryPermission.APPEND_DATA); // == ADD_SUBDIRECTORY
      }
      if (x) {
        permissions.add(AclEntryPermission.EXECUTE);
      }
      builder.setPermissions(permissions);
      builder.setPrincipal(user);
      builder.setType(AclEntryType.ALLOW);
      var aclAttr = Files.getFileAttributeView(path, AclFileAttributeView.class);
      aclAttr.setAcl(List.of(builder.build()));
      return;
    }
    var user = (r ? "r" : "-") + (w ? "w" : "-") + (x ? "x" : "-");
    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(user + "------"));
  }

  public static Path createDirectories(Path directory) {
    try {
      return Files.createDirectories(directory);
    } catch (Exception e) {
      throw new RuntimeException("Create directories failed: " + directory, e);
    }
  }

  static List<Path> createFiles(Path directory, int count) throws Exception {
    var paths = new ArrayList<Path>();
    for (int i = 0; i < count; i++) {
      paths.add(Files.createFile(directory.resolve("file-" + i)));
    }
    return paths;
  }

  /** Copy all files and directories from source to target directory. */
  static void copy(Path source, Path target) throws Exception {
    copy(source, target, __ -> true);
  }

  /** Copy selected files and directories from source to target directory. */
  static Set<Path> copy(Path source, Path target, Predicate<Path> filter) throws Exception {
    // debug("copy(source:`%s`, target:`%s`)%n", source, target);
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
        return Set.of();
      }
      if (target.startsWith(source)) {
        // copy "a/" to "a/b/"...
        throw new IllegalArgumentException("target must not a child of source");
      }
    }
    var paths = new TreeSet<Path>();
    try (var stream = Files.walk(source).sorted()) {
      for (var path : stream.collect(Collectors.toList())) {
        var destination = target.resolve(source.relativize(path));
        if (Files.isDirectory(path)) {
          Files.createDirectories(destination);
          continue;
        }
        if (filter.test(path)) {
          Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
          paths.add(destination);
        }
      }
    }
    return paths;
  }

  /** Delete directory. */
  static Path deleteIfExists(Path directory) {
    if (Files.notExists(directory)) return directory;
    return Paths.deleteDirectories(directory, __ -> true);
  }

  /** Walk directory tree structure. */
  static void walk(Path root, Consumer<String> out) {
    try (var stream = Files.walk(root)) {
      stream
          .map(root::relativize)
          .map(path -> path.toString().replace('\\', '/'))
          .sorted()
          .filter(Predicate.not(String::isEmpty))
          .forEach(out);
    } catch (Exception e) {
      throw new RuntimeException("Walking tree failed: " + root, e);
    }
  }


  /** Convert path element names of the given unit into a reversed deque. */
  public static Deque<String> deque(Path path) {
    var deque = new ArrayDeque<String>();
    path.forEach(name -> deque.addFirst(name.toString()));
    return deque;
  }

  /** Test supplied path for pointing to a Java Archive file. */
  public static boolean isJarFile(Path path) {
    return Files.isRegularFile(path) && name(path).endsWith(".jar");
  }

  /** Test supplied path for pointing to a Java compilation unit. */
  public static boolean isJavaFile(Path path) {
    return Files.isRegularFile(path) && name(path).endsWith(".java");
  }

  /** Test supplied path for pointing to a Java module declaration compilation unit. */
  public static boolean isModuleInfoJavaFile(Path path) {
    return isJavaFile(path) && name(path).equals("module-info.java");
  }

  /** Test supplied path for pointing to a Java module declaration for a given realm. */
  public static boolean isModuleInfoJavaFileForRealm(Path info, String realm) {
    return isModuleInfoJavaFile(info) && Collections.frequency(deque(info), realm) == 1;
  }

  /** Test for a path pointing to a file system root like {@code /} or {@code C:\}. */
  public static boolean isRoot(Path path) {
    return path.toAbsolutePath().normalize().getNameCount() == 0;
  }

  /** Return {@code true} if the given name of the view is supported by the given file. */
  public static boolean isViewSupported(Path file, String view) {
    return file.getFileSystem().supportedFileAttributeViews().contains(view);
  }

  /** List content of specified directory in natural order with the given filter applied. */
  public static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var directoryStream = Files.newDirectoryStream(directory, filter)) {
      directoryStream.forEach(paths::add);
    } catch (Exception e) {
      throw new Error("Stream directory '" + directory + "' failed: " + e, e);
    }
    return List.copyOf(paths);
  }

  /** Walk all trees to find matching paths the given filter starting at given root paths. */
  public static List<Path> find(Collection<Path> roots, int maxDepth, Predicate<Path> filter) {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    for (var root : roots) {
      try (var stream = Files.walk(root, maxDepth)) {
        stream.filter(filter).forEach(paths::add);
      } catch (Exception e) {
        throw new Error("Walk directory '" + root + "' failed: " + e, e);
      }
    }
    return List.copyOf(paths);
  }

  public static List<Path> findModuleInfoJavaFiles(Path directory, int limit) {
    if (isRoot(directory)) throw new IllegalStateException("Root directory: " + directory);
    var units = find(List.of(directory), limit, PathsTests::isModuleInfoJavaFile);
    if (units.isEmpty()) throw new IllegalStateException("No module-info.java: " + directory);
    return List.copyOf(units);
  }

  /** Join a collection of path objects to a string using the system-dependent separator. */
  public static String join(Collection<Path> paths) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
  }

  public static Optional<String> joinExisting(Path... elements) {
    var paths = retainExisting(elements);
    return paths.isEmpty() ? Optional.empty() : Optional.of(join(paths));
  }

  public static List<Path> retainExisting(Path... elements) {
    var paths = new ArrayList<Path>();
    for (var element : elements) if (Files.exists(element)) paths.add(element);
    return List.copyOf(paths);
  }

  /** Return path's file name as a {@link String}. */
  public static String name(Path path) {
    return path.getNameCount() == 0 ? "" : path.getFileName().toString();
  }

  public static String replaceBackslashes(Path path) {
    return path.toString().replace('\\', '/');
  }

  public static String quote(Path path) {
    return '"' + replaceBackslashes(path) + '"';
  }

  /** Return the size of a file in bytes. */
  public static long size(Path path) {
    try {
      return Files.size(path);
    } catch (Exception e) {
      throw new Error("Size of file failed: " + e, e);
    }
  }

}