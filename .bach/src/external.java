import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.external.FXGL;
import com.github.sormuras.bach.external.GluonAttach;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.external.Jackson;
import com.github.sormuras.bach.external.JavaFX;
import com.github.sormuras.bach.external.Kotlin;

class external {
  public static void main(String... args) {
    var bach = new Bach(args);
    var grabber =
        bach.grabber(
            // https://repo.maven.apache.org/maven2/com/github/almasb/fxgl/
            FXGL.version("17"),
            // https://repo.maven.apache.org/maven2/com/gluonhq/attach/util/
            GluonAttach.version("4.0.13"),
            // https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jackson-parent/
            Jackson.version("2.13.0"),
            // https://repo.maven.apache.org/maven2/org/openjfx/javafx/
            JavaFX.version("18-ea+9"),
            // https://repo.maven.apache.org/maven2/org/junit/junit-bom/
            JUnit.version("5.8.2"),
            // https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-bom/
            Kotlin.version("1.6.10"));

    bach.logCaption("Grab external modules");
    grabber.grabExternalModules("com.almasb.fxgl.all");
    grabber.grabExternalModules("org.junit.jupiter", "org.junit.platform.console");

    bach.logCaption("Grab missing external modules");
    grabber.grabMissingExternalModules();
  }
}
