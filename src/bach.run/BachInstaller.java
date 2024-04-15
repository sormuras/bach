/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

import java.net.*;
import java.nio.file.*;
import java.util.*;

record BachInstaller(String version, Path home) {
  static String DEFAULT_VERSION = System.getProperty("-Default.version".substring(2), "main");
  static Path DEFAULT_HOME = Path.of(System.getProperty("-Default.home".substring(2), ".bach"));

  @SuppressWarnings("unused")
  static void installDefaultVersionIntoDefaultDirectory() throws Exception {
    var installer = new BachInstaller(DEFAULT_VERSION);
    installer.install();
  }

  @SuppressWarnings("unused")
  static void listInstallableVersions() {
    System.out.println("- Default version: " + DEFAULT_VERSION);
    System.out.println("- Released versions: https://github.com/sormuras/run.bach/releases");
    System.out.println("- Head revisions: https://github.com/sormuras/run.bach/branches");
  }

  BachInstaller(String version) {
    this(version, DEFAULT_HOME);
  }

  void install() throws Exception {
    var uris =
        List.of(
            "https://github.com/sormuras/run.bach/archive/refs/tags/" + version + ".zip",
            "https://github.com/sormuras/run.bach/archive/refs/heads/" + version + ".zip");
    for (var uri : uris) {
      if (Internal.head(uri)) {
        install(uri);
        return;
      }
    }
  }

  void install(String uri) throws Exception {
    System.out.println("Install Bach " + version + " to " + home.toUri() + "...");
    var tmp = Files.createTempDirectory("run.bach-" + version + "-");
    var dir = Files.createDirectories(home.resolve("src/run.bach/run/bach"));
    var zip = tmp.resolve("run.bach-" + version + ".zip");
    // download and unzip
    Internal.copy(uri, zip, StandardCopyOption.REPLACE_EXISTING);
    Internal.unzip(zip, dir, 1);

    // clean up
    Internal.delete(tmp);
  }

  private interface Internal {
    boolean DEBUG = Boolean.getBoolean("-Debug".substring(2));

    static void debug(String message) {
      if (DEBUG) System.out.println(message);
    }

    static boolean head(String source) throws Exception {
      var url = URI.create(source).toURL();
      var con = (HttpURLConnection) url.openConnection();
      try {
        con.setRequestMethod("HEAD");
        var status = con.getResponseCode();
        debug("%d <- HEAD %s".formatted(status, source));
        if (status < 299) return true;
      } finally {
        con.disconnect();
      }
      return false;
    }

    static void copy(String source, Path target, CopyOption... options) throws Exception {
      debug("<< %s".formatted(source));
      Files.createDirectories(target.getParent());
      try (var stream =
          source.startsWith("http")
              ? URI.create(source).toURL().openStream()
              : Files.newInputStream(Path.of(source))) {
        var size = Files.copy(stream, target, options);
        debug(">> %,7d %s".formatted(size, target.getFileName()));
      }
    }

    static void delete(Path path) throws Exception {
      var start = path.normalize().toAbsolutePath();
      if (Files.notExists(start)) return;
      for (var root : start.getFileSystem().getRootDirectories()) {
        if (start.equals(root)) {
          debug("deletion of root directory?! " + path);
          return;
        }
      }
      debug("delete directory tree " + start);
      try (var stream = Files.walk(start)) {
        var files = stream.sorted((p, q) -> -p.compareTo(q));
        for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
      }
    }

    static void unzip(Path zip, Path dir, int sub) throws Exception {
      debug("<< %s".formatted(zip.toUri()));
      debug(">> %s".formatted(dir.toUri()));
      var files = new ArrayList<Path>();
      try (var fs = FileSystems.newFileSystem(zip)) {
        for (var root : fs.getRootDirectories()) {
          try (var stream = Files.walk(root)) {
            var list = stream.filter(Files::isRegularFile).toList();
            for (var file : list) {
              var relative = root.relativize(file);
              var source = sub == 0 ? relative : relative.subpath(sub, relative.getNameCount());
              var target = dir.resolve(source.toString());
              debug(">> - " + target.toUri());
              Files.createDirectories(target.getParent());
              Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
              files.add(target);
            }
          }
        }
      }
      debug(">> %d files unzipped".formatted(files.size()));
    }
  }

  public static void main(String... args) throws Exception {
    installDefaultVersionIntoDefaultDirectory();
  }
}
