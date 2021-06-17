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
import java.lang.module.ModuleDescriptor;

class build {
  public static void main(String... args) {
    var options =
        Options.ofCommandLineArguments(args)
            .with("--main-jar-with-sources", "true")
            .underlay(Options.ofDefaultValues());
    var logbook = Logbook.of(Printer.ofSystem(), options.verbose());
    var bach = new Bach(Settings.of(options, logbook), project());
    bach.buildAndWriteLogbook();
  }

  static Project project() {
    var name = "MultiRelease-11";
    var version = ModuleDescriptor.Version.parse("11");
    var folders = Folders.of(".");
    var foo = folders.root("foo");
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    foo,
                    DeclaredModuleReference.of(foo.resolve("main/java/module-info.java")),
                    SourceFolders.of(
                        SourceFolder.of(foo.resolve("main/java")),
                        SourceFolder.of(foo.resolve("main/java-12")),
                        SourceFolder.of(foo.resolve("main/java-13")),
                        SourceFolder.of(foo.resolve("main/java-14")),
                        SourceFolder.of(foo.resolve("main/java-15")),
                        SourceFolder.of(foo.resolve("main/java-16"))),
                    SourceFolders.of(
                        SourceFolder.of(foo.resolve("main/java")),
                        SourceFolder.of(foo.resolve("main/java-12")),
                        SourceFolder.of(foo.resolve("main/java-13")),
                        SourceFolder.of(foo.resolve("main/java-14")),
                        SourceFolder.of(foo.resolve("main/java-15")),
                        SourceFolder.of(foo.resolve("main/java-16"))))),
            ModulePaths.of(folders.externalModules()),
            11);
    var spaces = Spaces.of(main, CodeSpaceTest.empty());
    var tools = Tools.of("javac", "jar");
    var externals = Externals.of();
    return new Project(name, version, folders, spaces, tools, externals);
  }
}
