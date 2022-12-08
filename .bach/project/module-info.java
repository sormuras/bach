import run.bach.Command;

@Command(name = "rebuild", args = "clean + build")
@Command(
    name = "make",
    args = {
      "check-java-version",
      "19",
      /* === */ "+",
      "javac",
      "--release=17",
      "--module=run.bach,run.duke",
      "--module-source-path=src/*/main/java",
      """
      -d""",
      ".bach/out/classes-17",
      /* === */ "+",
      "jar",
      "--create",
      "--file=.bach/out/main/modules/run.bach.jar",
      "-C",
      ".bach/out/classes-17/run.bach",
      ".",
      /* === */ "+",
      "jar",
      "--create",
      "--file=.bach/out/main/modules/run.duke.jar",
      "-C",
      ".bach/out/classes-17/run.duke",
      ".",
      /* === */ "+",
      "test"
    })
@Command(name = "test", args = "test.bach")
module project {
  requires run.bach;

  provides run.bach.ProjectFactory with
      project.Factory;
}
