# Scaffold Project

A simple modular Java project containing modular tests.

## main realm

- module `scaffold` contains 2 packages: `scaffold` and `scaffold.api`

## test: 2 test modules

- module `integration` contains package `integration`
- module `scaffold` contains 2 packages: `scaffold` and `scaffold.api`

```text
└───src
    ├───main
    │   └───scaffold
    │       └───scaffold
    │           └───api
    └───test
        ├───integration
        │   └───integration
        └───scaffold
            └───scaffold
                └───api
```

## compile

`bach build`

## run

```text
java --module-path bin\compiled\main;.bach\modules --module scaffold/scaffold.ScaffoldMain

R:\dev\github\sormuras\bach\demo\scaffold\.bach
R:\dev\github\sormuras\bach\demo\scaffold\bin
R:\dev\github\sormuras\bach\demo\scaffold\README.md
R:\dev\github\sormuras\bach\demo\scaffold\src
```