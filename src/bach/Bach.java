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
import java.lang.module.ModuleDescriptor.Version;
import java.util.Objects;
/** Bach - Java Shell Builder. */
public class Bach {
  /** Version of Bach. */
  public static Version VERSION = Version.parse("11.0-ea");
  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/Main.java
  /** Bach's main program. */
  static class Main {
    public static void main(String... args) {
      System.out.println("Bach.java " + Bach.VERSION);
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/model/Project.java
  /** Bach's project model. */
  public static final class Project {
    private final String name;
    private final Version version;
    public Project(String name, Version version) {
      this.name = Objects.requireNonNull(name, "name");
      this.version = version;
    }
    public String name() {
      return name;
    }
    public Version version() {
      return version;
    }
  }
  // src/de.sormuras.bach/main/java/de/sormuras/bach/model/ProjectBuilder.java
  /** Project model builder. */
  public static final class ProjectBuilder {
    private String name;
    private Version version;
    public ProjectBuilder name(String name) {
      this.name = name;
      return this;
    }
    public ProjectBuilder version(Version version) {
      this.version = version;
      return this;
    }
    public Project build() {
      return new Project(name, version);
    }
  }
}
