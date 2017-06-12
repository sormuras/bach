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
import java.time.*;
import java.time.format.*;
import java.util.function.*;
import java.util.logging.*;

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
    long start = System.currentTimeMillis();
    logger.fine("building...");
    clean();
    format();
    compile();
    test();
    link();
    logger.info(() -> "finished after " + (System.currentTimeMillis() - start) + " ms");
  }

  static Builder builder() {
    return new Builder();
  }

  default void clean() {
    logger.warning("not implemented, yet");
  }

  default void compile() {
    logger.warning("(javac, javadoc, jar) not implemented, yet");
  }

  Configuration configuration();

  default void format() {
    logger.warning("not implemented, yet");
  }

  default void link() {
    logger.warning("not implemented, yet");
  }

  /** Resolve named module by downloading its jar artifact from the specified location. */
  default Path resolve(String module, URI uri) {
    Path targetDirectory = Paths.get(".bach", "dependencies");
    return Util.download(uri, targetDirectory, module + ".jar", path -> true);
  }

  default void test() {
    logger.warning("not implemented, yet");
  }

  class Builder implements Configuration {
    Handler handler = buildHandler();
    Level level = Level.FINE;
    String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
    String version = "1.0.0-SNAPSHOT";

    public Bach build() {
      Builder configuration = new Builder();
      configuration.name = name;
      configuration.version = version;
      if (handler != null && logger.getHandlers().length == 0) {
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
      }
      logger.setLevel(level);
      return new Default(configuration);
    }

    private Handler buildHandler() {
      Handler handler = new ConsoleHandler();
      handler.setLevel(Level.ALL);
      handler.setFormatter(new Util.SingleLineFormatter());
      return handler;
    }

    public Builder handler(Handler handler) {
      this.handler = handler;
      return this;
    }

    public Builder level(Level level) {
      this.level = level;
      return this;
    }

    @Override
    public String name() {
      return name;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
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
      logger.fine("initialized");
    }

    @Override
    public Configuration configuration() {
      return configuration;
    }
  }

  interface Util {

    class SingleLineFormatter extends java.util.logging.Formatter {

      private final DateTimeFormatter instantFormatter =
              DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS").withZone(ZoneId.systemDefault());

      @Override
      public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append(instantFormatter.format(record.getInstant()));
        builder.append(' ');
        builder.append(record.getSourceClassName());
        builder.append(' ');
        builder.append(record.getSourceMethodName());
        builder.append(' ');
        builder.append(formatMessage(record));
        builder.append(System.lineSeparator());
        if (record.getThrown() == null) {
          return builder.toString();
        }
        builder.append(System.lineSeparator());
        builder.append(' ');
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        record.getThrown().printStackTrace(pw);
        pw.close();
        builder.append(sw.getBuffer());
        return builder.toString();
      }
    }

    /** Download the resource specified by its URI to the target directory. */
    static Path download(URI uri, Path targetDirectory) {
      return Util.download(uri, targetDirectory, extractFileName(uri), path -> true);
    }

    /** Download the resource from URI to the target directory using the provided file name. */
    static Path download(URI uri, Path directory, String fileName, Predicate<Path> skip) {
      try {
        URL url = uri.toURL();
        Files.createDirectories(directory);
        Path target = directory.resolve(fileName);
        URLConnection urlConnection = url.openConnection();
        FileTime urlLastModifiedTime = FileTime.fromMillis(urlConnection.getLastModified());
        if (Files.exists(target)) {
          if (Files.getLastModifiedTime(target).equals(urlLastModifiedTime)) {
            if (Files.size(target) == urlConnection.getContentLengthLong()) {
              if (skip.test(target)) {
                logger.fine(() -> "skipped, using `" + target + "`");
                return target;
              }
            }
          }
          Files.delete(target);
        }
        logger.fine(() -> "transferring `" + uri + "`...");
        try (InputStream sourceStream = url.openStream();
            OutputStream targetStream = Files.newOutputStream(target)) {
          sourceStream.transferTo(targetStream);
        }
        Files.setLastModifiedTime(target, urlLastModifiedTime);
        logger.info(() -> "stored `" + target + "` [" + urlLastModifiedTime + "]");
        return target;
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

  Logger logger = Logger.getLogger("Bach");
}
