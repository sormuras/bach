/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

// TODO import module java.base;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

@SuppressWarnings("unused")
interface Bach {
  static void build() {
    /* java @build */ {
      var build = Path.of("build");
      if (Files.isRegularFile(build)) {
        System.out.println("Running java @build ...");
        Internal.java("@" + build);
        return;
      }
      System.out.println("Running java @build not possible.");
      System.out.println("  Create a `build` argument file to enable it.");
    }
    /* java Build.java */ {
      var candidates =
          List.of(
              Path.of("Build.java"),
              Path.of(".bach/Build.java"),
              Path.of(".bach/src/Build.java"),
              Path.of(".bach/src/run/Build.java"),
              Path.of(".bach/src/run.bach/run/Build.java"));
      for (var build : candidates) {
        if (Files.isRegularFile(build)) {
          System.out.println("Running java " + build + " ...");
          Internal.java(build.toString());
          return;
        }
      }
      System.out.println("Running java Build.java not possible.");
      System.out.println("  Create a `Build.java` file to enable it.");
    }
    // zero-configuration
    System.out.println("TODO: java @bach build");
    System.out.println("TODO: java .bach/src/run/bach/Main.java build");
    System.out.println("TODO: java .bach/src/run.bach/run/bach/Main.java build");
    // zero-installation + zero-configuration
    System.out.println("TODO: init(temp) && java ${temp}/.bach/src/run/bach/Main.java build");
  }

  static void init() {
    new Installer().install();
  }

  static void status() {
    var directory = Path.of("");
    System.out.printf(
        """
           ___      ___      ___      ___
          /\\  \\    /\\  \\    /\\  \\    /\\__\\
         /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_
        /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\
        \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  / Java %s
         \\::/  /   /:/  /  \\:\\__\\    /:/  / %s
          \\/__/    \\/__/    \\/__/    \\/__/ %s
        """,
        Runtime.version(), System.getProperty("os.name"), directory.toUri());

    System.out.println("\nModule declarations");
    var matcher = directory.getFileSystem().getPathMatcher("glob:**module-info.java");
    try (var stream = Files.find(directory, 9, (p, _) -> matcher.matches(p))) {
      for (var path : stream.toList()) {
        System.out.println(" -> " + path.toUri());
      }
    } catch (Exception exception) {
      throw new RuntimeException("Find in %s failed".formatted(directory), exception);
    }
  }

  interface Internal {
    boolean DEBUG = Boolean.getBoolean("-Debug".substring(2));

    static void debug(String message) {
      if (DEBUG) System.out.println(message);
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

    static void java(String... args) {
      var java = Path.of(System.getProperty("java.home"), "bin", "java" /*.exe*/);
      var code = run(List.of(java.toString()), args);
      if (code == 0) return;
      throw new RuntimeException("Non-zero error code: " + code);
    }

    static int run(List<String> command, String... arguments) {
      debug("| " + String.join(" ", command));
      var out = System.out;
      var err = System.err;
      record LinePrinter(InputStream stream, PrintStream writer) implements Runnable {
        @Override
        public void run() {
          new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
        }
      }
      var processBuilder = new ProcessBuilder(new ArrayList<>(command));
      processBuilder.command().addAll(List.of(arguments));
      try {
        var process = processBuilder.start();
        var threadBuilder = Thread.ofVirtual();
        threadBuilder.name("-out").start(new LinePrinter(process.getInputStream(), out));
        threadBuilder.name("-err").start(new LinePrinter(process.getErrorStream(), err));
        return process.isAlive() ? process.waitFor() : process.exitValue();
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        return -1;
      } catch (Exception exception) {
        exception.printStackTrace(err);
        return 1;
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
              // debug(target.toUri().toString());
              Files.createDirectories(target.getParent());
              Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
              files.add(target);
            }
          }
        }
      }
      debug(">> %d files copied".formatted(files.size()));
    }
  }

  record Installer(String version, Path home, Path path) {
    // defaults to git head reference of the `main` branch
    static String VERSION = System.getProperty("-Dversion".substring(2), "main");
    // defaults to the current working directory
    static Path HOME = Path.of(System.getProperty("-Dhome".substring(2), ""));
    // git submodule add <repository> [<path>] <- .bach/src[/run.bach]/run/bach
    static Path PATH = Path.of(System.getProperty("-Dpath".substring(2), ".bach/src/run/bach"));

    Installer() {
      this(VERSION);
    }

    Installer(String version) {
      this(version, HOME, PATH);
    }

    void install() {
      try {
        installSources();
        installArgumentFiles();
      } catch (Exception exception) {
        System.err.println("Install failed: " + exception.getMessage());
      }
    }

    void installSources() throws Exception {
      var uris =
          List.of(
              "https://github.com/sormuras/run.bach/archive/refs/tags/" + version + ".zip",
              "https://github.com/sormuras/run.bach/archive/refs/heads/" + version + ".zip");
      for (var uri : uris) {
        if (Internal.head(uri)) {
          installSourcesFromUri(uri);
          return;
        }
      }
    }

    void installSourcesFromUri(String uri) throws Exception {
      var tmp = Files.createTempDirectory("run.bach-" + version + "-");
      var dir = Files.createDirectories(home.resolve(path));
      var zip = tmp.resolve("run.bach-" + version + ".zip");
      System.out.println("Installing Bach [" + version + "] into " + path.toUri() + "...");
      // download and unzip
      Internal.copy(uri, zip, StandardCopyOption.REPLACE_EXISTING);
      Internal.unzip(zip, dir, 1);
      // clean up
      Internal.delete(tmp);
    }

    void installArgumentFiles() throws Exception {
      var bach = home.resolve("bach");
      if (!Files.exists(bach)) {
        var program = home.resolve(path).resolve("Main.java");
        var command = home.relativize(program).toString().replace('\\', '/');
        var lines = List.of("# Argument file for launching Bach's main application", command);
        Files.write(bach, lines);
      }
    }
  }
}
