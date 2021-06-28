package com.github.sormuras.bach.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/** {@link Duration}-related utilities. */
public class Durations {

  public static String beautify(Duration duration) {
    return duration
        .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
        .toString()
        .substring(2)
        .replaceAll("(\\d[HMS])(?!$)", "$1 ")
        .toLowerCase();
  }

  public static String beautifyBetweenNow(Instant start) {
    return beautify(Duration.between(start, Instant.now()));
  }

  /** Hidden default constructor. */
  private Durations() {}
}
