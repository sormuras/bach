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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

class UtilTests {

  @Test
  void isJavaFile() {
    assertFalse(Util.isJavaFile(Paths.get("")));
    assertFalse(Util.isJavaFile(Paths.get("a/b")));
    assertTrue(Util.isJavaFile(Paths.get("src/test/UtilTests.java")));
    assertFalse(Util.isJavaFile(Paths.get("src/test-resources/Util.isJavaFile.java")));
  }

  @Test
  void isJarFile() {
    assertFalse(Util.isJarFile(Paths.get("")));
    assertFalse(Util.isJarFile(Paths.get("a/b")));
  }

  @Test
  void getPatchMap() {
    assertEquals(Map.of(), Util.getPatchMap(List.of(), List.of()));
    var main = Paths.get("demo/02-testing/src/main/java");
    var test = Paths.get("demo/02-testing/src/test/java");
    assertEquals(
        Map.of(
            "application", List.of(main.resolve("application")),
            "application.api", List.of(main.resolve("application.api"))),
        Util.getPatchMap(List.of(test), List.of(main)));
  }

  @Test
  void getClassPath() {
    assertEquals(List.of(), Util.getClassPath(List.of(), List.of()));
    var mods = List.of(Paths.get(".bach/resolved"));
    var deps = List.of(Paths.get(".bach/tools/google-java-format"));
    assertTrue(Util.getClassPath(mods, deps).size() >= 5);
  }

  @Test
  void getClassPathFails() {
    var deps = List.of(Paths.get("does", "not", "exist"));
    assertThrows(UncheckedIOException.class, () -> Util.getClassPath(deps, deps));
  }

  @Test
  void findDirectories() {
    var root = Paths.get(".").toAbsolutePath().normalize();
    var dirs = Util.findDirectories(root);
    assertTrue(dirs.contains(root.resolve("demo")));
    assertTrue(dirs.contains(root.resolve("src")));
  }

  @Test
  void findDirectoryNames() {
    var root = Paths.get(".").toAbsolutePath().normalize();
    var dirs = Util.findDirectoryNames(root);
    assertTrue(dirs.contains("demo"));
    assertTrue(dirs.contains("src"));
  }

  @Test
  void findDirectoriesReturnEmptyListWhenRootDoesNotExist() {
    var root = Paths.get("does", "not", "exist");
    assertTrue(Util.findDirectories(root).isEmpty());
    assertTrue(Util.findDirectoryNames(root).isEmpty());
  }

  @Test
  void findDirectoriesFails() throws Exception {
    var root = Files.createTempDirectory("findDirectoriesFails-");
    denyListing(root);
    assertThrows(UncheckedIOException.class, () -> Util.findDirectories(root));
    assertThrows(UncheckedIOException.class, () -> Util.findDirectoryNames(root));
    Util.removeTree(root);
  }

