package test.base.magnificat.internal;

import java.time.Duration;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import test.base.magnificat.Bach;

public class Strings {

  public static String generateBanner() {
    var nameAndVersionAnd = "Bach " + Bach.version() + " ";
    return nameAndVersionAnd
        + new StringJoiner(", ", "(", ")")
            .add("Java " + Runtime.version().toString())
            .add(System.getProperty("os.name"))
            .add(System.getProperty("user.dir"));
  }

  public static String toString(Duration duration) {
    return duration
        .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
        .toString()
        .substring(2)
        .replaceAll("(\\d[HMS])(?!$)", "$1 ")
        .toLowerCase();
  }
}
