package com.github.sormuras.bach.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Module-URI pair collecting annotation. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface Links {
  /** @return the module-uri pairs */
  Link[] value();
}
