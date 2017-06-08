//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

/open bach/Bach.java

Bach bach = new Bach()
bach.call("java", "-version")
Bach.Util.cleanTree(Paths.get("target/test"), true)
{
bach.command("javac")
    .addAll("-d", "target/test/classes")
    .markDumpLimit(1)
    .addAllJavaFiles(Paths.get("bach"))
    .addAllJavaFiles(Paths.get("test"))
    .execute();
}
bach.call("java", "-ea", "-cp", "target/test/classes", "BachTests")

bach.format(false, Paths.get("test"), "--skip-sorting-imports")
bach.resolve("org.junit.jupiter.api", "http://central.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.0.0-M4/junit-jupiter-api-5.0.0-M4.jar")

bach.call("javadoc", "-Xdoclint:none", "-d", "target/test/doc", "bach/Bach.java")

bach.call("jar", "--create", "--file=target/test/bach.jar", "-C", "target/test/classes", "Bach.class")
bach.call("jar", "--create", "--file=target/test/bach-sources.jar", "-C", "bach", "Bach.java")
bach.call("jar", "--create", "--file=target/test/bach-javadoc.jar", "-C", "target/test/doc", ".")

/exit
