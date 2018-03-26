# What is JUnit 5?

---
 
##### JUnit 5 = JUnit Platform + ...

- Foundation for launching testing frameworks |
- Defines and uses `TestEngine` interface     |

---
 
##### JUnit 5 = ... JUnit Jupiter + ...

- New programming model for writing tests
- New extension model for writing extensions
- `JupiterTestEngine implements TestEngine`

---
 
##### JUnit 5 = ... JUnit Vintage + ...

- Enables running JUnit 3 and JUnit 4 tests
- `VintageTestEngine implements TestEngine`

---

##### JUnit 5 = ... + Your Testing Framework

- What is a test? You define it!
- How is a test evaluated? You define it!
- `YourTestEngine implements TestEngine`
- Get tooling support for granted.

---

# This is JUnit 5

// big picture from PDF

// now into the red box "Jupiter"

---

# Jupiter

- First Jupiter Test
- Annotations
- Standard Test Class
- Display Names
- Assertions
- Assumptions
- Disabling Tests
- Conditional Test Execution
- Tagging and Filtering
- Test Instance Lifecycle
- Nested Tests
- Dependency Injection for Constructors and Methods
- Test Interfaces and Default Methods
- Repeated Tests
- Parameterized Tests
- Test Templates
- Dynamic Tests

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