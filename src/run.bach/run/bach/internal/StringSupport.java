package run.bach.internal;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public interface StringSupport {
  static String prettify(Duration duration) {
    var string = duration.truncatedTo(ChronoUnit.MILLIS).toString(); // ISO-8601: "PT8H6M12.345S"
    return string.substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase(Locale.ROOT);
  }

  record Property(String key, String value) {}

  static Property parseProperty(String string) {
    return StringSupport.parseProperty(string, '=');
  }

  static Property parseProperty(String string, char separator) {
    int index = string.indexOf(separator);
    if (index < 0) {
      var message = "Expected a `KEY%sVALUE` string, but got: %s".formatted(separator, string);
      throw new IllegalArgumentException(message);
    }
    var key = string.substring(0, index);
    var value = string.substring(index + 1);
    return new Property(key, value);
  }
}
