/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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
import java.util.Map;
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
          "junit",
          "org.apiguardian.api",
          "org.hamcrest",
          "org.junit.platform.commons",
          "org.junit.platform.console",
          "org.junit.platform.engine",
          "org.junit.platform.launcher",
          "org.junit.platform.reporting",
          "org.junit.jupiter",
          "org.junit.jupiter.api",
          "org.junit.jupiter.engine",
          "org.junit.jupiter.params",
          "org.junit.vintage.engine",
          "org.opentest4j",
          "org.lwjgl",
          "org.lwjgl.assimp",
          "org.lwjgl.bgfx",
          "org.lwjgl.cuda",
          "org.lwjgl.egl",
          "org.lwjgl.glfw",
          "org.lwjgl.jawt",
          "org.lwjgl.jemalloc",
          "org.lwjgl.libdivide",
          "org.lwjgl.llvm",
          "org.lwjgl.lmdb",
          "org.lwjgl.lz4",
          "org.lwjgl.meow",
          "org.lwjgl.nanovg",
          "org.lwjgl.nfd",
          "org.lwjgl.nuklear",
          "org.lwjgl.odbc",
          "org.lwjgl.openal",
          "org.lwjgl.opencl",
          "org.lwjgl.opengl",
          "org.lwjgl.opengles",
          "org.lwjgl.openvr",
          "org.lwjgl.opus",
          "org.lwjgl.ovr",
          "org.lwjgl.par",
          "org.lwjgl.remotery",
          "org.lwjgl.rpmalloc",
          "org.lwjgl.shaderc",
          "org.lwjgl.sse",
          "org.lwjgl.stb",
          "org.lwjgl.tinyexr",
          "org.lwjgl.tinyfd",
          "org.lwjgl.tootle",
          "org.lwjgl.vma",
          "org.lwjgl.vulkan",
          "org.lwjgl.xxhash",
          "org.lwjgl.yoga",
          "org.lwjgl.zstd",
          "org.lwjgl.natives",
          "org.lwjgl.assimp.natives",
          "org.lwjgl.bgfx.natives",
          "org.lwjgl.cuda.natives",
          "org.lwjgl.egl.natives",
          "org.lwjgl.glfw.natives",
          "org.lwjgl.jawt.natives",
          "org.lwjgl.jemalloc.natives",
          "org.lwjgl.libdivide.natives",
          "org.lwjgl.llvm.natives",
          "org.lwjgl.lmdb.natives",
          "org.lwjgl.lz4.natives",
          "org.lwjgl.meow.natives",
          "org.lwjgl.nanovg.natives",
          "org.lwjgl.nfd.natives",
          "org.lwjgl.nuklear.natives",
          "org.lwjgl.odbc.natives",
          "org.lwjgl.openal.natives",
          "org.lwjgl.opencl.natives",
          "org.lwjgl.opengl.natives",
          "org.lwjgl.opengles.natives",
          "org.lwjgl.openvr.natives",
          "org.lwjgl.opus.natives",
          "org.lwjgl.ovr.natives",
          "org.lwjgl.par.natives",
          "org.lwjgl.remotery.natives",
          "org.lwjgl.rpmalloc.natives",
          "org.lwjgl.shaderc.natives",
          "org.lwjgl.sse.natives",
          "org.lwjgl.stb.natives",
          "org.lwjgl.tinyexr.natives",
          "org.lwjgl.tinyfd.natives",
          "org.lwjgl.tootle.natives",
          "org.lwjgl.vma.natives",
          "org.lwjgl.vulkan.natives",
          "org.lwjgl.xxhash.natives",
          "org.lwjgl.yoga.natives",
          "org.lwjgl.zstd.natives");

  @Test
  void defaultLibraryLinks() {
    assertEquals(DEFAULT_LINK_KEYS, Library.defaultLinks().keySet());
  }

  @Test
  void customLibraryLinks() {
    var custom =
        Map.of(
            "foo", new Library.Link("foo(${VERSION})", Version.parse("1")),
            "bar", new Library.Link("bar(${VERSION})", Version.parse("3")));
    var library = new Library(Set.of(), custom, Set.of());
    assertTrue(library.requires().isEmpty());
    assertEquals("Link{foo(${VERSION})@1}", library.links().get("foo").toString());
    assertEquals("Link{bar(${VERSION})@3}", library.links().get("bar").toString());
  }
}
