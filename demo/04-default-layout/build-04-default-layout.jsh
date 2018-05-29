/open ../../src/bach/Bach.java

var bach = new Bach()
var project = Project.newProject(".")

bach.run("compile", new Task.CompilerTask(bach, project))
bach.run("run", new Task.RunnerTask(bach, project))

/exit
