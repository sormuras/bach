import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.api.CodeSpace;
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
import com.github.sormuras.bach.api.Tweak;
import com.github.sormuras.bach.tool.JavadocCall;
import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.Set;

class build {
  public static void main(String... args) {
    var options = Options.ofCommandLineArguments(args).underlay(Options.ofDefaultValues());
    var logbook = Logbook.of(Printer.ofSystem(), options.verbose());
    var bach = new Bach(Settings.of(options, logbook), project());
    var folders = bach.project().folders();
    bach.settings().workflows().newCleanWorkflow().with(bach).clean();
    bach.buildAndWriteLogbook();
    bach.run(
        new JavadocCall()
            .with("--module", "showcode")
            .with("--module-source-path", "./*/main/java")
            .with("-docletpath", folders.modules(CodeSpace.MAIN, "showcode@99.jar"))
            .with("-doclet", "showcode.ShowDoclet"));
  }

  static Project project() {
    var name = "ProcessingCode";
    var version = ModuleDescriptor.Version.parse("99");
    var folders = Folders.of(".");
    var showcode = folders.root("showcode");
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    showcode,
                    DeclaredModuleReference.of(showcode.resolve("main/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(showcode.resolve("main/java"))),
                    SourceFolders.of())),
            ModulePaths.of(folders.externalModules()),
            0);
    var tests = folders.root("tests");
    var test =
        new CodeSpaceTest(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    tests,
                    DeclaredModuleReference.of(tests.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(tests.resolve("test/java"))),
                    SourceFolders.of())),
            ModulePaths.of(folders.modules(CodeSpace.MAIN), folders.externalModules()));
    var spaces = Spaces.of(main, test);
    var tools =
        Tools.of("javac", "jar", "javadoc")
            .with(
                new Tweak(
                    Set.of(CodeSpace.TEST),
                    "javac",
                    List.of("--processor-module-path", folders.modules(CodeSpace.MAIN).toString())),
                new Tweak(Set.of(CodeSpace.TEST), "javac", List.of("-Xplugin:showPlugin")));
    var externals = Externals.of();
    return new Project(name, version, folders, spaces, tools, externals);
  }
}
