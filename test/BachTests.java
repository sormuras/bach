import java.nio.file.Paths;
import java.util.logging.Level;

class BachTests {

  private static void defaultFolders() {
    Bach bach = Bach.builder().log(Level.OFF).build();
    assert bach.path(Folder.JDK_HOME).isAbsolute();
    assert bach.path(Folder.JDK_HOME_MODS).isAbsolute();
  }

  private static void buildLayout() {
    assert Layout.BASIC == Bach.Builder.buildLayout(Paths.get("demo/basic"));
    assert Layout.FIRST == Bach.Builder.buildLayout(Paths.get("demo/idea"));
    assert Layout.TRAIL == Bach.Builder.buildLayout(Paths.get("demo/common"));
  }

  public static void main(String[] args) {
    defaultFolders();
    buildLayout();
    ScoreTests.main(args);
    CommandTests.main(args);
  }

}
