//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/open ../../src/bach/Bach.java
/open ../../src/bach/Bach.jsh

Bach.JdkTool.Javac javac = new Bach.JdkTool.Javac()
javac.destinationPath = Paths.get("target", "mods")
javac.moduleSourcePath = List.of(Paths.get("src"), Paths.get("src-de"), Paths.get("src-fr"))
javac.run()

java("--module-path", javac.destinationPath, "--module", "world/com.greetings.Main")

/exit
