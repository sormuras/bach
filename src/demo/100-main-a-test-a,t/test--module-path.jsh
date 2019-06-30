/open ../../../BUILDING

var classPath = new ArrayList<String>()
classPath.add(get(Path.of(System.getProperty("user.home")).resolve(".bach/tool/junit").toString(), "org.junit.platform", "junit-platform-console-standalone", "1.5.0").toString())
classPath.add(get("lib/test-runtime-only", "de.sormuras.mainrunner", "de.sormuras.mainrunner.engine", "2.0.5").toString())

var args = new Arguments()
args.add("-ea")
args.add("--class-path")
args.addPath(classPath.toArray(String[]::new))
args.add("--module-path")
args.addPath("bin/test/modules", "bin/main/modules")
args.add("--add-modules")
args.add("t")
args.add("org.junit.platform.console.ConsoleLauncher")
args.add("--fail-if-no-tests")
args.add("--select-module").add("t")

/exit exe("java", args.toArray())
