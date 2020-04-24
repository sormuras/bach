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

package de.sormuras.bach.project;

import java.nio.file.Path;
import java.util.StringJoiner;

/** An optionally targeted directory of Java source files: {@code src/foo/main/java[-11]}. */
public /*static*/ class Directory {

  /** Directory kind. */
  public enum Type {
    UNKNOWN,
    SOURCE,
    SOURCE_WITH_ROOT_MODULE_DESCRIPTOR,
    RESOURCE;

    public boolean isSource() {
      return this == SOURCE || this == SOURCE_WITH_ROOT_MODULE_DESCRIPTOR;
    }

    public boolean isSourceWithRootModuleDescriptor() {
      return this == SOURCE_WITH_ROOT_MODULE_DESCRIPTOR;
    }

    public String toMarkdown() {
      return isSource() ? ":scroll:" : this == RESOURCE ? ":books:" : "?";
    }
  }


  private final Path path;
  private final Type type;
  private final int release;

  public Directory(Path path, Type type, int release) {
    this.path = path;
    this.type = type;
    this.release = release;
  }

  public Path path() {
    return path;
  }

  public Type type() {
    return type;
  }

  public int release() {
    return release;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Directory.class.getSimpleName() + "[", "]")
        .add("path=" + path)
        .add("type=" + type)
        .add("release=" + release)
        .toString();
  }

  public String toMarkdown() {
    return type.toMarkdown() + " `" + path + "`" + (release == 0 ? "" : "@" + release);
  }
}
