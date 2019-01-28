//usr/bin/env jshell --show-version "$0" "$@"; exit $?

/open ../../src/bach/Bach.java

var bach = new Bach()

var destination = Path.of("target", "mods")
var moduleSourcePath = List.of(Path.of("src"), Path.of("src-de"), Path.of("src-fr"))

var javac = bach.command("javac")
javac.add("-d").add(destination)
javac.add("--module-source-path").add(moduleSourcePath)
javac.addAllJavaFiles(moduleSourcePath)

bach.run(javac)
bach.run("java", "--module-path", destination, "--module", "world/com.greetings.Main")

/exit
