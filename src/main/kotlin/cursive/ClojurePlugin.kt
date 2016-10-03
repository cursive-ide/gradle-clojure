/*
 * Copyright 2016 Colin Fleming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cursive

import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.ConventionTask
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.DefaultJavaForkOptions
import org.gradle.process.internal.ExecException
import org.gradle.process.internal.JavaExecHandleBuilder
import org.gradle.util.ConfigureUtil
import java.io.File
import java.io.OutputStream
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * @author Colin Fleming
 */
class ClojurePlugin : Plugin<Project> {
  val logger = Logging.getLogger(this.javaClass)

  override fun apply(project: Project) {
    logger.info("Applying ClojurePlugin")
    val javaBasePlugin = project.plugins.apply(JavaBasePlugin::class.java)
    val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)

    project.plugins.apply(JavaPlugin::class.java)

    val mainSourceSet = javaPluginConvention.sourceSets?.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
    if (mainSourceSet is HasConvention) {
      val mainCompileTask = createCompileTask(project, mainSourceSet)
      val conventionMapping = mainCompileTask.conventionMapping
      conventionMapping.map("classpath", {
        mainSourceSet.compileClasspath
            .plus(SimpleFileCollection(mainSourceSet.allSource.srcDirs))
            .plus(SimpleFileCollection(mainSourceSet.output.classesDir))
      })
      conventionMapping.map("namespaces", {
        mainCompileTask.findNamespaces()
      })
    }

    val testSourceSet = javaPluginConvention.sourceSets?.getByName(SourceSet.TEST_SOURCE_SET_NAME)
    if (testSourceSet is HasConvention) {
      val testCompileTask = createCompileTask(project, testSourceSet)

      val mainSrcDirs = mainSourceSet?.allSource?.srcDirs ?: emptyList<File>()
      val testSrcDirs = testSourceSet.allSource.srcDirs
      val outputDirs = if (mainSourceSet != null)
        listOf(mainSourceSet.output.classesDir, testSourceSet.output.classesDir)
      else
        listOf(testSourceSet.output.classesDir)

      val compileMapping = testCompileTask.conventionMapping
      compileMapping.map("classpath", {
        testSourceSet.compileClasspath
            .plus(SimpleFileCollection(mainSrcDirs + testSrcDirs + outputDirs))
      })
      compileMapping.map("namespaces", {
        testCompileTask.findNamespaces()
      })

      val testTask = createTestTask(project)

      val testRunnerMapping = testTask.conventionMapping
      testRunnerMapping.map("classpath", {
        testSourceSet.runtimeClasspath.plus(SimpleFileCollection(mainSrcDirs + testSrcDirs + outputDirs))
      })
      testRunnerMapping.map("namespaces", {
        testCompileTask.findNamespaces()
      })

      testTask.dependsOn(testCompileTask.name)
    }
  }

  fun createCompileTask(project: Project, sourceSet: SourceSet): ClojureCompiler {
    val projectInternal = project as ProjectInternal
    val sourceRootDir: String = "src/${sourceSet.name}/clojure"

    logger.info("Creating DefaultSourceDirectorySet for source set $sourceSet")
    val clojureSrcSet = ClojureSourceSetImpl(sourceSet.name, projectInternal.fileResolver)
    val clojureDirSet = clojureSrcSet.getClojure()
    DslObject(sourceSet).convention.plugins.put("clojure", clojureSrcSet)

    val srcDir = project.file(sourceRootDir)
    logger.info("Creating Clojure SourceDirectorySet for source set $sourceSet with src dir $srcDir")
    clojureDirSet.srcDir(srcDir)

    logger.info("Adding Clojure SourceDirectorySet $clojureDirSet to source set $sourceSet")
    sourceSet.allSource?.source(clojureDirSet)
    sourceSet.resources?.filter?.exclude { clojureDirSet.contains(it.file) }

    val name = sourceSet.getCompileTaskName("clojure")
    val compilerClass = ClojureCompiler::class.java
    logger.info("Creating Clojure compile task $name with class $compilerClass")
    val compile = project.tasks.create(name, compilerClass)
    compile.description = "Compiles the $sourceSet Clojure code"

    val javaTask = project.tasks.findByName(sourceSet.compileJavaTaskName)
    if (javaTask != null) {
      compile.dependsOn(javaTask.name)
    }

    project.tasks.findByName(sourceSet.classesTaskName)?.dependsOn(compile.name)

    val conventionMapping = compile.conventionMapping
    conventionMapping.map("destinationDir", {
      sourceSet.output.classesDir
    })

    compile.source(clojureDirSet)

    return compile
  }
}

fun createTestTask(project: Project): ClojureTestRunner {
  val name = "testClojure"
  val testRunnerClass = ClojureTestRunner::class.java

  val testRunner = project.tasks.create(name, testRunnerClass)
  project.tasks.getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(testRunner)
  testRunner.description = "Runs the Clojure tests"
  testRunner.group = JavaBasePlugin.VERIFICATION_GROUP

  testRunner.outputs.upToDateWhen { false }

  return testRunner
}


