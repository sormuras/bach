//usr/bin/env jshell --show-version "$0" "$@"; exit $?

/open ../../src/main/java/Bach.java

Bach bach = new Bach()

bach.javac(options -> {
  options.moduleSourcePaths = List.of(Paths.get("src"), Paths.get("src-de"), Paths.get("src-fr"));
  return options;
})

// jar --create --file foo.jar --main-class com.foo.Main --module-version 1.0 -C foo/classes resources
Path jarred = Paths.get("target", "jarred")
Files.createDirectories(jarred)
bach.jar(options -> {
  options.file = jarred.resolve("world.jar");
  options.main = "com.greetings.Main";
  options.version = "1.0";
  options.path = bach.folder.resolveTargetMods().resolve("world");
  return options;
}, ".")

bach.java(options -> {
  options.modulePaths = List.of(jarred);
  options.module = "world";
  return options;
})

/exit
