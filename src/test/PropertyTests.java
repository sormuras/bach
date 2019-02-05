/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class PropertyTests {

  private static <E extends Enum<E>> Stream<DynamicTest> stream(
      Class<E> enumClass, Consumer<E> consumer) {
    return Stream.of(enumClass.getEnumConstants())
        .map(property -> dynamicTest(property.name(), () -> consumer.accept(property)));
  }

  @TestFactory
  Stream<DynamicTest> property() {
    return stream(Property.class, this::assertProperty);
  }

  private void assertProperty(Property property) {
    assertTrue(property.key.startsWith("bach."));
    assertFalse(property.defaultValue.isBlank());
    // assertFalse(property.description.isBlank());
  }
}
