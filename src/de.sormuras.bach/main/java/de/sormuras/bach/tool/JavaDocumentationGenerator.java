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

import java.nio.file.Path;
import java.util.List;

/** A {@code javadoc} call configuration generating HTML pages of API documentation from sources. */
public /*static*/ final class JavaDocumentationGenerator extends Tool {

  JavaDocumentationGenerator(List<? extends Option> options) {
    super("javadoc", options);
  }

  /** Specifies the destination directory where {@code javadoc} saves the generated HTML files. */
  public static final class DestinationDirectory extends KeyValueOption<Path> {

    public DestinationDirectory(Path directory) {
      super("-d", directory);
    }
  }

  /** Document the specified module(s). */
  public static final class DocumentListOfModules implements Option {
    private final List<String> modules;

    public DocumentListOfModules(List<String> modules) {
      this.modules = modules;
    }

    public List<String> modules() {
      return modules;
    }

    @Override
    public void visit(Arguments arguments) {
      arguments.add("--module", String.join(",", modules));
    }
  }
}
