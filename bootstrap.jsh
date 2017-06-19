//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

//
// download "Bach.java"
//
URL url = new URL("https://raw.githubusercontent.com/sormuras/bach/master/src/main/java/Bach.java");
Path target = Paths.get("target")
Path script = target.resolve("Bach.java")
if (Files.notExists(script)) {
  Files.createDirectories(target);
  try (InputStream stream = url.openStream()) { Files.copy(stream, script); }
}

//
// source "Bach.java" into this jshell environment and build
//
/open target/Bach.java

new Bach.Builder().build().call("java", "--version")

/exit
