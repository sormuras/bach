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

package test.base;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Tree {
  /** Walk directory tree structure. */
  static List<String> walk(Path root) {
    var list = new ArrayList<String>();
    walk(root, list::add);
    return list;
  }

  /** Walk directory tree structure. */
  static void walk(Path root, Consumer<String> out) {
    try (var stream = Files.walk(root)) {
      stream
          .map(root::relativize)
          .map(path -> path.toString().replace('\\', '/'))
          .filter(Predicate.not(String::isEmpty))
          .sorted()
          .forEach(out);
    } catch (Exception e) {
      throw new RuntimeException("Files walk failed: " + root, e);
    }
  }
}
