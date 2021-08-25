package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class ConstantInterfaceTests {
  @Test
  void isFinalAndNothingElse() {
    assertEquals(Modifier.FINAL, ConstantInterface.class.getModifiers());
  }

  @Test
  void hasZeroRecordComponents() {
    assertEquals(0, ConstantInterface.class.getRecordComponents().length);
  }

  @Test
  void canBeInstantiatedAndReturnsAnNonNullStringRepresentation() {
    assertNotNull(new ConstantInterface().toString());
  }
}
