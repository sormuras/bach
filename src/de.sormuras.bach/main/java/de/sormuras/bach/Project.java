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

import java.lang.module.ModuleDescriptor.Version;
import java.util.StringJoiner;

/** A project descriptor. */
public /*static*/ final class Project {

  private final Info info;

  public Project(Info info) {
    this.info = info;
  }

  public Info info() {
    return info;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
        .add("info=" + info)
        .toString();
  }

  public String toTitleAndVersion() {
    return info.title() + ' ' + info.version();
  }

  /** A basic information holder. */
  public static final class Info {

    private final String title;
    private final Version version;

    public Info(String title, Version version) {
      this.title = title;
      this.version = version;
    }

    public String title() {
      return title;
    }

    public Version version() {
      return version;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Info.class.getSimpleName() + "[", "]")
          .add("title='" + title + "'")
          .add("version=" + version)
          .toString();
    }
  }

  /** A builder for building {@link Project} objects. */
  public static class Builder {

    private String title;
    private Version version;

    public Project build() {
      var info = new Info(title, version);
      return new Project(info);
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder version(String version) {
      return version(Version.parse(version));
    }

    public Builder version(Version version) {
      this.version = version;
      return this;
    }
  }
}
