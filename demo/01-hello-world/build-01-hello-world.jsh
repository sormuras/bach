//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/open ../../Bach.java

Bach bach = new Bach()
// bach.project.mains.put("world", "com.greetings.Main")
// bach.project.versions.put("hallo", "1.2")
// bach.project.versions.put("hello", "2.3")
// bach.project.versions.put("world", "3.2.1")

JdkTool.Javac javac = new JdkTool.Javac()
javac.destinationPath = Paths.get("target", "mods")
javac.moduleSourcePath = List.of(Paths.get("src"), Paths.get("src-de"), Paths.get("src-fr"))
javac.run()

// bach.worker.buildJarForEachModule()

JdkTool.run("java", "--module-path", javac.destinationPath, "--module", "world/com.greetings.Main")

/exit
