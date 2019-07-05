package de.sormuras.bach;

import java.nio.file.Files;
import java.nio.file.Path;

public class UtilTests {
  public static void main(String[] args) {
    checkAssigned();
    checkFindDirectoryEntries();
  }

  private static void checkAssigned() {
    assert Util.assigned(Path.of("a")).getNameCount() == 1;
    assert Util.assigned(Path.of("b"), "path").getNameCount() == 1;
  }

  private static void checkFindDirectoryEntries() {
    var files = Util.findDirectoryEntries(Path.of(""), Files::isRegularFile);
    assert files.contains("bach.properties");
  }
}
