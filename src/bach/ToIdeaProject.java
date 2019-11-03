import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Generate IDEA project configuration directory.
 */
public class ToIdeaProject implements Consumer<Bach.Project> {

  public static void main(String... args) {
    var base = Path.of(args[0]);
    var project = Bach.Project.Builder.build(base);
    new ToIdeaProject(new PrintWriter(System.out, true), base).accept(project);
  }

  private final PrintWriter out;
  private final Path idea;

  ToIdeaProject(PrintWriter out, Path base) {
    this.out = out;
    this.idea = base.resolve(".idea");
  }

  @Override
  public void accept(Bach.Project project) {
    if (Files.exists(idea)) {
      out.println("IDEA configuration already exists: " + idea.toUri());
      return;
    }
    try {
      write(project, project.units.stream().map(unit -> unit.descriptor).collect(Collectors.toList()));
    } catch (Exception e) {
      throw new Error("Writing IDEA project failed", e);
    }
  }

  String moduleLocation(String name) {
    var file = name + ".iml";
    var path = "$PROJECT_DIR$/.idea/" + file;
    return "fileurl='file://" + path + "' filepath='" + path + "'";
  }

  void write(Bach.Project project, Collection<ModuleDescriptor> descriptors) throws Exception {
    var idea = Files.createDirectories(this.idea);
    Files.write(
        idea.resolve("google-java-format.xml"),
        List.of(
            "<?xml version='1.0' encoding='UTF-8'?>",
            "<project version='4'>",
            "  <component name='GoogleJavaFormatSettings'>",
            "    <option name='enabled' value='true' />",
            "  </component>",
            "</project>",
            ""));
    Files.write(
        idea.resolve("misc.xml"),
        List.of(
            "<?xml version='1.0' encoding='UTF-8'?>",
            "<project version='4'>",
            "  <component name='ProjectRootManager' version='2' languageLevel='JDK_11' project-jdk-name='11' project-jdk-type='JavaSDK'>",
            "    <output url='file://$PROJECT_DIR$/.idea/out' />",
            "  </component>",
            "</project>",
            ""));
    var modulesXml = idea.resolve("modules.xml");
    Files.write(
        modulesXml,
        List.of(
            "<?xml version='1.0' encoding='UTF-8'?>",
            "<project version='4'>",
            "  <component name='ProjectModuleManager'>",
            "    <modules>",
            "      <module " + moduleLocation(project.name) + " />"));
    var names = descriptors.stream().map(ModuleDescriptor::name).collect(Collectors.toList());
    var modules = names.stream()
            .map(name -> "      <module " + moduleLocation(name) + " />")
            .collect(Collectors.toList());
    Files.write(modulesXml, modules, StandardOpenOption.APPEND);
    Files.write(
        modulesXml,
        List.of("    </modules>", "  </component>", "</project>"),
        StandardOpenOption.APPEND);
    Files.write(
        idea.resolve(project.name + ".iml"),
        List.of(
            "<?xml version='1.0' encoding='UTF-8'?>",
            "<module type='JAVA_MODULE' version='4'>",
            "  <component name='NewModuleRootManager' inherit-compiler-output='true'>",
            "    <content url='file://$MODULE_DIR$' />",
            "  </component>",
            "</module>",
            ""));
    for (var module : descriptors) {
      var name = module.name();
      var content = "src/" + name;
      var folderUrl = "url='file://$MODULE_DIR$/" + content + "/main/java'";
      var lines = new ArrayList<String>();
      lines.add("<?xml version='1.0' encoding='UTF-8'?>");
      lines.addAll(
          List.of(
              "<module type='JAVA_MODULE' version='4'>",
              "  <component name='NewModuleRootManager' inherit-compiler-output='true'>",
              "    <content url='file://$MODULE_DIR$/" + content + "'>",
              "      <sourceFolder " + folderUrl + " isTestSource='false' />",
              "    </content>",
              "    <orderEntry type='sourceFolder' forTests='false' />")
      );
      for (var requires : module.requires()) {
        if (names.contains(requires.name())) {
          lines.add("    <orderEntry type='module' module-name='" + requires.name() + "' />");
        }
      }
      lines.addAll(
          List.of(
              "    <orderEntry type='library' name='lib' level='project' />",
              "    <orderEntry type='inheritedJdk' />",
              "  </component>",
              "</module>",
              "")
      );
      Files.write(idea.resolve(name + ".iml"), lines);
    }
    var libraries = Files.createDirectories(idea.resolve("libraries"));
    Files.write(
        libraries.resolve("lib.xml"),
        List.of(
            "<component name='libraryTable'>",
            "  <library name='lib'>",
            "    <classes>",
            "      <root url='file://$PROJECT_DIR$/lib' />",
            "    </classes>",
            "    <javadoc />",
            "    <sources />",
            "    <jarDirectory url='file://$PROJECT_DIR$/lib' recursive='false' />",
            "  </library>",
            "</component>"));
  }
}
