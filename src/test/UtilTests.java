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
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class UtilTests {

  @Test
  void downloadUsingHttps(Bach bach) throws Exception {
    var temporary = Files.createTempDirectory("BachTests.downloadUsingHttps-");
    bach.util.download(URI.create("https://junit.org/junit5/index.html"), temporary);
    bach.util.removeTree(temporary);
  }

  @Test
  void downloadUsingLocalFileSystem(BachContext context) throws Exception {
    var bach = context.bach;
    var logger = context.recorder;

    var tempRoot = Files.createTempDirectory("BachTests.downloadUsingLocalFileSystem-");
    var content = List.of("Lorem", "ipsum", "dolor", "sit", "amet");
    var tempFile = Files.createFile(tempRoot.resolve("source.txt"));
    Files.write(tempFile, content);
    var tempPath = Files.createDirectory(tempRoot.resolve("target"));
    var first = bach.util.download(tempFile.toUri(), tempPath);
    var name = tempFile.getFileName().toString();
    var actual = tempPath.resolve(name);
    assertEquals(actual, first);
    assertTrue(Files.exists(actual));
    assertLinesMatch(content, Files.readAllLines(actual));
    assertLinesMatch(
        List.of(
            "download.*",
            "transferring `" + tempFile.toUri().toString() + "`...",
            "`" + name + "` downloaded .*"),
        logger.all);
    // reload
    logger.all.clear();
    var second = bach.util.download(tempFile.toUri(), tempPath);
    assertEquals(first, second);
    assertLinesMatch(
        List.of(
            "download.*",
            "local file already exists -- comparing properties to remote file...",
            "local and remote file properties seem to match, using .*"),
        logger.all);
    // offline mode
    logger.all.clear();
    bach.vars.offline = true;
    var third = bach.util.download(tempFile.toUri(), tempPath);
    assertEquals(second, third);
    assertLinesMatch(List.of("download.*"), logger.all);
    // offline mode with error
    Files.delete(actual);
    assertThrows(Error.class, () -> bach.util.download(tempFile.toUri(), tempPath));
    // online but different file
    logger.all.clear();
    bach.vars.offline = false;
    Files.write(actual, List.of("Hello world!"));
    var forth = bach.util.download(tempFile.toUri(), tempPath);
    assertEquals(actual, forth);
    assertLinesMatch(content, Files.readAllLines(actual));
    assertLinesMatch(
        List.of(
            "download.*",
            "local file already exists -- comparing properties to remote file...",
            "local file `.*` differs from remote one -- replacing it",
            "transferring `" + tempFile.toUri().toString() + "`...",
            "`" + name + "` downloaded .*"),
        logger.all);
    bach.util.removeTree(tempRoot);
  }

  @Test
  void isJavaFile(Bach.Util util) {
    assertFalse(util.isJavaFile(Paths.get("")));
    assertFalse(util.isJavaFile(Paths.get("a/b")));
    assertTrue(util.isJavaFile(Paths.get("src/test/UtilTests.java")));
    assertFalse(util.isJavaFile(Paths.get("src/test-resources/Util.isJavaFile.java")));
  }

  @Test
  void isJarFile(Bach.Util util) {
    assertFalse(util.isJarFile(Paths.get("")));
    assertFalse(util.isJarFile(Paths.get("a/b")));
  }

  @Test
  void getPatchMap(Bach.Util util) {
    assertEquals(Map.of(), util.getPatchMap(List.of(), List.of()));
    var main = Paths.get("demo/02-testing/src/main/java");
    var test = Paths.get("demo/02-testing/src/test/java");
    assertEquals(
        Map.of(
            "application", List.of(main.resolve("application")),
            "application.api", List.of(main.resolve("application.api"))),
        util.getPatchMap(List.of(test), List.of(main)));
  }

  @Test
  void getClassPath(Bach.Util util) {
    assertEquals(List.of(), util.getClassPath(List.of(), List.of()));
    var mods = List.of(Paths.get(".bach/resolved"));
    var deps = List.of(Paths.get(".bach/tools/google-java-format"));
    assertTrue(util.getClassPath(mods, deps).size() >= 5);
  }

  @Test
  void getClassPathFails(Bach.Util util) {
    var deps = List.of(Paths.get("does", "not", "exist"));
    assertThrows(UncheckedIOException.class, () -> util.getClassPath(deps, deps));
  }

  @Test
  void findDirectories(Bach.Util util) {
    var root = Paths.get(".").toAbsolutePath().normalize();
    var dirs = util.findDirectories(root);
    assertTrue(dirs.contains(root.resolve("demo")));
    assertTrue(dirs.contains(root.resolve("src")));
  }

  @Test
  void findDirectoryNames(Bach.Util util) {
    var root = Paths.get(".").toAbsolutePath().normalize();
    var dirs = util.findDirectoryNames(root);
    assertTrue(dirs.contains("demo"));
    assertTrue(dirs.contains("src"));
  }

  @Test
  void findDirectoriesReturnEmptyListWhenRootDoesNotExist(Bach.Util util) {
    var root = Paths.get("does", "not", "exist");
    assertTrue(util.findDirectories(root).isEmpty());
    assertTrue(util.findDirectoryNames(root).isEmpty());
  }

  @Test
  void findDirectoriesFails(Bach.Util util) throws Exception {
    var root = Files.createTempDirectory("findDirectoriesFails-");
    denyListing(root);
    assertThrows(UncheckedIOException.class, () -> util.findDirectories(root));
    assertThrows(UncheckedIOException.class, () -> util.findDirectoryNames(root));
    util.removeTree(root);
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
  void getPathOfModuleReference(Bach.Util util) {
    var moduleReference = ModuleFinder.ofSystem().find("java.base").orElseThrow();
    assertEquals(URI.create("jrt:/java.base"), util.getPath(moduleReference).toUri());
  }

  @Test
  void findJdkCommandPath(Bach.Util util) {
    assertTrue(util.findJdkCommandPath("java").isPresent());
    assertFalse(util.findJdkCommandPath("does not exist").isPresent());
  }

  @Test
  void moduleInfoEmpty() {
    var info = ModuleInfo.of(List.of("module foo {}"));
    assertEquals("foo", info.getName());
    assertTrue(info.getRequires().isEmpty());
  }

  @Test
  void moduleInfoFromModuleWithoutNameFails() {
    var source = "module { no name }";
    Exception e = assertThrows(IllegalArgumentException.class, () -> ModuleInfo.of(source));
    assertEquals("expected java module descriptor unit, but got: " + source, e.getMessage());
  }

  @Test
  void moduleInfoFromNonExistingFileFails() {
    var source = Paths.get(".", "module-info.java");
    var exception = assertThrows(UncheckedIOException.class, () -> ModuleInfo.of(source));
    assertEquals("reading '" + source + "' failed", exception.getMessage());
  }

  @Test
  void moduleInfoRequiresBarAndBaz() {
    var source = "module   foo{requires a; requires static b; requires any modifier c;}";
    var info = ModuleInfo.of(source);
    assertEquals("foo", info.getName());
    assertEquals(3, info.getRequires().size());
    assertTrue(info.getRequires().contains("a"));
    assertTrue(info.getRequires().contains("b"));
    assertTrue(info.getRequires().contains("c"));
  }

  @Test
  void moduleInfoFromFile() {
    var source = Paths.get("demo/02-testing/src/test/java/application");
    var info = ModuleInfo.of(source);
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
    var info = ModuleInfo.of(Paths.get(resource.toURI()));
    assertEquals("com.google.m", info.getName());
    assertEquals(3, info.getRequires().size());
    assertTrue(info.getRequires().contains("com.google.r1"));
    assertTrue(info.getRequires().contains("com.google.r2"));
    assertTrue(info.getRequires().contains("com.google.r3"));
  }

  @Test
  void getExternalModuleNames(Bach.Util util) {
    var names = util.getExternalModuleNames(Paths.get("demo"));
    assertTrue(names.contains("org.junit.jupiter.api"));
    assertFalse(names.contains("hello"));
    assertFalse(names.contains("world"));
  }

  @Test
  void getExternalModuleNamesForNonExistingPathFails(Bach.Util util) {
    var path = Paths.get("does not exist");
    var e = assertThrows(UncheckedIOException.class, () -> util.getExternalModuleNames(path));
    assertEquals("walking path failed for: does not exist", e.getMessage());
  }

  @Test
  void removeTreeForNonExistingPathFails(Bach.Util util) {
    var path = Paths.get("does not exist");
    var e = assertThrows(UncheckedIOException.class, () -> util.removeTree(path));
    assertEquals("removing tree failed: does not exist", e.getMessage());
  }

  @Test
  void dumpTreeForNonExistingPathFails(Bach.Util util) {
    var path = Paths.get("does not exist");
    var e =
        assertThrows(UncheckedIOException.class, () -> util.dumpTree(path, System.out::println));
    assertEquals("dumping tree failed: does not exist", e.getMessage());
  }

  private void createFiles(Path directory, int count) throws Exception {
    for (int i = 0; i < count; i++) {
      Files.createFile(directory.resolve("file-" + i));
    }
  }

  private void assertTreeDumpMatches(Bach.Util util, Path root, String... expected) {
    expected[0] = expected[0].replace(File.separatorChar, '/');
    List<String> dumpedLines = new ArrayList<>();
    util.dumpTree(root, line -> dumpedLines.add(line.replace(File.separatorChar, '/')));
    assertLinesMatch(List.of(expected), dumpedLines);
  }

  @Test
  void tree(Bach.Util util) throws Exception {
    Path root = Files.createTempDirectory("tree-root-");
    assertTrue(Files.exists(root));
    assertEquals(1, Files.walk(root).count());
    assertTreeDumpMatches(util, root, root.toString(), ".");

    createFiles(root, 3);
    assertEquals(1 + 3, Files.walk(root).count());
    assertTreeDumpMatches(util, root, root.toString(), ".", "./file-0", "./file-1", "./file-2");

    createFiles(Files.createDirectory(root.resolve("a")), 3);
    createFiles(Files.createDirectory(root.resolve("b")), 3);
    createFiles(Files.createDirectory(root.resolve("x")), 3);
    assertTrue(Files.exists(root));
    assertEquals(1 + 3 + 4 * 3, Files.walk(root).count());
    assertTreeDumpMatches(
        util,
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

    util.removeTree(root, path -> path.startsWith(root.resolve("b")));
    assertEquals(1 + 2 + 3 * 3, Files.walk(root).count());
    assertTreeDumpMatches(
        util,
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

    util.removeTree(root, path -> path.endsWith("file-0"));
    assertEquals(1 + 2 + 3 * 2, Files.walk(root).count());
    assertTreeDumpMatches(
        util,
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

    util.removeTree(root);
    assertTrue(Files.notExists(root));
  }
}
