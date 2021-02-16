package com.github.sormuras.bach.internal;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** String-related helpers. */
public class Strings {

  /** {@return a human-readable representation of the given duration} */
  public static String toString(Duration duration) {
    return duration
        .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
        .toString()
        .substring(2)
        .replaceAll("(\\d[HMS])(?!$)", "$1 ")
        .toLowerCase();
  }

  /** Hidden default constructor. */
  private Strings() {}
}
