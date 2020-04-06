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

import java.net.URI;
import java.util.StringJoiner;

/** A set of additional project information. */
public /*static*/ class Information {

  public static Information of() {
    return new Information("", null);
  }

  private final String description;
  private final URI uri;

  public Information(String description, URI uri) {
    this.description = description;
    this.uri = uri;
  }

  public String description() {
    return description;
  }

  public URI uri() {
    return uri;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Information.class.getSimpleName() + "[", "]")
        .add("description='" + description + "'")
        .add("uri=" + uri)
        .toString();
  }
}
