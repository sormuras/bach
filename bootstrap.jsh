//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

//
// download "Bach.java"
//
Path target = Paths.get("target")
Path source = target.resolve("Bach.java")
if (Files.notExists(source)) {
  Files.createDirectories(target);
  URL url = new URL("https://raw.githubusercontent.com/sormuras/bach/master/src/main/java/Bach.java");
  try (InputStream in = url.openStream()) {
    Files.copy(in, source, StandardCopyOption.REPLACE_EXISTING);
  }
  System.out.printf("created %s [%s]%n", source, url);
}

//
// source "Bach.java" into this jshell environment and build
//
/open target/Bach.java

new Bach.Builder().build().call("java", "--version")

/exit
