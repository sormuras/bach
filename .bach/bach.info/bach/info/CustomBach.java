package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.project.Property;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CustomBach extends Bach implements MainSpaceBuilder {

  public static void main(String... args) {
    var options = Options.of(args.length == 0 ? new String[] {"build"} : args);
    var bach = provider().newBach(options);
    System.exit(new Main().run(bach));
  }

  public static Provider<CustomBach> provider() {
    return CustomBach::new;
  }

  private CustomBach(Options options) {
    super(options);
  }

  @Override
  public String computeProjectVersion(ProjectInfo info) {
    var value = bach().options().find(Property.PROJECT_VERSION);
    if (value.isPresent()) return value.get();
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(now);
    try {
      return Files.readString(Path.of("VERSION")) + "-custom+" + timestamp;
    } catch (Exception exception) {
      logbook().log(Level.WARNING, "Read version failed: %s", exception);
      return info.version();
    }
  }
}
