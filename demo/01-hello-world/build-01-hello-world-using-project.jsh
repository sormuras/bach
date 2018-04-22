//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/open ../../src/bach/Bach.java

var sources = List.of(Paths.get("src"), Paths.get("src-de"), Paths.get("src-fr"))
var project = Project.builder().mainModule("world").mainClass("com.greetings.Main").newModuleGroup("main").moduleSourcePath(sources).end().build()

var bach = new Bach()
bach.run("compile", new Task.CompilerTask(bach, project))
bach.run("run", new Task.RunnerTask(bach, project))

/exit
