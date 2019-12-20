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

import de.sormuras.bach.Bach.Default;
import de.sormuras.bach.util.Paths;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

/** Mutable project configuration. */
public class Configuration {

  public static Configuration of() {
    return new Configuration(Folder.of());
  }

  public static Configuration of(String name, String version) {
    return of().setName(name).setVersion(Version.parse(version));
  }

  /** Property enumeration backed by {@linkplain System#getProperties()} values. */
  enum Property {
    PROJECT_NAME,
    PROJECT_VERSION,

    MAVEN_GROUP,
    MAVEN_REPOSITORY_ID,
    MAVEN_REPOSITORY_URL;

    private final String key = name().toLowerCase().replace('_', '.');

    String get() {
      return System.getProperty(key);
    }

    String get(String defaultValue) {
      return System.getProperty(key, defaultValue);
    }

    <V> Optional<V> ifPresent(Function<String, V> mapper) {
      return Optional.ofNullable(get()).map(mapper);
    }
  }

  private final Folder folder;

  private String name;
  private Version version;

  private Deployment deployment;

  public Configuration(Folder folder) {
    this.folder = folder;

    setName(Property.PROJECT_NAME.get(Paths.name(folder.base(), Default.PROJECT_NAME)));
    setVersion(Property.PROJECT_VERSION.ifPresent(Version::parse).orElse(Default.PROJECT_VERSION));

    setDeployment(
        Property.MAVEN_GROUP.get(getName()),
        Property.MAVEN_REPOSITORY_ID.get(),
        Property.MAVEN_REPOSITORY_URL.get());
  }

  public Folder getFolder() {
    return folder;
  }

  public String getName() {
    return name;
  }

  public Configuration setName(String name) {
    this.name = name;
    return this;
  }

  public Version getVersion() {
    return version;
  }

  public Configuration setVersion(Version version) {
    this.version = version;
    return this;
  }

  public Deployment getDeployment() {
    return deployment;
  }

  public Configuration setDeployment(Deployment deployment) {
    this.deployment = deployment;
    return this;
  }

  public Configuration setDeployment(String group, String repository, String url) {
    var mavenPom = folder.src(Default.MAVEN_POM_TEMPLATE);
    var mavenUri = url != null ? URI.create(url) : null;
    return setDeployment(new Deployment(group, mavenPom, repository, mavenUri));
  }
}
