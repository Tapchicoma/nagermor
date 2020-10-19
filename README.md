## Description

Proof of concept of incremental java sources compilation approach.

Includes both CLI tool and sample gradle plugin.

### Unsupported java features

Currently implementation misses support for following Java features:
- Java 9+ modules
- Tracking changes in jvm version

### CLI tool

To build command line tool run following command:
```shell
./gradlew :cli:installDist
```

Then you could run CLI via following command:
```shell
./cli/build/install/nagermor/lib/nagermor --help
```

Caches are stored in `<user-folder>/.nagermor` directory.

### Gradle plugin

Sources are located in `:gradle-plugin` module.

Usage (not published to Gradle plugin portal):
```gradle
plugins {
  id("by.egorr.nagermor")
}
```

Plugin will create `compile*WithNagermor` tasks.

### Compilation

To compile this project you need to use Java 11+.
