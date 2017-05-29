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
/open target/Bach.java

{
Bach.create()
    .set(Level.FINE)
    .format()
    .load("org.junit.jupiter.api", URI.create("http://central.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.0.0-M4/junit-jupiter-api-5.0.0-M4.jar"))
    .load("org.junit.platform.commons", URI.create("http://central.maven.org/maven2/org/junit/platform/junit-platform-commons/1.0.0-M4/junit-platform-commons-1.0.0-M4.jar"))
    .load("org.opentest4j.opentest4j", URI.create("http://central.maven.org/maven2/org/opentest4j/opentest4j/1.0.0-M2/opentest4j-1.0.0-M2.jar"))
    .compile()
    .javadoc("de.sormuras.solartools", Paths.get("src/de.sormuras.solartools/main/java"), "de.sormuras.solartools")
    .jar()
    .test()
    .runCompiled("de.sormuras.solartools");
}
/exit
