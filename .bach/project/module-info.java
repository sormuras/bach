import run.bach.Command;

@Command(name = "rebuild", args = "clean + build")
@Command(name = "remake", args = "clean + make")
@Command(
    name = "make",
    args = {
      """
      duke check-java-version 17
      """,
      """
      javac --release=17 --module=run.bach,run.duke --module-source-path=src/*/main/java -d .bach/out/classes-17
      """,
      """
      duke tree --mode=create .bach/out/main/modules
      """,
      """
      jar --create --file=.bach/out/main/modules/run.bach.jar -C .bach/out/classes-17/run.bach .
      """,
      """
      jar --create --file=.bach/out/main/modules/run.duke.jar -C .bach/out/classes-17/run.duke .
      """,
    })
module project {
  requires run.bach;

  provides run.bach.ProjectFactory with
      project.Factory;
}
