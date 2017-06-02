//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

Path bachJava = Paths.get("target/Bach.java")
if (Files.notExists(bachJava)) {
  URL bachURL = new URL("https://raw.githubusercontent.com/sormuras/bach/master/bach/Bach.java");
  Files.createDirectories(bachJava.getParent());
  try (InputStream in = bachURL.openStream()) {
    Files.copy(in, bachJava, StandardCopyOption.REPLACE_EXISTING);
  }
  System.out.printf("created %s [url=%s]%n", bachJava, bachURL);
}
/open bach/Bach.java

Bach bach = new Bach()
bach.set(Level.FINE)
bach.format()
bach.resolve("org.junit.jupiter.api", "http://central.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.0.0-M4/junit-jupiter-api-5.0.0-M4.jar")
bach.resolve("org.junit.platform.commons", "http://central.maven.org/maven2/org/junit/platform/junit-platform-commons/1.0.0-M4/junit-platform-commons-1.0.0-M4.jar")
bach.resolve("org.opentest4j.opentest4j", "http://central.maven.org/maven2/org/opentest4j/opentest4j/1.0.0-M2/opentest4j-1.0.0-M2.jar")
bach.compile()
bach.run("de.sormuras.solartools", "de.sormuras.solartools.Main")
// TODO bach.javadoc("de.sormuras.solartools", Paths.get("src/de.sormuras.solartools/main/java"), "de.sormuras.solartools")
// TODO bach.jar()
// TODO bach.test()

/exit
