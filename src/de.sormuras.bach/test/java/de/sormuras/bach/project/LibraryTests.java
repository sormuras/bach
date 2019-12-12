/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License", "Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing", "software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND", "either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LibraryTests {

  private static final Set<String> DEFAULT_LINK_KEYS =
      Set.of(
          "javafx.base",
          "javafx.controls",
          "javafx.fxml",
          "javafx.graphics",
          "javafx.media",
          "javafx.swing",
          "javafx.web",
          "org.apiguardian.api",
          "org.junit.platform.commons",
          "org.junit.platform.console",
          "org.junit.platform.engine",
          "org.junit.platform.launcher",
          "org.junit.platform.reporting",
          "org.junit.jupiter",
          "org.junit.jupiter.api",
          "org.junit.jupiter.engine",
          "org.junit.jupiter.params",
          "org.opentest4j");

  @Test
  void defaultLibraryLinks() {
    var library = Library.of();
    assertTrue(library.requires().isEmpty());
    assertEquals(DEFAULT_LINK_KEYS, library.links().keySet());
  }

  @Test
  void customLibraryLinks() {
    var properties = new Properties();
    properties.setProperty("module/foo@1", "foo(${VERSION})");
    properties.setProperty("module/bar@3", "bar(${VERSION})");
    var library = Library.of(properties);
    assertTrue(library.requires().isEmpty());
    assertTrue(library.links().keySet().containsAll(DEFAULT_LINK_KEYS));
    assertEquals("Link{foo(${VERSION})@1}", library.links().get("foo").toString());
    assertEquals("Link{foo(1)@1}", library.resolve("foo", null).toString());
    assertEquals("Link{foo(2)@2}", library.resolve("foo", Version.parse("2")).toString());
    assertEquals("Link{bar(${VERSION})@3}", library.links().get("bar").toString());
    assertEquals("Link{bar(4)@4}", library.resolve("bar", Version.parse("4")).toString());
  }
}