interface ClojureSourceSet {
  fun getClojure(): SourceDirectorySet
  fun clojure(configureClosure: Closure<Any?>?): ClojureSourceSet
}


open class ClojureSourceSetImpl(displayName: String?, resolver: FileResolver?) : ClojureSourceSet {
  private val clojure = ApiFacade.sourceDirectorySet(displayName + " Clojure source", resolver)

  init {
    clojure.filter.include("**/*.clj", "**/*.cljc")
  }

  override fun getClojure(): SourceDirectorySet = clojure

  override fun clojure(configureClosure: Closure<Any?>?): ClojureSourceSet {
    ApiFacade.configureByClosure(this, configureClosure)
    return this
  }
}

class ReflectionWarnings(var enabled: Boolean, var projectOnly: Boolean, var asErrors: Boolean)

open class ClojureCompiler @Inject constructor(val fileResolver: FileResolver) :
    AbstractCompile(),
    JavaForkOptions by DefaultJavaForkOptions(fileResolver) {

  var aotCompile: Boolean = false
  var copySourceToOutput: Boolean? = null
  var reflectionWarnings = ReflectionWarnings(false, false, false)

  var namespaces: Collection<String> = emptyList()

  fun reflectionWarnings(configureClosure: Closure<Any?>?): ReflectionWarnings {
    ConfigureUtil.configure(configureClosure, reflectionWarnings)
    return reflectionWarnings
  }

  @TaskAction
  override fun compile() {
    logger.info("Starting ClojureCompiler task")

    if (copySourceToOutput ?: !aotCompile) {
      project.copy {
        it.from(getSource()).into(destinationDir)
      }
      return
    }

    if (aotCompile) {
      logger.info("Destination: $destinationDir")

      // Required (I think) because Kotlin accesses the field directly (not the getter) from the same class
      val namespaces = conventionMapping.getConventionValue<List<String>>(emptyList(), "namespaces", false)
      if (namespaces.isEmpty()) {
        logger.info("No Clojure namespaces defined, skipping $name")
        return
      }

      logger.info("Compiling " + namespaces.joinToString(", "))

      val script = listOf("(try",
                          "  (binding [*compile-path* \"${destinationDir.canonicalPath}\"",
                          "            *warn-on-reflection* ${reflectionWarnings.enabled}]",
                          "    " + namespaces.map { "(compile '$it)" }.joinToString("\n    ") + ")",
                          "  (catch Throwable e",
                          "    (.printStackTrace e)",
                          "    (System/exit 1)))",
                          "(System/exit 0)")
          .joinToString("\n")

      //    println(script)

      val stdout = object : LineProcessingOutputStream() {
        override fun processLine(line: String) {
          System.out.print(line)
        }
      }

      val sourceRoots = getSourceRoots()

      var reflectionWarningCount = 0
      var libraryReflectionWarningCount = 0

      val stderr = object : LineProcessingOutputStream() {
        override fun processLine(line: String) {
          if (line.startsWith(REFLECTION_WARNING_PREFIX)) {
            if (reflectionWarnings.projectOnly) {
              val colon = line.indexOf(':')
              val file = line.substring(REFLECTION_WARNING_PREFIX.length, colon)
              val found = sourceRoots.find { File(it, file).exists() } != null
              if (found) {
                reflectionWarningCount++
                System.err.print(line)
              } else {
                libraryReflectionWarningCount++
              }
            } else {
              reflectionWarningCount++
              System.err.print(line)
            }
          } else {
            System.err.print(line)
          }
        }
      }

      executeScript(script, stdout, stderr)

      if (libraryReflectionWarningCount > 0) {
        System.err.println("$libraryReflectionWarningCount reflection warnings from dependencies")
      }
      if (reflectionWarnings.asErrors && reflectionWarningCount > 0) {
        throw ExecException("$reflectionWarningCount reflection warnings found")
      }
    }
  }

  private fun executeScript(script: String, stdout: OutputStream, stderr: OutputStream) {
    val file = createTempFile("clojure-compiler", ".clj", temporaryDir)
    file.bufferedWriter().use { out ->
      out.write("$script\n")
    }

    //    println("Classpath: " + classpath.joinToString(", "))

    val exec = JavaExecHandleBuilder(fileResolver)
    copyTo(exec)
    exec.main = "clojure.main"
    exec.classpath = classpath
    exec.setArgs(listOf("-i", file.canonicalPath))
    exec.defaultCharacterEncoding = "UTF8"

    exec.standardOutput = stdout
    exec.errorOutput = stderr

    val result = exec.build().start().waitForFinish()

    stdout.close()
    stderr.close()

    result.assertNormalExitValue()
  }

  fun findNamespaces(): List<String> {

    fun findNamespace(file: File, roots: Set<String>): String {
      var current = file.parentFile
      var namespace = demunge(file.name.substring(0, file.name.lastIndexOf('.')))
      while (current != null) {
        if (roots.contains(current.canonicalPath)) {
          return namespace
        }
        namespace = demunge(current.name) + '.' + namespace
        current = current.parentFile
      }
      throw RuntimeException("No source root found for ${file.canonicalPath}")
    }

    val sources = getSource()
    //    println("Sources: " + sources.joinToString(", "))
    val roots = getSourceRoots()
    //    println("Roots: " + roots.joinToString(", "))
    val namespaces = sources.map { findNamespace(it, roots) }.toList()
    return namespaces
  }

  private fun getSourceRoots(): HashSet<String> {
    val roots = source
        .filter { it is SourceDirectorySet }
        .flatMap { (it as SourceDirectorySet).srcDirs }
        .map { it.canonicalPath }
        .toHashSet()
    return roots
  }

  companion object {
    val CHAR_MAP = mapOf('-' to "_",
                         ':' to "_COLON_",
                         '+' to "_PLUS_",
                         '>' to "_GT_",
                         '<' to "_LT_",
                         '=' to "_EQ_",
                         '~' to "_TILDE_",
                         '!' to "_BANG_",
                         '@' to "_CIRCA_",
                         '#' to "_SHARP_",
                         '\'' to "_SINGLEQUOTE_",
                         '"' to "_DOUBLEQUOTE_",
                         '%' to "_PERCENT_",
                         '^' to "_CARET_",
                         '&' to "_AMPERSAND_",
                         '*' to "_STAR_",
                         '|' to "_BAR_",
                         '{' to "_LBRACE_",
                         '}' to "_RBRACE_",
                         '[' to "_LBRACK_",
                         ']' to "_RBRACK_",
                         '/' to "_SLASH_",
                         '\\' to "_BSLASH_",
                         '?' to "_QMARK_")

    val DEMUNGE_MAP = CHAR_MAP.map { it.value to it.key }.toMap()
    val DEMUNGE_PATTERN = Pattern.compile(DEMUNGE_MAP.keys
                                              .sortedByDescending { it.length }
                                              .map { "\\Q$it\\E" }
                                              .joinToString("|"))

    val REFLECTION_WARNING_PREFIX = "Reflection warning, "

    fun munge(name: String): String {
      val sb = StringBuilder()
      for (c in name) {
        if (CHAR_MAP.containsKey(c))
          sb.append(CHAR_MAP[c])
        else
          sb.append(c)
      }
      return sb.toString()
    }

    fun demunge(mungedName: String): String {
      val sb = StringBuilder()
      val m = DEMUNGE_PATTERN.matcher(mungedName)
      var lastMatchEnd = 0
      while (m.find()) {
        val start = m.start()
        val end = m.end()
        // Keep everything before the match
        sb.append(mungedName.substring(lastMatchEnd, start))
        lastMatchEnd = end
        // Replace the match with DEMUNGE_MAP result
        val origCh = DEMUNGE_MAP[m.group()]
        sb.append(origCh)
      }
      // Keep everything after the last match
      sb.append(mungedName.substring(lastMatchEnd))
      return sb.toString()
    }
  }
}


