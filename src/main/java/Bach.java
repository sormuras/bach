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

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.function.*;

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

  /** Resolve named module by downloading its jar artifact from the specified location. */
  default Path resolve(String module, URI uri) {
    Path targetDirectory = Paths.get(".bach", "dependencies");
    return Util.download(uri, targetDirectory, module + ".jar", path -> true);
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

  interface Util {

    /** Download the resource specified by its URI to the target directory. */
    static Path download(URI uri, Path targetDirectory) {
      return Util.download(uri, targetDirectory, extractFileName(uri), path -> true);
    }

    /** Download the resource from URI to the target directory using the provided file name. */
    static Path download(
        URI uri, Path targetDirectory, String targetFileName, Predicate<Path> skip) {
      try {
        URL url = uri.toURL();
        Files.createDirectories(targetDirectory);
        Path targetPath = targetDirectory.resolve(targetFileName);
        URLConnection urlConnection = url.openConnection();
        FileTime urlLastModifiedTime = FileTime.fromMillis(urlConnection.getLastModified());
        if (Files.exists(targetPath)) {
          if (Files.getLastModifiedTime(targetPath).equals(urlLastModifiedTime)) {
            if (Files.size(targetPath) == urlConnection.getContentLengthLong()) {
              if (skip.test(targetPath)) {
                // TODO log.fine("download skipped - using `%s`", targetPath);
                return targetPath;
              }
            }
          }
          Files.delete(targetPath);
        }
        // TODO log.fine("download `%s` in progress...", uri);
        try (InputStream sourceStream = url.openStream();
            OutputStream targetStream = Files.newOutputStream(targetPath)) {
          sourceStream.transferTo(targetStream);
        }
        Files.setLastModifiedTime(targetPath, urlLastModifiedTime);
        // TODO log.fine("download `%s` completed", uri);
        // TODO log.info("stored `%s` [%s]", targetPath, urlLastModifiedTime.toString());
        return targetPath;
      } catch (IOException e) {
        throw new Error("should not happen", e);
      }
    }

    /** Extract the file name from the uri. */
    static String extractFileName(URI uri) {
      String urlString = uri.getPath();
      int begin = urlString.lastIndexOf('/') + 1;
      return urlString.substring(begin).split("\\?")[0].split("#")[0];
    }

    /** Return path to JDK installation directory. */
    static Path findJdkHome() {
      // extract path from current process information: <JDK_HOME>/bin/java[.exe]
      Path executable = ProcessHandle.current().info().command().map(Paths::get).orElse(null);
      if (executable != null) {
        Path path = executable.getParent(); // <JDK_HOME>/bin
        if (path != null) {
          return path.getParent(); // <JDK_HOME>
        }
      }
      // next, examine system environment...
      String jdkHome = System.getenv("JDK_HOME");
      if (jdkHome != null) {
        return Paths.get(jdkHome);
      }
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome != null) {
        return Paths.get(javaHome);
      }
      // still here? not so good... try with default (not-existent) path
      return Paths.get("jdk-" + Runtime.version().major());
    }
  }
}
