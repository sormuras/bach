//usr/bin/env jshell "$0" "$@"; exit $?

System.out.println("Java " + Runtime.version())

/open ../../../src/bach/Bach.java
// var bach = new Bach()
// System.out.println("build.jsh using " + bach)

/open .bach\src\Build.java
Build.main()

/open ../../../BUILDING
var classes = Path.of(".bach/build/classes")
var modules = Path.of(".bach/build/modules")
run("javac", new Arguments().add("--module", "com.greetings").add("-d", classes).add("--module-source-path", ".").toArray())
Files.createDirectories(modules)
run("jar", new Arguments().add("--create").add("--file", modules.resolve("com.greetings.jar")).add("--main-class", "com.greetings.Main").add("-C", classes.resolve("com.greetings")).add(".").toArray())
exe("java", new Arguments().add("--module-path", modules).add("--module", "com.greetings").toArray())

/exit
