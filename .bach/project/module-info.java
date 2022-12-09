import run.bach.Command;

@Command(name = "rebuild", args = "clean + format + build")
@Command(name = "remake", args = "clean + make")
@Command(
    name = "make",
    args = {
      """
      duke check-java-version 17
      """,
      """
      javac --release=17 --module=run.bach,run.duke --module-source-path=src/*/main/java -d .bach/out/make-17
      """,
      """
      duke tree --mode=create .bach/out/main/modules
      """,
      """
      jar --create --file=.bach/out/main/modules/run.bach.jar -C .bach/out/make-17/run.bach .
      """,
      """
      jar --create --file=.bach/out/main/modules/run.duke.jar -C .bach/out/make-17/run.duke .
      """,
      """
      javac --release=17 --module=test.bach --module-source-path=src/*/test/java --module-path .bach/out/main/modules -d .bach/out/make-17
      """,
      """
      java --module-path .bach/out/make-17 --module test.bach/test.bach.Main
      """,
    })
module project {
  requires run.bach;

  provides run.bach.Project.Factory with
      project.BachProjectFactory;
}
