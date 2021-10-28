import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.external.MultiExternalModuleLocator;
import com.github.sormuras.bach.simple.SimpleSpace;
import java.util.Map;

class build {
  public static void main(String... args) {
    try (var bach = new Bach(args)) {
      var grabber =
          bach.grabber(
              JUnit.version("5.8.1"),
              new MultiExternalModuleLocator(
                  Map.of(
                      "net.jqwik.api",
                      "https://oss.sonatype.org/content/repositories/snapshots/net/jqwik/jqwik-api/1.6.0-SNAPSHOT/jqwik-api-1.6.0-20211028.081012-29.jar",
                      "net.jqwik.engine",
                      "https://oss.sonatype.org/content/repositories/snapshots/net/jqwik/jqwik-engine/1.6.0-SNAPSHOT/jqwik-engine-1.6.0-20211028.081012-27.jar")));

      var test = SimpleSpace.of(bach, "test").withModule("bar").withModule("foo");

      test.grab(
          grabber,
          "org.junit.platform.console",
          "org.junit.platform.jfr",
          "org.junit.jupiter",
          "net.jqwik.engine");

      test.compile();
      test.runAllTests();
    }
  }
}
