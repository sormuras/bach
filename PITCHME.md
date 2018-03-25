# What is JUnit 5?

---
 
# JUnit 5 = *JUnit Platform* + ...

- Foundation for launching testing frameworks on the JVM
- Defines and uses `TestEngine` interface

---
 
# JUnit 5 = ... + *JUnit Jupiter* + ...

- New programming model for writing tests 
- New extension model for writing extensions
- `JupiterTestEngine implements TestEngine`

---
 
# JUnit 5 = ... + *JUnit Vintage*

- Enables running JUnit 3 and JUnit 4 tests
- `VintageTestEngine implements TestEngine`

---
 
# JUnit 5 = ... + *Your Testing Framework*

- What is a test? You define it!
- How is a test evaluated? You define it!
- `FooTestEngine implements TestEngine`

---

### First Jupiter Test

```java
import org.junit.jupiter.api.*;

class FirstJUnit5Tests {

    @Test
    void myFirstTest() {
        Assertions.assertEquals(2, 1 + 1, "2 is two");
    }

}
```

@[1](Import JUnit Jupiter API)
@[3,6](Use 'package-private' modifier)
@[5-8](@Test-annotated method, also 'package-private')