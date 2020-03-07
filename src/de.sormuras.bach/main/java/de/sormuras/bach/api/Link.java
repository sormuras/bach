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

/** Link to an external module. */
public /*static*/ final class Link {

  public static Link direct(URI uri) {
    return new Link(uri, null, null, null);
  }

  public static Link maven(String group, String artifact) {
    return new Link(null, group, artifact, null);
  }

  public static Link maven(String group, String artifact, String version) {
    return new Link(null, group, artifact, version);
  }

  public static Link version(String version) {
    return new Link(null, null, null, version);
  }

  private final URI uri;
  private final String group;
  private final String artifact;
  private final String version;

  public Link(URI uri, String group, String artifact, String version) {
    this.uri = uri;
    this.group = group;
    this.artifact = artifact;
    this.version = version;
  }

  public URI uri() {
    return uri;
  }

  public String group() {
    return group;
  }

  public String artifact() {
    return artifact;
  }

  public String version() {
    return version;
  }
}
