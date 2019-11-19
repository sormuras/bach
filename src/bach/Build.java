class Build {
  public static void main(String... args) throws Exception {
    var version = "master-SNAPSHOT";
    System.out.println("Build with Bach.java " + version);
    load(
        "de.sormuras.bach",
        version,
        java.net.URI.create(
            "https://jitpack.io/com/github/sormuras/bach/"
                + version
                + "/bach-"
                + version
                + ".jar"));
    start(
        ProcessHandle.current().info().command().orElse("java"),
        "--module-path=.bach/build/lib",
        "-D.bach/project.version=2.0-M2",
        "--module",
        "de.sormuras.bach/de.sormuras.bach.Bach",
        "build");
  }

  static void load(String module, String version, java.net.URI uri) throws Exception {
    var lib = java.nio.file.Path.of(".bach/build/lib");
    var target = lib.resolve(module + '-' + version + ".jar");
    if (java.nio.file.Files.exists(target) && !version.endsWith("SNAPSHOT")) return;
    System.out.printf("  %s%n", target);
    java.nio.file.Files.createDirectories(lib);
    try (var sourceStream = uri.toURL().openStream();
        var targetStream = java.nio.file.Files.newOutputStream(target)) {
      sourceStream.transferTo(targetStream);
    }
  }

  static void start(String... command) throws Exception {
    System.out.printf("| %s%n", String.join(" ", command));
    var process = new ProcessBuilder(command).inheritIO().start();
    int code = process.waitFor();
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }
}
