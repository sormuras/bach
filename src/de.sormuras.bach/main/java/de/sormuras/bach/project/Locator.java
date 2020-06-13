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

import de.sormuras.bach.internal.Maven;

/** A module-uri pair used to resolve external modules. */
public final class Locator implements Comparable<Locator> {

  public static Resource of(String module) {
    return new Resource(module);
  }

  private final String module;
  private final String uri;

  public Locator(String module, String uri) {
    this.module = module;
    this.uri = uri;
  }

  public String module() {
    return module;
  }

  public String uri() {
    return uri;
  }

  @Override
  public int compareTo(Locator other) {
    return module.compareTo(other.module);
  }

  public static class Resource {
    private final String module;

    public Resource(String module) {
      this.module = module;
    }

    /**
     * Create a new locator pointing to an artifact built by JitPack.
     *
     * @param user GitHub username or the complete group like {@code "com.azure.${USER}"}
     * @param repository Name of the repository or project
     * @param version The version string of the repository or project, which is either a release
     *     tag, a commit hash, or {@code "${BRANCH}-SNAPSHOT"} for a version that has not been
     *     released.
     * @return A new JitPack-based {@code Locator} instance
     * @see <a href="https://jitpack.io/docs">jitpack.io</a>
     */
    public Locator fromJitPack(String user, String repository, String version) {
      var group = user.indexOf('.') == -1 ? "com.github." + user : user;
      var joiner = Maven.Joiner.of(group, repository, version);
      return new Locator(module, joiner.repository("https://jitpack.io").toString());
    }

    /**
     * Create a new locator pointing to an artifact hosted at Maven Central.
     *
     * @param group Maven Group ID
     * @param artifact Maven Artifact ID
     * @param version The version string
     * @return A new Maven Central-based {@code Locator} instance
     * @see <a href="https://search.maven.org">search.maven.org</a>
     */
    public Locator fromMavenCentral(String group, String artifact, String version) {
      return new Locator(module, Maven.central(group, artifact, version));
    }
  }
}
