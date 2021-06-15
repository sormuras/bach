package test.integration;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.Externals;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tools;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleDescriptor.Version;

public class Auxiliary {

  public static Bach newEmptyBach() {
    return newEmptyBach(Logbook.ofErrorPrinter());
  }

  public static Bach newEmptyBach(StringWriter writer) {
    return newEmptyBach(Logbook.of(Printer.of(new PrintWriter(writer, true)), true));
  }

  public static Bach newEmptyBach(Logbook logbook) {
    var options = Options.ofDefaultValues();
    var factory = new Factory();
    var folders = Folders.of(".");
    var core = new Core(logbook, options, factory, folders);

    var settings = Settings.of();

    var name = "empty";
    var version = Version.parse("0");
    var spaces = Spaces.of(CodeSpaceMain.empty(), CodeSpaceTest.empty());
    var tools = Tools.of();
    var externals = Externals.of();
    var project = new Project(name, version, folders, spaces, tools, externals);

    return new Bach(core, settings, project);
  }

  private Auxiliary() {}
}
