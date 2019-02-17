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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

class UtilTests {

  @Test
  void currentJavaHome() {
    assertTrue(Files.isDirectory(Util.currentJavaHome()));
  }

  @Test
  void integer() {
    assertEquals(0, Util.integer("0", 1));
    assertEquals(0, Util.integer("*", 0));
    assertThrows(NumberFormatException.class, () -> Util.integer("*", null));
  }

  @Test
  void isJavaFile() {
    assertFalse(Util.isJavaFile(Path.of("")));
    assertFalse(Util.isJavaFile(Path.of("a/b")));
    assertTrue(Util.isJavaFile(Path.of("src/test/UtilTests.java")));
    assertFalse(Util.isJavaFile(Path.of("src/test-resources/Util.isJavaFile.java")));
  }

  @Test
  void last() {
    assertEquals("", last(""));
    assertEquals("a", last("a"));
    assertEquals("b", last("a", "b"));
    assertEquals("c", last("a", "b", "c"));
    assertEquals(File.separator, last(File.separator));
    assertEquals("a", last(File.separator + "a"));
    assertEquals("b", last("a" + File.separator + "b"));
    assertEquals("b", last(File.separator + "a" + File.separator + "b"));
  }

  @Test
  void loadPropertiesFromDirectoryFails() {
    assertThrows(UncheckedIOException.class, () -> Util.loadProperties(Path.of(".")));
  }

  @Test
  void loadPropertiesFromTestResources() {
    var path = Path.of("src", "test-resources", "Property.load.properties");
    var map = Util.loadProperties(path);
    assertEquals("true", map.get("bach.offline"));
    assertEquals("Test Project Name", map.get("project.name"));
    assertEquals("1.2.3-SNAPSHOT", map.get("project.version"));
    assertEquals("level = %s | message = %s %n", map.get("bach.log.format"));
    assertEquals(4, map.size());
  }

  @Test
  void removeTreeForNonExistingPathFails() {
    var path = Path.of("does not exist");
    var e = assertThrows(UncheckedIOException.class, () -> Util.removeTree(path));
    assertEquals("removing tree failed: does not exist", e.getMessage());
  }

  @Test
  void removeTreeForEmptyDirectoryWorks() throws Exception {
    var temp = Files.createTempDirectory("bach-UtilTests.downloadUsingHttps-");
    assertTrue(Files.exists(temp));
    Util.removeTree(temp, __ -> true);
    assertFalse(Files.exists(temp));
  }

  @Test
  void emptyDirectoryChecks() throws Exception {
    var temp = Files.createTempDirectory("bach-UtilTests.emptyDirectoryChecks-");
    assertTrue(Files.exists(temp));
    assertTrue(Util.isEmpty(temp));
    var file = Files.createTempFile(temp, "file-", ".temp");
    assertFalse(Util.isEmpty(temp));
    assertThrows(UncheckedIOException.class, () -> Util.isEmpty(file));
    Util.removeTree(temp, __ -> true);
    assertFalse(Files.exists(file));
    assertFalse(Files.exists(temp));
  }

  @Test
  void findDirectories() {
    var root = Path.of(".").toAbsolutePath().normalize();
    var dirs = Util.findDirectories(root);
    assertTrue(dirs.contains(root.resolve("demo")));
    assertTrue(dirs.contains(root.resolve("src")));
  }

  @Test
  void findDirectoryNames() {
    var root = Path.of(".").toAbsolutePath().normalize();
    var dirs = Util.findDirectoryNames(root);
    assertTrue(dirs.contains("demo"));
    assertTrue(dirs.contains("src"));
  }

  @Test
  void findDirectoriesReturnEmptyListWhenRootDoesNotExist() {
    var root = Path.of("does", "not", "exist");
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

  private static String last(String first, String... more) {
    return Util.last(Path.of(first, more)).toString();
  }
}
