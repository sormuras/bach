# What is JUnit 5?

JUnit 5 = ...
 
---
 
# What is JUnit 5?

JUnit 5 = JUnit Platform + ... 

---
 
# What is JUnit 5?

JUnit 5 = JUnit Platform + _JUnit Jupiter_ + _JUnit Vintage_

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

[@1](Import JUnit Jupiter API)
[@3,6](Use 'package-private' modifier)
[@5-8](@Test-annotated method, also 'package-private')