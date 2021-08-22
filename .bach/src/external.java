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
            FXGL.version("11.17"),
            GluonAttach.version("4.0.12"),
            Jackson.version("2.13.0-rc1"),
            JavaFX.version("18-ea+1"),
            JUnit.version("5.8.0-RC1"),
            Kotlin.version("1.5.30-RC"));

    bach.logCaption("Grab external modules");
    grabber.grabExternalModules("com.almasb.fxgl.all");
    grabber.grabExternalModules("org.junit.jupiter", "org.junit.platform.console");

    bach.logCaption("Grab missing external modules");
    grabber.grabMissingExternalModules();
  }
}
