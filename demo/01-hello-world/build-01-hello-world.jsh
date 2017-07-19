//usr/bin/env jshell --show-version "$0" "$@"; exit $?

/open ../../src/main/java/Bach.java

Bach bach = new Bach()
bach.project.mains.put("world", "com.greetings.Main")
bach.project.versions.put("hallo", "1.2.3")
bach.project.versions.put("hello", "2.3")

bach.javac(options -> {
  options.moduleSourcePaths = List.of(Paths.get("src"), Paths.get("src-de"), Paths.get("src-fr"));
  return options;
})

bach.worker.buildJarForEachModule()

bach.call("java", "--module-path", bach.project.resolveTargetJarred(), "--module", "world")

/exit
