import run.bach.Command;
import run.bach.ProjectInfo;
import run.bach.ProjectInfo.Space;

@ProjectInfo(
    name = "Bach",
    version = "*",
    spaces = {
      @Space(
          name = "main",
          modules = {"run.bach", "run.duke"},
          release = 17,
          launchers = "run.bach/run.bach.Main"),
      @Space(
          name = "test",
          modules = {"test.bach", "test.duke", "test.junit"},
          launchers = {"test.bach/test.bach.Main", "test.duke/test.duke.Main"},
          requires = "main")
    })
@Command(name = "rebuild", args = "clean + format + build + bundle")
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

  provides run.bach.Composer with
      project.BachComposer;
  provides java.util.spi.ToolProvider with
      project.Build,
      project.Bundle,
      project.CompileClasses,
      project.Format;
}
