import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderTests {

  private static final JavaShellBuilder JAVA_SHELL_BUILDER = new JavaShellBuilder();

  private static JavaShellBuilder.Folder folder(String name) {
    return JavaShellBuilder.Folder.valueOf(name);
  }

  private static Path path(String name) {
    return JAVA_SHELL_BUILDER.path(folder(name));
  }

  private static void defaults() {
    for (JavaShellBuilder.Folder folder : JavaShellBuilder.Folder.values()) {
      assert JAVA_SHELL_BUILDER.path(folder) != null;
    }
    assert path("JDK_HOME").isAbsolute();
    assert path("JDK_HOME_BIN").isAbsolute();
    assert path("JDK_HOME_MODS").isAbsolute();
    assert path("TARGET").equals(Paths.get("target", "bach"));
    assert path("TARGET_COMPILE_MAIN").equals(Paths.get("target", "bach", "main", "java"));
  }

  private static void paths() {
    JavaShellBuilder builder = new JavaShellBuilder();
    Path bin = Paths.get("bin");
    builder.set(JavaShellBuilder.Folder.TARGET, bin);
    assert bin.equals(builder.path(JavaShellBuilder.Folder.TARGET));
    assert bin.resolve("main/java").equals(builder.path(folder("TARGET_COMPILE_MAIN")));
    builder.set(JavaShellBuilder.Folder.TARGET_COMPILE_MAIN, Paths.get("classes"));
    assert bin.equals(builder.path(JavaShellBuilder.Folder.TARGET));
    assert bin.resolve("classes").equals(builder.path(folder("TARGET_COMPILE_MAIN")));
  }

  public static void main(String... args) {
    defaults();
    paths();
  }
}
