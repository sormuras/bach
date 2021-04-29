package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.Option;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JigsawQuickStartGreetingsTests {

  @Test
  void build() {
    var name = "JigsawQuickStartGreetings";
    var root = Path.of("test.projects", name);
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of(name + " Options")
                .with(Option.CHROOT, root)
                // "--strict"
                // "--limit-tools", "javac,jar"
                // "--jar-with-sources"
                .with(Option.VERBOSE)
                .with(Action.BUILD));

    assertTrue(bach.options().is(Option.VERBOSE));
    assertEquals(root, bach.project().folders().root());
    assertEquals(name, bach.project().name());

    var main = bach.project().spaces().main();
    assertEquals(0, main.release());
    assertEquals(1, main.modules().size());
    assertEquals("com.greetings", main.modules().toNames(","));
    var test = bach.project().spaces().test();
    assertEquals(0, test.modules().size());

    assertEquals(0, bach.run(), bach.logbook().toString());
  }
}
