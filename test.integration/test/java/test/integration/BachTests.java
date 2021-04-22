package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Plugins;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.Spaces;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void explicit() {
    var logbook = Logbook.of(Printer.ofErrors(), true);
    var plugins = new Plugins();
    var options = Options.ofDefaultValues();
    var folders = Folders.of("");
    var spaces = new Spaces(new CodeSpaceMain(), new CodeSpaceTest());
    var project = new Project("explicit", folders, spaces);
    var bach = new Bach(logbook, options, plugins, project);

    assertEquals("explicit", bach.project().name());
  }
}
