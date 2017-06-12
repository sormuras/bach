/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

// default package

import java.nio.file.*;

/** JShell Builder. */
@SuppressWarnings({
  "WeakerAccess",
  "RedundantIfStatement",
  "UnusedReturnValue",
  "SameParameterValue",
  "SimplifiableIfStatement"
})
public interface Bach {

  default void build() {
    clean();
    format();
    compile();
    test();
    link();
  }

  static Builder builder() {
    return new Builder();
  }

  default void clean() {
    System.out.println("clean not implemented, yet");
  }

  default void compile() {
    System.out.println("compile (javac, javadoc, jar) not implemented, yet");
  }

  Configuration configuration();

  default void format() {
    System.out.println("format not implemented, yet");
  }

  default void link() {
    System.out.println("link not implemented, yet");
  }

  default void test() {
    System.out.println("test not implemented, yet");
  }

  class Builder implements Configuration {
    String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
    String version = "1.0.0-SNAPSHOT";

    public Bach build() {
      Builder configuration = new Builder();
      configuration.name = name;
      configuration.version = version;
      return new Default(configuration);
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String version() {
      return version;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }
  }

  interface Configuration {
    String name();

    String version();
  }

  class Default implements Bach {

    final Configuration configuration;

    Default(Configuration configuration) {
      this.configuration = configuration;
    }

    @Override
    public Configuration configuration() {
      return configuration;
    }
  }
}
