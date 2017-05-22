import java.util.Objects;
import java.util.logging.Level;

class ScoreTests {

  private static void defaultValues() {
    Bach.Score score = Bach.builder().score();
    assert Objects.equals("bach", score.name);
    assert Objects.equals("", score.version);
    assert Objects.equals(Layout.AUTO, score.layout);
    assert score.mains.isEmpty();
  }

  private static void builderValues() {
    Bach.Score score = Bach.builder()
            .name("name")
            .version("123")
            .layout(Layout.TRAIL)
            .log(Level.ALL)
            .main("a.b.c", "a.b.c.main.Launcher")
            .score();
    assert Objects.equals("name", score.name);
    assert Objects.equals("123", score.version);
    assert Objects.equals(Layout.TRAIL, score.layout);
    assert Objects.equals(Level.ALL, score.level);
    assert Objects.equals("a.b.c.main.Launcher", score.mains.get("a.b.c"));
  }

  public static void main(String[] args) {
    defaultValues();
    builderValues();
  }

}