open class ClojureTestRunner @Inject constructor(val fileResolver: FileResolver) :
    ConventionTask(),
    JavaForkOptions by DefaultJavaForkOptions(fileResolver) {

  var classpath: FileCollection = SimpleFileCollection()
  var namespaces: Collection<String> = emptyList()
  var junitReport: File? = null

  @TaskAction
  fun test() {
    logger.info("Starting ClojureTestRunner task")

    val namespaces = conventionMapping.getConventionValue<List<String>>(emptyList(), "namespaces", false)
    if (namespaces.isEmpty()) {
      logger.info("No Clojure namespaces defined, skipping $name")
      return
    }

    logger.info("Testing " + namespaces.joinToString(", "))

    val namespaceVec = namespaces.joinToString(" ", "'[", "]")
    val runnerInvocation = if (junitReport != null)
      """(run-tests $namespaceVec "${junitReport?.absolutePath}")"""
    else
      """(run-tests $namespaceVec)"""

    val script = "$testRunnerScript\n$runnerInvocation"

    //    println(script)

    executeScript(script)
  }

  private fun executeScript(script: String) {
    val file = createTempFile("clojure-compiler", ".clj", temporaryDir)
    file.bufferedWriter().use { out ->
      out.write("$script\n")
    }

    val classpath = conventionMapping.getConventionValue<FileCollection>(SimpleFileCollection(), "classpath", false)

    //    println("Classpath: " + classpath.joinToString(", "))

    val exec = JavaExecHandleBuilder(fileResolver)
    copyTo(exec)
    exec.main = "clojure.main"
    exec.classpath = classpath
    exec.setArgs(listOf("-i", file.canonicalPath))

    exec.build().start().waitForFinish().assertNormalExitValue()
  }

  companion object {
    val testRunnerScript =
            ClojureTestRunner::class.java.getResourceAsStream("/cursive/test_runner.clj").bufferedReader().use { it.readText() }
  }
}
