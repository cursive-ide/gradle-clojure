# gradle-clojure #

A deliberately minimal Gradle plugin for Clojure compilation and test running.

## Quick Start ##

```groovy
plugins {
  id "com.cursive-ide.clojure" version "1.0.1"
}

compileClojure {
  aotCompile = true            // Defaults to false
  copySourceToOutput = false   // Defaults to !aotCompile

  reflectionWarnings {
    enabled = true             // Defaults to false
    projectOnly = true         // Only show warnings from your project, not dependencies - default false
    asErrors = true            // Treat reflection warnings as errors and fail the build
                               // If projectOnly is true, only warnings from your project are errors.
  }

  // Compiler options for AOT
  disableLocalsClearing = true                 // Defaults to false
  elideMeta = ['doc', 'file', 'line', 'added'] // Defaults to []
  directLinking = true                         // Defaults to false

  // compileClojure implements the standard JavaForkOptions interface, and thus supports the
  // standard Gradle mechanisms for configuring a Java process:
  // systemProperty systemProperties minHeapSize maxHeapSize
  // jvmArgs bootstrapClasspath classpath enableAssertions debug environment

  systemProperty 'java.awt.headless', true
  maxHeapSize '2048m'
  jvmArgs '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005'
}

compileTestClojure {
  // compileTestClojure accepts the same options as compileClojure, but you're unlikely to AOT
  // compile your tests

  // Select the files for testing using the standard Gradle include/exclude mechanisms
  exclude 'cursive/**/*generative*'
}

testClojure {
  // Standard JVM execution options here for test process
  systemProperty 'java.awt.headless', true

  // Specifying junitReport will trigger JUnit XML report generation
  // in addition to standard console output (turned off by default)
  junitReport = file("$buildDir/reports/junit-report.xml")
}
```

This plugin assumes you're using a sane layout for your Clojure code - namespaces corresponding
to your source code layout, and one namespace per file. The plugin uses the filenames to 
calculate the namespaces involved, it does not parse the files looking for `ns` forms.

This plugin currently only implements compilation and test running. More features may be added,
but features provided by Gradle itself will not be (uberjarring, project publishing). I don't
use those features myself, examples of build script snippets to perform them for the doc would
be very welcome.

There is currently no functionality for running a REPL - you'll need to run an application which
starts an nREPL server, or something similar.

## Planned Features ##

1. Android support - the Kotlin plugin has an example of this. I'm planning to add this soon so 
   that Cursive can be used with Android Studio.
2. Code execution support with project classpath.

## Licence ##

Apache License, Version 2.0
