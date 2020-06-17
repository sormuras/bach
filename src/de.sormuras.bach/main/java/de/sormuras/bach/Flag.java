package de.sormuras.bach;

import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

/** A flag represents a feature toggle. */
public enum Flag {
  DRY_RUN(false),
  FAIL_FAST(true),
  FAIL_ON_ERROR(true);

  public static Set<Flag> ofSystem() {
    var flags = new TreeSet<Flag>();
    for (var flag : values()) {
      var property = System.getProperty(flag.key(), flag.isEnabledByDefault() ? "true" : "false");
      if (Boolean.parseBoolean(property)) flags.add(flag);
    }
    return EnumSet.copyOf(flags);
  }

  private final boolean enabledByDefault;

  Flag(boolean enabledByDefault) {
    this.enabledByDefault = enabledByDefault;
  }

  public boolean isEnabledByDefault() {
    return enabledByDefault;
  }

  public String key() {
    return name().toLowerCase().replace('_', '-');
  }
}
