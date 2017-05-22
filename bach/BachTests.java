import java.nio.file.Paths;
import java.util.Objects;

class BachTests {

  private static void layoutOf() {
    assert Layout.BASIC == Bach.Builder.layoutOf(Paths.get("demo/basic"));
    assert Layout.FIRST == Bach.Builder.layoutOf(Paths.get("demo/idea"));
    assert Layout.TRAIL == Bach.Builder.layoutOf(Paths.get("demo/common"));
  }

  public static void main(String[] args) {
    layoutOf();
    ScoreTests.main(args);
    CommandTests.main(args);
  }

}
