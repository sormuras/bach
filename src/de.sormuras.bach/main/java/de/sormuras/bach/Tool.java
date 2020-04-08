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

package de.sormuras.bach;

import de.sormuras.bach.util.Strings;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/** Interface for {@code String}-based mutable tool invocation objects. */
public interface Tool {

  /** Return name of the tool to run. */
  String name();

  /** Return list of argument strings compiled from option properties. */
  List<String> args();

  @Convention
  default String join(Collection<Path> paths) {
    return Strings.toString(paths).replace("{MODULE}", "*");
  }

  default List<String> toStrings() {
    return Strings.list(name(), args());
  }
}
