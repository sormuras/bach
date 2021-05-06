package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Records;
import java.lang.reflect.RecordComponent;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public interface Wither<R extends Record> {

  @SuppressWarnings("unchecked")
  default R with(String name, Object value) {
    var records = new Records<>((Class<R>) getClass());
    return records.with((R) this, name, value);
  }

  @SuppressWarnings("unchecked")
  default R with(
      String name, Function<RecordComponent, Object> wrapper, BinaryOperator<Object> merger) {
    var records = new Records<>((Class<R>) getClass());
    return records.with((R) this, name, wrapper, merger);
  }
}
