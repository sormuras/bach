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

package de.sormuras.bach.project.library;

import de.sormuras.bach.project.Locator;
import java.util.Locale;
import java.util.Set;

/**
 * LWJGL is a Java library that enables cross-platform access to popular native APIs.
 *
 * @see <a href="https://lwjgl.org">lwjgl.org</a>
 */
public /*static*/ class LWJGLModules extends Locator.AbstractLocator {

  public LWJGLModules() {
    var version = "3.2.3";
    putJLWGL(version, "", "assimp", "bgfx", "cuda", "egl", "glfw");
    putJLWGL(version, "jawt", "jemalloc", "libdivide", "llvm", "lmdb", "lz4");
    putJLWGL(version, "meow", "nanovg", "nfd", "nuklear", "odbc");
    putJLWGL(version, "openal", "opencl", "opengl", "opengles", "openvr");
    putJLWGL(version, "opus", "ovr", "par", "remotery", "rpmalloc", "shaderc");
    putJLWGL(version, "sse", "stb", "tinyexr", "tinyfd", "tootle", "vma");
    putJLWGL(version, "vulkan", "xxhash", "yoga", "zstd");
  }

  private void putJLWGL(String version, String... names) {
    var group = "org.lwjgl";
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    var win = os.contains("win");
    var mac = os.contains("mac");
    // to be resolved: windows-x86, linux-arm32, linux-arm64
    var classifier = "natives-" + (win ? "windows" : mac ? "macos" : "linux");
    var nonnatives = Set.of("cuda", "egl", "jawt", "odbc", "opencl", "vulkan");
    var windows = Set.of("ovr"); // only windows natives are available
    for (var name : names) {
      var module = "org.lwjgl" + (name.isEmpty() ? "" : '.' + name);
      var artifact = "lwjgl" + (name.isEmpty() ? "" : '-' + name);
      var gav = String.join(":", group, artifact, version);
      put(module, Maven.central(gav, module, 0, null));
      if (nonnatives.contains(name)) continue;
      if (windows.contains(name) && !win) continue;
      put(module + ".natives", Maven.central(gav + ":" + classifier, module + ".natives", 0, null));
    }
  }
}
