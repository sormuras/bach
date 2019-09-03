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

package de.sormuras.bach;

import java.nio.file.Files;
import java.nio.file.Path;

/*BODY*/
public /*STATIC*/ class Validation {

  public static class Error extends AssertionError {
    Error(String expected, Object hint) {
      super(String.format("expected that %s: %s", expected, hint));
    }
  }

  static void validateDirectoryIfExists(Path path) {
    if (Files.exists(path)) validateDirectory(path);
  }

  static void validateDirectory(Path path) {
    if (!Files.isDirectory(path)) throw new Error("path is a directory", path.toUri());
  }
}
