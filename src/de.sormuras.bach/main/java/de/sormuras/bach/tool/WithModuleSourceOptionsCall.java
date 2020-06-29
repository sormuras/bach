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

/**
 * A tool call with a list of module source-related options.
 *
 * @param <T> The type of the tool
 */
public interface WithModuleSourceOptionsCall<T> extends WithModuleOptionsCall<T> {

  /**
   * Specify where to find input source files for multiple modules.
   *
   * @param moduleSourcePath The path where to find input source files for multiple modules
   * @return A new tool call instance with {@code --module-source-path <module-source-path>}
   *     appended to the list of arguments
   */
  default T withModuleSourcePath(String moduleSourcePath) {
    return with("--module-source-path", moduleSourcePath);
  }

  /**
   * Specify character encoding used by source files.
   *
   * @param encoding The character encoding used by source files
   * @return A new tool call instance with {@code -encoding <encoding>} appended to the list of
   *     arguments
   */
  default T withEncoding(String encoding) {
    return with("-encoding", encoding);
  }
}
