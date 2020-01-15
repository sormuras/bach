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

import de.sormuras.bach.Bach.Default;
import de.sormuras.bach.Log;
import de.sormuras.bach.util.Paths;
import java.lang.module.ModuleDescriptor.Version;
import java.util.Optional;
import java.util.function.Function;

/** Mutable project configuration. */
public class Configuration {

  public static Configuration of() {
    return new Configuration(Folder.of());
  }

  public static Configuration of(String name, String version) {
    return of(name, name, version);
  }

  public static Configuration of(String name, String group, String version) {
    return of().setName(name).setGroup(group).setVersion(Version.parse(version));
  }

  /** Property enumeration backed by {@linkplain System#getProperties()} values. */
  enum Property {
    PROJECT_NAME,
    PROJECT_GROUP,
    PROJECT_VERSION;

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

  private Log log;
  private String name;
  private String group;
  private Version version;
  private Library library;

  private int mainRelease;
  private int testRelease;
  private boolean mainPreview;
  private boolean testPreview;

  public Configuration(Folder folder) {
    this.folder = folder;

    setLog(Log.ofSystem());
    setName(Property.PROJECT_NAME.get(Paths.name(folder.base(), Default.PROJECT_NAME)));
    setGroup(Property.PROJECT_GROUP.get(getName()));
    setVersion(Property.PROJECT_VERSION.ifPresent(Version::parse).orElse(Default.PROJECT_VERSION));
    setLibrary(Library.of());

    setMainRelease(0);
    setMainPreview(false);
    setTestRelease(Runtime.version().feature());
    setTestPreview(true);
  }

  public Folder getFolder() {
    return folder;
  }

  public Log getLog() {
    return log;
  }

  public Configuration setLog(Log log) {
    this.log = log;
    return this;
  }

  public String getName() {
    return name;
  }

  public Configuration setName(String name) {
    this.name = name;
    return this;
  }

  public String getGroup() {
    return group;
  }

  public Configuration setGroup(String group) {
    this.group = group;
    return this;
  }

  public Version getVersion() {
    return version;
  }

  public Configuration setVersion(Version version) {
    this.version = version;
    return this;
  }

  public Library getLibrary() {
    return library;
  }

  public Configuration setLibrary(Library library) {
    this.library = library;
    return this;
  }

  public int getMainRelease() {
    return mainRelease;
  }

  public Configuration setMainRelease(int mainRelease) {
    this.mainRelease = mainRelease;
    return this;
  }

  public int getTestRelease() {
    return testRelease;
  }

  public Configuration setTestRelease(int testRelease) {
    this.testRelease = testRelease;
    return this;
  }

  public boolean isMainPreview() {
    return mainPreview;
  }

  public Configuration setMainPreview(boolean mainPreview) {
    this.mainPreview = mainPreview;
    return this;
  }

  public boolean isTestPreview() {
    return testPreview;
  }

  public Configuration setTestPreview(boolean testPreview) {
    this.testPreview = testPreview;
    return this;
  }
}
