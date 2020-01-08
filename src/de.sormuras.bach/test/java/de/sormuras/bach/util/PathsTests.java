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

package de.sormuras.bach.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PathsTests {
  @Test
  void userHomeIsDirectory() {
    assertTrue(Files.isDirectory(Paths.USER_HOME));
  }

  @Test
  void listEmptyDirectoryYieldsAnEmptyListOfPaths(@TempDir Path temp) {
    assertEquals(List.of(), Paths.list(temp, "*"));
    assertEquals(List.of(), Paths.list(temp, Files::isRegularFile));
    assertEquals(List.of(), Paths.list(temp, Files::isDirectory));
  }

  @Test
  void normalize() {
    assertEquals("", Path.of("").normalize().toString());
    assertEquals("", Path.of(".").normalize().toString());
    assertEquals("", Path.of("a/..").normalize().toString());
    assertEquals("", Path.of("a/b/../../.").normalize().toString());
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

  static List<Path> createFiles(Path directory, int count) throws Exception {
    var paths = new ArrayList<Path>();
    for (int i = 0; i < count; i++) {
      paths.add(Files.createFile(directory.resolve("file-" + i)));
    }
    return paths;
  }

  @Nested
  class Tree {
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

      Paths.delete(root, path -> path.startsWith(root.resolve("b")));
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

      Paths.delete(root, path -> path.endsWith("file-0"));
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

      Paths.copy(root.resolve("x"), root.resolve("a/b/c"));
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

      Paths.copy(root.resolve("x"), root.resolve("x/y"));
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

      Paths.deleteIfExists(root);
      assertTrue(Files.notExists(root));
    }

    @Test
    void copyNonExistingDirectoryFails() {
      var root = Path.of("does not exist");
      assertThrows(IllegalArgumentException.class, () -> Paths.copy(root, Path.of(".")));
    }

    @Test
    void copyAndItsPreconditions(@TempDir Path temp) throws Exception {
      var regular = createFiles(temp, 2).get(0);
      assertThrows(Throwable.class, () -> Paths.copy(regular, Path.of(".")));
      var directory = Files.createDirectory(temp.resolve("directory"));
      createFiles(directory, 3);
      assertThrows(Throwable.class, () -> Paths.copy(directory, regular));
      Paths.copy(directory, directory);
      assertThrows(Throwable.class, () -> Paths.copy(temp, directory));
      var forbidden = Files.createDirectory(temp.resolve("forbidden"));
      try {
        chmod(forbidden, false, false, true);
        assertThrows(Throwable.class, () -> Paths.copy(directory, forbidden));
      } finally {
        chmod(forbidden, true, true, true);
      }
    }

    @Test
    void deleteEmptyDirectory() throws Exception {
      var empty = Files.createTempDirectory("deleteEmptyDirectory");
      assertTrue(Files.exists(empty));
      Paths.delete(empty);
      assertTrue(Files.notExists(empty));
    }

    @Test
    void deleteNonExistingPath() {
      var root = Path.of("does not exist");
      assertDoesNotThrow(() -> Paths.delete(root));
    }

    @Test
    void walkFailsForNonExistingPath() {
      var root = Path.of("does not exist");
      assertThrows(RuntimeException.class, () -> Paths.walk(root, System.out::println));
    }

    private void assertTreeWalkMatches(Path root, String... expected) {
      var actualLines = new ArrayList<String>();
      Paths.walk(root, actualLines::add);
      assertLinesMatch(List.of(expected), actualLines);
    }
  }
}
