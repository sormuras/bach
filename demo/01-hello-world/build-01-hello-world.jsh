//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/open ../../src/bach/Bach.java

var bach = new Bach()

var javac = new JdkTool.Javac()
javac.destination = Paths.get("target", "mods")
javac.moduleSourcePath = List.of(Paths.get("src"), Paths.get("src-de"), Paths.get("src-fr"))

bach.run("[compile]", javac.toCommand(bach))
bach.run("java", "--module-path", javac.destination, "--module", "world/com.greetings.Main")

/exit
