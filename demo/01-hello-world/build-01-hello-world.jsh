//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/open ../../src/bach/Bach.java

var javac = new JdkTool.Javac()
javac.destination = Paths.get("target", "mods")
javac.moduleSourcePath = List.of(Paths.get("src"), Paths.get("src-de"), Paths.get("src-fr"))

var bach = new Bach()
bach.run(javac)
bach.run("java", "--module-path", javac.destination, "--module", "world/com.greetings.Main")

/exit
