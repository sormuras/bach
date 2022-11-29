import run.bach.Command;

@Command(name = "build", line = "check + compile + test")
@Command(name = "check", line = "check-java-version 19")
@Command(
    name = "compile",
    line =
        """
        javac --release=17 --module=run.bach --module-source-path=src/*/main/java -d .bach/out/classes-17
        jar --create --file=.bach/out/modules/run.bach.jar -C .bach/out/classes-17/run.bach .
        jar --update --file=.bach/out/modules/run.bach.jar -C src/run.bach/main/java .
        """)
@Command(
    name = "test",
    args = {"test-jars", "test.bach"})
@Command(
    name = "test-jars",
    line =
        """
        jar --describe-module --file=.bach/out/modules/run.bach.jar
        jar --validate --file=.bach/out/modules/run.bach.jar
        """)
module project {
  requires run.bach;
}
