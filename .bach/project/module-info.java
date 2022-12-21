import run.bach.Command;
import run.bach.ProjectInfo;
import run.bach.ProjectInfo.Module;
import run.bach.ProjectInfo.Space;

@ProjectInfo(
    name = "Bach",
    spaces = {
      @Space(
          name = "main",
          release = 17,
          launchers = "run.bach/run.bach.Main",
          modules = {
            @Module(content = "src/run.bach", info = "main/java/module-info.java"),
            @Module(content = "src/run.duke", info = "main/java/module-info.java")
          }),
      @Space(
          name = "test",
          requires = "main",
          launchers = {"test.bach/test.bach.Main", "test.duke/test.duke.Main"},
          modules = {
            @Module(content = "src/test.bach", info = "test/java/module-info.java"),
            @Module(content = "src/test.duke", info = "test/java/module-info.java"),
            @Module(content = "src/test.junit", info = "test/java/module-info.java"),
          })
    })
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

  provides run.bach.Composer with
      project.BachComposer;
  provides run.duke.ToolOperator with
      project.Format;
  provides java.util.spi.ToolProvider with
      project.Zip;
}
