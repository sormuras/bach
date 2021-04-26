package test.integration;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.Spaces;

public class Auxiliary {

  public static Bach newEmptyBach() {
    var logbook = Logbook.ofErrorPrinter();
    var factory = new Factory();
    var options = Options.ofDefaultValues();
    var folders = Folders.of("");
    var spaces = new Spaces(new CodeSpaceMain(), new CodeSpaceTest());
    var project = new Project("empty", folders, spaces);
    return new Bach(logbook, options, factory, project);
  }

  private Auxiliary() {}
}