  private void denyListing(Path path) throws Exception {
    if (OS.WINDOWS.isCurrentOs()) {
      var upls = path.getFileSystem().getUserPrincipalLookupService();
      var user = upls.lookupPrincipalByName(System.getProperty("user.name"));
      var builder = AclEntry.newBuilder();
      builder.setPermissions(
          EnumSet.of(
              AclEntryPermission.EXECUTE,
              // AclEntryPermission.READ_DATA, // == LIST_DIRECTORY
              AclEntryPermission.READ_ATTRIBUTES,
              AclEntryPermission.READ_NAMED_ATTRS,
              AclEntryPermission.WRITE_DATA,
              AclEntryPermission.APPEND_DATA,
              AclEntryPermission.WRITE_ATTRIBUTES,
              AclEntryPermission.WRITE_NAMED_ATTRS,
              AclEntryPermission.DELETE_CHILD,
              AclEntryPermission.DELETE,
              AclEntryPermission.READ_ACL,
              AclEntryPermission.WRITE_ACL,
              AclEntryPermission.WRITE_OWNER,
              AclEntryPermission.SYNCHRONIZE));
      builder.setPrincipal(user);
      builder.setType(AclEntryType.ALLOW);
      var aclAttr = Files.getFileAttributeView(path, AclFileAttributeView.class);
      aclAttr.setAcl(List.of(builder.build()));
      return;
    }
    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("------rwx"));
  }

  @Test
  void getPathOfModuleReference() {
    var moduleReference = ModuleFinder.ofSystem().find("java.base").orElseThrow();
    assertEquals(URI.create("jrt:/java.base"), Util.getPath(moduleReference).toUri());
  }

  @Test
  void findJdkCommandPath() {
    assertTrue(Util.findJdkCommandPath("java").isPresent());
    assertFalse(Util.findJdkCommandPath("does not exist").isPresent());
  }

  @Test
  void moduleInfoEmpty() {
    var info = Util.ModuleInfo.of(List.of("module foo {}"));
    assertEquals("foo", info.getName());
    assertTrue(info.getRequires().isEmpty());
  }

  @Test
  void moduleInfoFromModuleWithoutNameFails() {
    var source = "module { no name }";
    Exception e = assertThrows(IllegalArgumentException.class, () -> Util.ModuleInfo.of(source));
    assertEquals("expected java module descriptor unit, but got: " + source, e.getMessage());
  }

  @Test
  void moduleInfoFromNonExistingFileFails() {
    var source = Paths.get(".", "module-info.java");
    var exception = assertThrows(UncheckedIOException.class, () -> Util.ModuleInfo.of(source));
    assertEquals("reading '" + source + "' failed", exception.getMessage());
  }

  @Test
  void moduleInfoRequiresBarAndBaz() {
    var source = "module   foo{requires a; requires static b; requires any modifier c;}";
    var info = Util.ModuleInfo.of(source);
    assertEquals("foo", info.getName());
    assertEquals(3, info.getRequires().size());
    assertTrue(info.getRequires().contains("a"));
    assertTrue(info.getRequires().contains("b"));
    assertTrue(info.getRequires().contains("c"));
  }

  @Test
  void moduleInfoFromFile() {
    var source = Paths.get("demo/02-testing/src/test/java/application");
    var info = Util.ModuleInfo.of(source);
    assertEquals("application", info.getName());
    assertEquals(2, info.getRequires().size());
    assertTrue(info.getRequires().contains("application.api"));
    assertTrue(info.getRequires().contains("org.junit.jupiter.api"));
  }

  @Test
  void moduleInfoFromM1() throws Exception {
    var loader = getClass().getClassLoader();
    var resource = loader.getResource("UtilTests.module-info.java");
    if (resource == null) {
      fail("resource not found!");
    }
    var info = Util.ModuleInfo.of(Paths.get(resource.toURI()));
    assertEquals("com.google.m", info.getName());
    assertEquals(3, info.getRequires().size());
    assertTrue(info.getRequires().contains("com.google.r1"));
    assertTrue(info.getRequires().contains("com.google.r2"));
    assertTrue(info.getRequires().contains("com.google.r3"));
  }

  @Test
  void getExternalModuleNames() {
    var names = Util.getExternalModuleNames(Paths.get("demo"));
    assertTrue(names.contains("org.junit.jupiter.api"));
    assertFalse(names.contains("hello"));
    assertFalse(names.contains("world"));
  }

  @Test
  void getExternalModuleNamesForNonExistingPathFails() {
    var path = Paths.get("does not exist");
    var e = assertThrows(UncheckedIOException.class, () -> Util.getExternalModuleNames(path));
    assertEquals("walking path failed for: does not exist", e.getMessage());
  }

  @Test
  void removeTreeForNonExistingPathFails() {
    var path = Paths.get("does not exist");
    var e = assertThrows(UncheckedIOException.class, () -> Util.removeTree(path));
    assertEquals("removing tree failed: does not exist", e.getMessage());
  }

  @Test
  void dumpTreeForNonExistingPathFails() {
    var path = Paths.get("does not exist");
    var e =
        assertThrows(UncheckedIOException.class, () -> Util.dumpTree(path, System.out::println));
    assertEquals("dumping tree failed: does not exist", e.getMessage());
  }

  private void createFiles(Path directory, int count) throws IOException {
    for (int i = 0; i < count; i++) {
      Files.createFile(directory.resolve("file-" + i));
    }
  }

  private void assertTreeDumpMatches(Path root, String... expected) {
    expected[0] = expected[0].replace(File.separatorChar, '/');
    List<String> dumpedLines = new ArrayList<>();
    Util.dumpTree(root, line -> dumpedLines.add(line.replace(File.separatorChar, '/')));
    assertLinesMatch(List.of(expected), dumpedLines);
  }

  @Test
  void tree() throws IOException {
    Path root = Files.createTempDirectory("tree-root-");
    assertTrue(Files.exists(root));
    assertEquals(1, Files.walk(root).count());
    assertTreeDumpMatches(root, root.toString(), ".");

    createFiles(root, 3);
    assertEquals(1 + 3, Files.walk(root).count());
    assertTreeDumpMatches(root, root.toString(), ".", "./file-0", "./file-1", "./file-2");

    createFiles(Files.createDirectory(root.resolve("a")), 3);
    createFiles(Files.createDirectory(root.resolve("b")), 3);
    createFiles(Files.createDirectory(root.resolve("x")), 3);
    assertTrue(Files.exists(root));
    assertEquals(1 + 3 + 4 * 3, Files.walk(root).count());
    assertTreeDumpMatches(
        root,
        root.toString(),
        ".",
        "./a",
        "./a/file-0",
        "./a/file-1",
        "./a/file-2",
        "./b",
        "./b/file-0",
        "./b/file-1",
        "./b/file-2",
        "./file-0",
        "./file-1",
        "./file-2",
        "./x",
        "./x/file-0",
        "./x/file-1",
        "./x/file-2");

    Util.removeTree(root, path -> path.startsWith(root.resolve("b")));
    assertEquals(1 + 2 + 3 * 3, Files.walk(root).count());
    assertTreeDumpMatches(
        root,
        root.toString(),
        ".",
        "./a",
        "./a/file-0",
        "./a/file-1",
        "./a/file-2",
        "./file-0",
        "./file-1",
        "./file-2",
        "./x",
        "./x/file-0",
        "./x/file-1",
        "./x/file-2");

    Util.removeTree(root, path -> path.endsWith("file-0"));
    assertEquals(1 + 2 + 3 * 2, Files.walk(root).count());
    assertTreeDumpMatches(
        root,
        root.toString(),
        ".",
        "./a",
        "./a/file-1",
        "./a/file-2",
        "./file-1",
        "./file-2",
        "./x",
        "./x/file-1",
        "./x/file-2");

    Util.removeTree(root);
    assertTrue(Files.notExists(root));
  }
}
