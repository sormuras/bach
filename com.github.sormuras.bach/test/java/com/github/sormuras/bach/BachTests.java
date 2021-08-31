package com.github.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void packagePrivateSealerRecordCanBeInstantiatedAndReturnsNonNullStringRepresentation() {
    assertNotNull(new Bach.Sealer().toString());
  }
}
