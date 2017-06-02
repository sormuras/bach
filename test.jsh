//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

/open bach/Bach.java

Bach bach = new Bach()
bach.call("java", "-version")
{
bach.command("javac")
    .addAll("-d", "target/test")
    .markDumpLimit(1)
    .addAllJavaFiles(Paths.get("bach"))
    .addAllJavaFiles(Paths.get("test"))
    .execute();
}
bach.call("java", "-ea", "-cp", "target/test", "BachTests")

/exit
