import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.DeclaredModule;
import com.github.sormuras.bach.api.DeclaredModuleFinder;
import com.github.sormuras.bach.api.DeclaredModuleReference;
import com.github.sormuras.bach.api.Externals;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.ModulePaths;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.SourceFolder;
import com.github.sormuras.bach.api.SourceFolders;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tools;
import com.github.sormuras.bach.locator.JUnit;

import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.Set;

class build {
  public static void main(String... args) {
    var options = Options.ofCommandLineArguments(args).underlay(Options.ofDefaultValues());
    var logbook = Logbook.of(Printer.ofSystem(), options.verbose());
    var bach = new Bach(Settings.of(options, logbook), project());
    bach.buildAndWriteLogbook();
  }

  static Project project() {
    var name = "RecordingEvents";
    var version = ModuleDescriptor.Version.parse("99");
    var folders = Folders.of(".");

    var bar = folders.root("bar");
    var foo = folders.root("foo");
    var test =
        new CodeSpaceTest(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    bar,
                    DeclaredModuleReference.of(bar.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(bar.resolve("test/java"))),
                    SourceFolders.of()),
                new DeclaredModule(
                    foo,
                    DeclaredModuleReference.of(foo.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(foo.resolve("test/java"))),
                    SourceFolders.of())),
            ModulePaths.of(folders.externalModules()));
    var spaces = Spaces.of(CodeSpaceMain.empty(), test);
    var tools = Tools.of("javac", "jar", "test", "junit");
    var externals = new Externals(
        Set.of("org.junit.platform.console", "org.junit.platform.jfr"),
        List.of(JUnit.V_5_7_2));

    return new Project(name, version, folders, spaces, tools, externals);
  }
}
