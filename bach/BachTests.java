import java.nio.file.Paths;
import java.util.Objects;

class BachTests {

  private static void scoreDefault() {
    Bach.Score score = Bach.builder().layout(Layout.BASIC).score();
    assert Objects.equals(Layout.BASIC, score.layout);
    assert Objects.equals("bach", score.name);
  }

  private static void layoutOf() {
    assert Layout.BASIC == Bach.Builder.layoutOf(Paths.get("demo/basic"));
    assert Layout.FIRST == Bach.Builder.layoutOf(Paths.get("demo/idea"));
    assert Layout.TRAIL == Bach.Builder.layoutOf(Paths.get("demo/common"));
  }

  public static void main(String[] args) {
    scoreDefault();
    layoutOf();
  }

}
