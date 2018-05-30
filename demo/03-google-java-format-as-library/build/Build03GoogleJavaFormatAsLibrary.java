import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

public class Build03GoogleJavaFormatAsLibrary {

  public static void main(String... args) throws Exception {
    var bach = new Bach();

    var modulePath = Paths.get(".bach/module-path");
    var classPath = Paths.get(".bach/class-path");

    // module-path
    bach.util.download(
        URI.create(
            "http://central.maven.org/maven2/com/google/googlejavaformat/google-java-format/1.6/google-java-format-1.6.jar"),
        modulePath);
    bach.util.download(
        URI.create(
            "http://central.maven.org/maven2/com/google/guava/guava/24.1-jre/guava-24.1-jre.jar"),
        modulePath);

    // class-path
    //  * JPMS can't convert "file name" -> "module name"
    //    o doesn't provide Automatic-Module-Name in it's MANIFEST.MF
    //  * rename manually to "errorprone.jar" and put on the module-path yields:
    //    o Error occurred during initialization of boot layer
    //        java.lang.module.FindException: Unable to derive module descriptor for
    //          .bach\module-path\errorprone.jar
    //      Caused by: java.lang.module.InvalidModuleDescriptorException: Provider class
    //          com.sun.tools.javac.platform.JDKPlatformProvider not in module
    var errorprone =
        bach.util.download(
            URI.create(
                "http://central.maven.org/maven2/com/google/errorprone/javac-shaded/9+181-r4173-1/javac-shaded-9+181-r4173-1.jar"),
            classPath);

    var javac = new JdkTool.Javac();
    javac.classPath = List.of(errorprone);
    javac.modulePath = List.of(modulePath);
    javac.moduleSourcePath = List.of(Paths.get("src"));
    javac.destination = Paths.get("target");
    bach.run("[compile]", javac.toCommand(bach));

    var java = new JdkTool.Java();
    java.classPath = List.of(errorprone);
    java.modulePath = List.of(modulePath, javac.destination);
    java.module = "application/foo.Foo";
    bach.run("[launch]", java.toCommand(bach));
  }
}
