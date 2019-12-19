/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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
import java.nio.file.Path;

/** Properties used to upload compiled modules. */
public class /*record*/ Deployment {
  private final String mavenGroup;
  private final Path mavenPomTemplate;
  private final String mavenRepositoryId;
  private final URI mavenUri;

  public Deployment(String mavenGroup, Path mavenPomTemplate, String mavenRepositoryId, URI mavenUri) {
    this.mavenGroup = mavenGroup;
    this.mavenPomTemplate = mavenPomTemplate;
    this.mavenRepositoryId = mavenRepositoryId;
    this.mavenUri = mavenUri;
  }

  public String mavenGroup() {
    return mavenGroup;
  }

  public Path mavenPomTemplate() {
    return mavenPomTemplate;
  }

  /** Maven repository id. */
  public String mavenRepositoryId() {
    return mavenRepositoryId;
  }

  /** Maven URL as an URI. */
  public URI mavenUri() {
    return mavenUri;
  }
}
