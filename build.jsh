//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

Path bachJava = Paths.get("target/Bach.java")
if (Files.notExists(bachJava)) {
  URL bachURL = new URL("https://raw.githubusercontent.com/sormuras/bach/master/src/main/java/Bach.java");
  Files.createDirectories(bachJava.getParent());
  try (InputStream in = bachURL.openStream()) {
    Files.copy(in, bachJava, StandardCopyOption.REPLACE_EXISTING);
  }
  System.out.printf("created %s [url=%s]%n", bachJava, bachURL);
}
/open src/main/java/Bach.java

Bach bach = new Bach.Builder().level(Level.FINE).build()
bach.format()
bach.resolve("org.junit.jupiter.api", Bach.Util.maven("org.junit.jupiter", "junit-jupiter-api", "5.0.0-M4"))
bach.resolve("org.junit.platform.commons", Bach.Util.maven("org.junit.platform", "junit-platform-commons", "1.0.0-M4"))
bach.resolve("org.opentest4j", Bach.Util.maven("org.opentest4j", "opentest4j", "1.0.0-M2"))
bach.compile()
// TODO bach.run("de.sormuras.solartools", "de.sormuras.solartools.Main")
bach.test()

/exit
