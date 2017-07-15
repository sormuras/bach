//usr/bin/env jshell --show-version "$0" "$@"; exit $?

/open ../../src/main/java/Bach.java

Bach bach = new Bach()

bach.javac(options -> {
  options.moduleSourcePaths = List.of(Paths.get("src"), Paths.get("src-de"), Paths.get("src-fr"));
  return options;
})

bach.java(options -> {
  options.module = "world/com.greetings.Main";
  return options;
})

/exit
