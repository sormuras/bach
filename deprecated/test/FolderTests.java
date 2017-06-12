import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderTests {

  private static final Bach JAVA_SHELL_BUILDER = new Bach();

  private static Bach.Folder folder(String name) {
    return Bach.Folder.valueOf(name);
  }

  private static Path path(String name) {
    return JAVA_SHELL_BUILDER.path(folder(name));
  }

  private static void defaults() {
    for (Bach.Folder folder : Bach.Folder.values()) {
      assert JAVA_SHELL_BUILDER.path(folder) != null;
    }
    assert path("JDK_HOME").isAbsolute();
    assert path("JDK_HOME_BIN").isAbsolute();
    assert path("JDK_HOME_MODS").isAbsolute();
    assert path("TARGET").equals(Paths.get("target", "bach"));
    assert path("TARGET_MAIN_COMPILE").equals(Paths.get("target", "bach", "main", "compile"));
  }

  private static void paths() {
    Bach builder = new Bach();
    Path bin = Paths.get("bin");
    builder.set(Bach.Folder.TARGET, bin);
    assert bin.equals(builder.path(Bach.Folder.TARGET));
    assert bin.resolve("main/compile").equals(builder.path(folder("TARGET_MAIN_COMPILE")));
    builder.set(Bach.Folder.TARGET_MAIN_COMPILE, Paths.get("classes"));
    assert bin.equals(builder.path(Bach.Folder.TARGET));
    assert bin.resolve("classes").equals(builder.path(folder("TARGET_MAIN_COMPILE")));
  }

  public static void main(String... args) {
    defaults();
    paths();
  }
}
