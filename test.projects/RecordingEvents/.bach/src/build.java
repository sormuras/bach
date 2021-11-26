import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.external.Maven;
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
                      Maven.central("net.jqwik", "jqwik-api", "1.6.0"),
                      "net.jqwik.engine",
                      Maven.central("net.jqwik", "jqwik-engine", "1.6.0"))));

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
