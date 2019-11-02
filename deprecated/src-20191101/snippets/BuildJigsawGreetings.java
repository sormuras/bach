class Build {
  public static void main(String[] args) throws Exception {
    var classes = java.nio.file.Path.of("bin/classes");
    var modules = java.nio.file.Path.of("bin/modules");

    run("javac", "-d", classes, "--module-source-path", "src", "--module", "com.greetings");

    java.nio.file.Files.createDirectories(modules);
    run(
        "jar",
        "--create",
        "--file",
        modules.resolve("com.greetings-1.0.jar"),
        "--module-version",
        "1.0",
        "--main-class",
        "com.greetings.Main",
        "-C",
        classes.resolve("com.greetings"),
        ".");
    run("jar", "--describe-module", "--file", modules.resolve("com.greetings-1.0.jar"));
    run("jdeps", "--module-path", modules, "--check", "com.greetings");
    var java = ProcessHandle.current().info().command().orElse("java");
    start(java, "--module-path", modules, "--module", "com.greetings");
  }

  static void run(String name, Object... args) {
    var line = name + (args.length == 0 ? "" : " " + String.join(" ", strings(args)));
    System.out.println("| " + line);
    var tool = java.util.spi.ToolProvider.findFirst(name).orElseThrow();
    var code = tool.run(System.out, System.err, strings(args));
    if (code != 0) throw new Error(code + " <- " + line);
  }

  static void start(Object... command) throws Exception {
    var line = String.join(" ", strings(command));
    System.out.println("| " + line);
    var process = new ProcessBuilder(strings(command)).inheritIO().start();
    var code = process.waitFor();
    if (code != 0) throw new Error(code + " <- " + line);
  }

  static String[] strings(Object... objects) {
    var strings = new java.util.ArrayList<String>();
    for(int i = 0; i < objects.length; i++) strings.add(objects[i].toString());
    return strings.toArray(String[]::new);
  }
}
