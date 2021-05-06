package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Wither;
import com.github.sormuras.bach.internal.Records.Name;
import org.junit.jupiter.api.Test;

class RecordsTests {
  @Test
  void external() {
    record Person(@Name("-n") String name) {}
    var records = new Records<>(Person.class);
    var person = new Person("Foo");
    assertEquals("Foo", person.name());
    assertEquals("Bar", records.with(person, "name", "Bar").name());
    assertEquals("Baz", records.with(person, "-n", "Baz").name());
  }

  @Test
  void chainable() {
    record Person(@Name("-n") String name) implements Wither<Person> {}
    var foo = new Person("Foo");
    var bar = foo.with("name", "Bar");
    var baz = bar.with("-n", "Baz");
    assertEquals("Foo", foo.name());
    assertEquals("Bar", bar.name());
    assertEquals("Baz", baz.name());
  }
}
