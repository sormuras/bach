//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

/open src/main/java/Bach.java
/open src/main/java/Build.java

try {
  Build.main();
} catch (Throwable t) {
  Files.createFile(Paths.get("build.jsh.failed"));
}

/exit
