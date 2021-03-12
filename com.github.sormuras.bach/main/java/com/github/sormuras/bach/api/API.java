package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import java.lang.System.Logger.Level;

/** An internal interface extended by all public API overlay interfaces of this package. */
interface API {
  /** {@return the underlying instance of {@code Bach}} */
  Bach bach();

  /** Record a log message at debug level, which is not printed to the output stream by default. */
  default void log(String format, Object... args) {
    bach().logbook().log(Level.DEBUG, format, args);
  }

  /** Record a log message at info level, which is printed to the output stream by default. */
  default void say(String format, Object... args) {
    bach().logbook().log(Level.INFO, format, args);
  }
}
