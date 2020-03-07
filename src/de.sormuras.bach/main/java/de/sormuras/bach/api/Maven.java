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

package de.sormuras.bach.api;

import java.net.URI;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Supplier;

/** Maven-related resources and utilities. */
public interface Maven {

  /** Return new resource builder instance. */
  static Resource.Builder newResource() {
    return new Resource.Builder();
  }

  /** Maven Central URI. */
  static URI central(String group, String artifact, String version) {
    var resource = newResource().repository("https://repo.maven.apache.org/maven2");
    return resource.group(group).artifact(artifact).version(version).build().get();
  }

  /** Maven unified resource identifier supplier. */
  final class Resource implements Supplier<URI> {

    private final String repository;
    private final String group;
    private final String artifact;
    private final String version;
    private final String classifier;
    private final String type;

    public Resource(
        String repository,
        String group,
        String artifact,
        String version,
        String classifier,
        String type) {
      this.repository = repository;
      this.group = group;
      this.artifact = artifact;
      this.version = version;
      this.classifier = classifier;
      this.type = type;
    }

    @Override
    public URI get() {
      Objects.requireNonNull(repository, "repository");
      Objects.requireNonNull(group, "group");
      Objects.requireNonNull(artifact, "artifact");
      Objects.requireNonNull(version, "version");
      var filename = artifact + '-' + (classifier.isEmpty() ? version : version + '-' + classifier);
      return URI.create(
          new StringJoiner("/")
              .add(repository)
              .add(group.replace('.', '/'))
              .add(artifact)
              .add(version)
              .add(filename + '.' + type)
              .toString());
    }

    public static class Builder {
      private String repository;
      private String group;
      private String artifact;
      private String version;
      private String classifier = "";
      private String type = "jar";

      private Builder() {}

      public Resource build() {
        return new Resource(repository, group, artifact, version, classifier, type);
      }

      public Builder repository(String repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        return this;
      }

      public Builder group(String group) {
        this.group = Objects.requireNonNull(group, "group");
        return this;
      }

      public Builder artifact(String artifact) {
        this.artifact = Objects.requireNonNull(artifact, "artifact");
        return this;
      }

      public Builder version(String version) {
        this.version = Objects.requireNonNull(version, "version");
        return this;
      }

      public Builder classifier(String classifier) {
        this.classifier = Objects.requireNonNull(classifier, "classifier");
        return this;
      }

      public Builder type(String type) {
        this.type = Objects.requireNonNull(type, "type");
        return this;
      }
    }
  }
}
