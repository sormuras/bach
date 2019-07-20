# Test Project `requires-asm`

Sync asm module, build main module, and jdeps suggests to drop asm.

## Expected Output

```test
Bach.java 2-ea building missing-module 1.0.0-SNAPSHOT
>> ... >>
No test modules declared -- skip compilation.
No test module available for: a
>> jar("--describe-module", "--file", "bin\main\modules\a-1.0.0-SNAPSHOT.jar")
Running provided tool: sun.tools.jar.JarToolProvider@6f9e08d4
a@1.0.0-SNAPSHOT jar:file:///.../bach/src/test-project/requires-asm/bin/main/modules/a-1.0.0-SNAPSHOT.jar/!module-info.class
requires java.base mandated
requires org.objectweb.asm

>> jdeps("--multi-release", "base", "--module-path", "bin\main\modules;lib", "--check", "a")
Running provided tool: com.sun.tools.jdeps.Main$JDepsToolProvider@3d53e6f7
a (file:///.../bach/src/test-project/requires-asm/bin/main/modules/a-1.0.0-SNAPSHOT.jar)
  [Module descriptor]
    requires mandated java.base (@13-ea);
    requires org.objectweb.asm (@7.2.0.beta);
  [Suggested module descriptor for a]
    requires mandated java.base;
  [Transitive reduced graph for a]
    requires mandated java.base;

1 main module(s) created in file:///.../bach/src/test-project/requires-asm/bin/main/modules/
 ->       635 a-1.0.0-SNAPSHOT.jar <- module { name: a@1.0.0-SNAPSHOT, [org.objectweb.asm (@7.2.0.beta), mandated java.base (@13-ea)] }
Build successful.
```
