@run.bach.Command(
    name = "make",
    line =
        "check-java-version 19\n"
            + "javac --release=17 --module=run.bach,run.duke"
            + " --module-source-path=src/*/main/java -d .bach/out/classes-17\n"
            + "jar --create --file=.bach/out/main/modules/run.bach.jar -C"
            + " .bach/out/classes-17/run.bach .\n"
            + "jar --create --file=.bach/out/main/modules/run.duke.jar -C"
            + " .bach/out/classes-17/run.duke .\n"
            + "test\n")
@run.bach.Command(name = "test", args = "test.bach")
module project {
  requires run.bach;

  provides run.bach.ProjectFactory with
      project.Factory;
}
