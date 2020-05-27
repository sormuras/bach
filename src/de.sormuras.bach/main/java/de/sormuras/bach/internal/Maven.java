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

package de.sormuras.bach.internal;

import java.util.Objects;
import java.util.StringJoiner;

/** Maven-related utilities. */
public /*static*/ class Maven {

  public static final String CENTRAL_REPOSITORY = "https://repo.maven.apache.org/maven2";

  /** Return the string representation of a JAR file hosted at Maven Central. */
  public static String central(String group, String artifact, String version) {
    return central(group, artifact, version, "");
  }

  /** Return the string representation of a JAR file hosted at Maven Central. */
  public static String central(String group, String artifact, String version, String classifier) {
    return Joiner.of(group, artifact, version).classifier(classifier).toString();
  }

  /** A Maven unified resource identifier string representation builder. */
  public static class Joiner {

    /** Create new instance with group, artifact, and version initialized accordingly. */
    public static Joiner of(String group, String artifact, String version) {
      return new Joiner().group(group).artifact(artifact).version(version);
    }

    private String repository = CENTRAL_REPOSITORY;
    private String group;
    private String artifact;
    private String version;
    private String classifier = "";
    private String type = "jar";

    @Override
    public String toString() {
      var joiner = new StringJoiner("/").add(repository);
      joiner.add(group.replace('.', '/')).add(artifact).add(version);
      var file = artifact + '-' + (classifier.isBlank() ? version : version + '-' + classifier);
      return joiner.add(file + '.' + type).toString();
    }

    public Joiner repository(String repository) {
      this.repository = Objects.requireNonNull(repository, "repository");
      return this;
    }

    public Joiner group(String group) {
      this.group = Objects.requireNonNull(group, "group");
      return this;
    }

    public Joiner artifact(String artifact) {
      this.artifact = Objects.requireNonNull(artifact, "artifact");
      return this;
    }

    public Joiner version(String version) {
      this.version = Objects.requireNonNull(version, "version");
      return this;
    }

    public Joiner classifier(String classifier) {
      this.classifier = Objects.requireNonNull(classifier, "classifier");
      return this;
    }

    public Joiner type(String type) {
      this.type = Objects.requireNonNull(type, "type");
      return this;
    }
  }

  private Maven() {}
}
