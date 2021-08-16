package com.github.sormuras.bach.internal;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public sealed interface DurationSupport permits ConstantInterface {
  /** {@return a human-readable representation of the given duration} */
  static String toHumanReadableString(Duration duration) {
    return duration
        .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
        .toString()
        .substring(2)
        .replaceAll("(\\d[HMS])(?!$)", "$1 ")
        .toLowerCase();
  }
}
