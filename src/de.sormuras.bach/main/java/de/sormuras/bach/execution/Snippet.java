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

package de.sormuras.bach.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Source code lines. */
public /*static*/ final class Snippet {

  /** Return new snippet for the given source code lines. */
  public static Snippet of(String... lines) {
    return new Snippet(Set.of(), List.of(lines));
  }

  /** Return new snippet merged from all given snippets. */
  public static Snippet of(List<Snippet> snippets) {
    var imports = new TreeSet<String>();
    var lines = new ArrayList<String>();
    for(var snippet : snippets) {
      imports.addAll(snippet.imports());
      lines.addAll(snippet.lines());
    }
    return new Snippet(imports, lines);
  }

  private final Set<String> imports;
  private final List<String> lines;

  public Snippet(Set<String> imports, List<String> lines) {
    this.imports = imports;
    this.lines = lines;
  }

  public Set<String> imports() {
    return imports;
  }

  public List<String> lines() {
    return lines;
  }
}
