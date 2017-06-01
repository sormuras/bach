import java.nio.file.Paths;
import java.util.Objects;

public class FolderTests {
  private static void paths() {
    JavaShellBuilder builder = new JavaShellBuilder();
    assert Objects.equals(Paths.get("target", "bach"), builder.path(JavaShellBuilder.Folder.TARGET));
    assert Objects.equals(Paths.get("target", "bach", "main", "java"), builder.path(JavaShellBuilder.Folder.TARGET_COMPILE_MAIN));

    builder.set(JavaShellBuilder.Folder.TARGET, Paths.get("bin"));
    builder.set(JavaShellBuilder.Folder.TARGET_COMPILE_MAIN, Paths.get("classes"));
    assert Objects.equals(Paths.get("bin"), builder.path(JavaShellBuilder.Folder.TARGET));
    assert Objects.equals(Paths.get("bin", "classes"), builder.path(JavaShellBuilder.Folder.TARGET_COMPILE_MAIN));
  }

  public static void main(String... args) {
    paths();
  }
}
