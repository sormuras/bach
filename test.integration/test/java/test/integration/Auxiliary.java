package test.integration;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.Externals;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.Spaces;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleDescriptor.Version;
import java.util.List;
import java.util.Set;

public class Auxiliary {

  public static Bach newEmptyBach() {
    return newEmptyBach(Logbook.ofErrorPrinter());
  }

  public static Bach newEmptyBach(StringWriter writer) {
    return newEmptyBach(Logbook.of(Printer.of(new PrintWriter(writer, true)), true));
  }

  public static Bach newEmptyBach(Logbook logbook) {
    var factory = new Factory();
    var options = Options.ofDefaultValues();
    var folders = Folders.of("");
    var spaces = new Spaces(CodeSpaceMain.empty(), CodeSpaceTest.empty());
    var externals = new Externals(Set.of(), List.of());
    var name = "empty";
    var version = Version.parse("0");
    var project = new Project(name, version, folders, spaces, externals);
    return new Bach(logbook, options, factory, project);
  }

  private Auxiliary() {}
}
