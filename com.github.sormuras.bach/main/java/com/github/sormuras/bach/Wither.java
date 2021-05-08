package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Records;

public interface Wither<R extends Record> {

  @SuppressWarnings("unchecked")
  default R with(String name, Object value) {
    return Records.of((Class<R>) getClass()).with((R) this, name, value);
  }
}
