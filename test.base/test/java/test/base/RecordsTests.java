package test.base;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordsTests {

  @Test
  void distinctTypes() {
    record Point(int x, int y) {}
    record Arrow(Shaft shaft, Head head) {
      record Shaft(Point p) {}

      record Head(Point p) {}

      public Arrow with(Object o) {
        return new Arrow(o instanceof Shaft s ? s : shaft, o instanceof Head h ? h : head);
      }

      public Arrow withHead(int x, int y) {
        return with(new Head(new Point(x, y)));
      }
    }

    var arrow =
        new Arrow(new Arrow.Shaft(new Point(0, 0)), new Arrow.Head(new Point(1, 1)))
            .with(new Arrow.Shaft(new Point(2, 2)))
            .withHead(3, 3);

    assertEquals("Shaft[p=Point[x=2, y=2]]", arrow.shaft().toString());
    assertEquals("Head[p=Point[x=3, y=3]]", arrow.head().toString());
  }

  @Test
  void sameReferenceTypes() {
    record Person(String forename, String lastname) {
      public Person with(String component, Object o) {
        var components = Person.class.getRecordComponents();
        var i = -1;
        return new Person(
            component.equals(components[++i].getName()) ? (String) o : forename,
            component.equals(components[++i].getName()) ? (String) o : lastname);
      }

      private static final Person WITH = new Person("0", "1");

      public <T> Person with(Function<Person, T> accessor, T o) {
        var hint = accessor.apply(WITH);
        return new Person(
            hint == WITH.forename ? (String) o : forename,
            hint == WITH.lastname ? (String) o : lastname);
      }
    }

    var ab = new Person("A", "B");
    var cd = ab.with("forename", "C").with("lastname", "D");
    var ef = cd.with(Person::forename, "E").with(Person::lastname, "F");

    assertEquals("Person[forename=A, lastname=B]", ab.toString());
    assertEquals("Person[forename=C, lastname=D]", cd.toString());
    assertEquals("Person[forename=E, lastname=F]", ef.toString());

    var xx = new Person("X", "X");
    var xy = xx.with(Person::lastname, "Y");
    assertEquals("Person[forename=X, lastname=Y]", xy.toString());
  }

  @Test
  void samePrimitiveTypes() {
    record Point(int x, int y) {
      private static final Point WITH = new Point(0, 1);
      public <T> Point with(Function<Point, T> accessor, T value) {
        var hint = accessor.apply(WITH);
        return new Point(
            hint.equals(WITH.x) ? (int) value : x,
            hint.equals(WITH.y) ? (int) value : y
        );
      }
      public Point x(int x) {
        return new Point(x, y);
      }
      public Point y(int y) {
        return with(Point::y, y);
      }
    }
    var origin = new Point(0, 0);
    assertEquals("Point[x=1, y=2]", origin.with(Point::x, 1).with(Point::y, 2).toString());
    assertEquals("Point[x=1, y=2]", origin.x(1).y(2).toString());
  }
}
