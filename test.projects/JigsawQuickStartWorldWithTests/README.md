# Project `JigsawQuickStartWorldWithTests`

Greetings World With Tests!

_Inspired by Project Jigsaw: [Module System Quick-Start Guide]._

This example introduces code spaces: `main` and `test`

The `main` code space contains modules that are production-relevant.

The `test` code space contains test modules that read main modules by their modular API.

By convention, the source code for modules is in a directory that is the name of the module.


```text
├───com.greetings
│   └───main/java
│       │   module-info.java      --> | module com.greetings { ← ────────────────────────────────┐
│       └───com                       |   requires org.astro;  → ──┐                             │
│           └───greetings             | }                          |                             │
│                   Main.java                      ┌───────────────┘                             │
│                                                  │                                             │
├───org.astro                                      │ ┌───────────────────────────────────────────┤
│   ├───main/java                                  ↓ ↓                                           │
│   │   │   module-info.java      --> | module org.astro {   ├───┐ copy module name              │
│   │   └───org                       |   exports org.astro; ├───┼────────┐                      │
│   │       └───astro                 | }                        │        │ copy relevant        │
│   │               World.java                                   │        │ directives from      │
│   └───test/java                                           ┌────┴────┐   │ main module          │
│       │   module-info.java      --> | open /*test*/ module org.astro {  │                      │
│       └───org                       |   exports org.astro; ├────────────┘                      │
│           └───astro                 |   provides ...ToolProvider with org.astro.TestProvider;  │
│                   TestProvider.java | }                                                        │
│                                                                                                │
└───test.modules                                                                                 │
    └───test/java                                                                                │
        │   module-info.java      --> | open /*test*/ module test.modules {                      │
        └───test                      |   requires com.greetings; → ─────────────────────────────┤
            └───modules               |   requires org.astro; → ─────────────────────────────────┘
                    TestProvider.java |   provides ...ToolProvider with test.modules.TestProvider;
                                      | }
```

[Module System Quick-Start Guide]: https://openjdk.java.net/projects/jigsaw/quick-start
