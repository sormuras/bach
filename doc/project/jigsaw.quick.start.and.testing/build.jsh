/open ../../../BUILDING
var classes = Path.of(".bach/build/classes/main")
var modules = Path.of(".bach/build/modules/main")
run("javac", new Arguments().add("--module", "com.greetings,org.astro").add("-d", classes).add("--module-source-path", "./*/src/main/java").toArray())
Files.createDirectories(modules)
run("jar", new Arguments().add("--create").add("--file", modules.resolve("com.greetings.jar")).add("--main-class", "com.greetings.Main").add("-C", classes.resolve("com.greetings")).add(".").toArray())
run("jar", new Arguments().add("--create").add("--file", modules.resolve("org.astro.jar")).add("-C", classes.resolve("org.astro")).add(".").toArray())
exe("java", new Arguments().add("--module-path", modules).add("--module", "com.greetings").toArray())
/exit
