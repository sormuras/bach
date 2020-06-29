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

package de.sormuras.bach.tool;

import de.sormuras.bach.Call;

/**
 * A tool call with a list of module-related options.
 *
 * @param <T> The type of the tool
 */
public interface WithModuleOptionsCall<T> extends Call<T> {

  /**
   * Specify the initial module(s).
   *
   * @param modules The initial module(s)
   * @return A new tool call instance with {@code --module <module>(,<module>)*} appended
   */
  default T withModule(Iterable<String> modules) {
    return with("--module", String.join(",", modules));
  }

  /**
   * Root modules to resolve in addition to the initial modules.
   *
   * <p>Or all modules on the module path if {@code "ALL-MODULE-PATH"} is given.
   *
   * @param modules The root modules to resolve in addition to the initial modules
   * @return A new tool call instance
   */
  default T withResolvingAdditionalModules(Iterable<String> modules) {
    return with("--add-modules", String.join(",", modules));
  }

  /**
   * Specify where to find application modules.
   *
   * @param path The path where to find application modules
   * @return A new tool call instance
   */
  default T withModulePath(String path) {
    return with("--module-path", path);
  }

  /**
   * Override or augment a module with classes and resources in JAR files or directories.
   *
   * @param module The name of the module to override or augment
   * @param path The path where to find more assets for the given module
   * @return A new tool call instance
   */
  default T withPatchModule(String module, String path) {
    return with("--patch-module", module + '=' + path);
  }
}
