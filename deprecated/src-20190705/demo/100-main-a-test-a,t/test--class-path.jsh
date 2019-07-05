/open ../../../BUILDING

var classPath = new Arguments()
classPath.add("bin/test/compiled/classes/t")
classPath.add("bin/main/compiled/classes/a")
classPath.add(get(Path.of(System.getProperty("user.home")).resolve(".bach/tool/junit").toString(), "org.junit.platform", "junit-platform-console-standalone", "1.5.0"))
classPath.add(get("lib/test-runtime-only", "de.sormuras.mainrunner", "de.sormuras.mainrunner.engine", "2.0.5"))

var args = new Arguments()
args.add("-ea")
args.add("--class-path").addPath(classPath.toArray())

args.add("org.junit.platform.console.ConsoleLauncher")
args.add("--fail-if-no-tests")
args.add("--scan-class-path")

/exit exe("java", args.toArray())