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
import java.util.*;
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
    Util.log.fine("building...");
    clean();
    format();
    compile();
    test();
    link();
    Util.log.info(() -> "finished after " + (System.currentTimeMillis() - start) + " ms");
  }

  default void clean() {
    Util.log.warning("not implemented, yet");
  }

  default void compile() {
    Util.log.warning("(javac, javadoc, jar) not implemented, yet");
  }

  Configuration configuration();

  default void format() {
    Util.log.warning("not implemented, yet");
  }

  default void link() {
    Util.log.warning("not implemented, yet");
  }

  /** Get path for the specified folder. Same as: {@code configuration().folders().get(folder)} */
  default Path path(Folder folder) {
    return configuration().folders().get(folder);
  }

  /** Resolve named module by downloading its jar artifact from the specified location. */
  default Path resolve(String module, URI uri) {
    Path targetDirectory = path(Folder.DEPENDENCIES);
    return Util.download(uri, targetDirectory, module + ".jar", path -> true);
  }

  default void test() {
    Util.log.warning("not implemented, yet");
  }

  class Builder implements Configuration {
    Map<Folder, Folder.Location> customFolderLocations = new TreeMap<>();
    Map<Folder, Path> folders = buildFolders(Collections.emptyMap());
    Handler handler = new Util.ConsoleHandler(System.out, Level.ALL);
    Level level = Level.FINE;
    String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
    String version = "1.0.0-SNAPSHOT";

    public Bach build() {
      Builder configuration = new Builder();
      configuration.folders = buildFolders(customFolderLocations);
      configuration.handler = handler;
      configuration.level = level;
      configuration.name = name;
      configuration.version = version;
      Util.setHandler(handler);
      Util.log.setLevel(level);
      return new Default(configuration);
    }

    private Map<Folder, Path> buildFolders(Map<Folder, Folder.Location> locations) {
      Function<Folder, Folder.Location> locator =
          folder -> locations.getOrDefault(folder, folder.location);
      Map<Folder, Path> map = new EnumMap<>(Folder.class);
      for (Folder folder : Folder.values()) {
        Folder.Location location = locator.apply(folder);
        Path path = location.path;
        List<Folder> parents = location.parents;
        if (!parents.isEmpty()) {
          Iterator<Folder> iterator = parents.iterator();
          path = locator.apply(iterator.next()).path;
          while (iterator.hasNext()) {
            path = path.resolve(locator.apply(iterator.next()).path);
          }
          path = path.resolve(location.path);
        }
        map.put(folder, path);
      }
      return Collections.unmodifiableMap(map);
    }

    public Builder folder(Folder folder, Folder.Location location) {
      customFolderLocations.put(folder, location);
      folders = buildFolders(customFolderLocations);
      return this;
    }

    public Builder folder(Folder folder, Path path) {
      return folder(folder, Folder.Location.of(folder.location.parents, path));
    }

    @Override
    public Map<Folder, Path> folders() {
      return folders;
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
    default void dump(Consumer<String> stringConsumer) {
      stringConsumer.accept(name() + " " + version());
      stringConsumer.accept("  " + folders());
    }

    Map<Folder, Path> folders();

    String name();

    String version();
  }

  class Default implements Bach {

    final Configuration configuration;

    Default(Configuration configuration) {
      this.configuration = configuration;
      configuration.dump(Util.log::config);
      Util.log.info("initialized");
    }

    @Override
    public Configuration configuration() {
      return configuration;
    }
  }

  enum Folder {
    JDK_HOME(Location.of(Util.findJdkHome())),
    AUXILIARY(Location.of(Paths.get(".bach"))),
    DEPENDENCIES(Location.of(List.of(AUXILIARY), Paths.get("dependencies")));

    public static class Location {
      final List<Folder> parents;
      final Path path;

      private Location(List<Folder> parents, Path path) {
        this.path = path;
        this.parents = parents;
      }

      public static Location of(List<Folder> parents, Path path) {
        return new Location(parents, path);
      }

      public static Location of(Path path) {
        return new Location(Collections.emptyList(), path);
      }
    }

    final Location location;

    Folder(Location location) {
      this.location = location;
    }
  }

  interface Util {

    Logger log = Logger.getLogger("Bach");

    class ConsoleHandler extends StreamHandler {

      public ConsoleHandler(OutputStream stream, Level level) {
        super(stream, new SingleLineFormatter());
        setLevel(level);
      }

      @Override
      public void publish(LogRecord record) {
        super.publish(record);
        flush();
      }

      @Override
      public void close() {
        flush();
      }
    }

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
        builder.append('-');
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        record.getThrown().printStackTrace(pw);
        pw.close();
        builder.append(sw.getBuffer());
        return builder.toString();
      }
    }

    /** Maven uri for jar artifact at {@code http://central.maven.org/maven2} repository. */
    static URI maven(String group, String artifact, String version) {
      return maven("http://central.maven.org/maven2", group, artifact, version, "jar");
    }

    /** Maven uri for specified coordinates. */
    static URI maven(String repo, String group, String artifact, String version, String kind) {
      String path = artifact + '/' + version + '/' + artifact + '-' + version + '.' + kind;
      return URI.create(repo + '/' + group.replace('.', '/') + '/' + path);
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
                log.fine(() -> "skipped, using `" + target + "`");
                return target;
              }
            }
          }
          Files.delete(target);
        }
        log.fine(() -> "transferring `" + uri + "`...");
        try (InputStream sourceStream = url.openStream();
            OutputStream targetStream = Files.newOutputStream(target)) {
          sourceStream.transferTo(targetStream);
        }
        Files.setLastModifiedTime(target, urlLastModifiedTime);
        log.info(() -> "stored `" + target + "` [" + urlLastModifiedTime + "]");
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
      Path executable = ProcessHandle.current().info().command().map(Paths::get).orElse(null);
      if (executable != null && executable.getNameCount() > 2) {
        // noinspection ConstantConditions -- count is 3 or higher: "<JDK_HOME>/bin/java[.exe]"
        return executable.getParent().getParent().toAbsolutePath();
      }
      String jdkHome = System.getenv("JDK_HOME");
      if (jdkHome != null) {
        return Paths.get(jdkHome).toAbsolutePath();
      }
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome != null) {
        return Paths.get(javaHome).toAbsolutePath();
      }
      Path fallback = Paths.get("jdk-" + Runtime.version().major()).toAbsolutePath();
      log.warning("path of JDK not found, using: " + fallback);
      return fallback;
    }

    static void setHandler(Handler handler) {
      for (Handler remove : log.getHandlers()) {
        log.removeHandler(remove);
      }
      if (handler == null) {
        log.setUseParentHandlers(true);
        return;
      }
      log.addHandler(handler);
      log.setUseParentHandlers(false);
    }
  }
}
