//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

/open src/main/java/Bach.java
/open src/main/java/Build.java

Path failed = Paths.get("build.jsh.failed");
Files.deleteIfExists(failed)
try {
  Build.main();
} catch (Throwable t) {
  Files.createFile(failed);
}

/exit
