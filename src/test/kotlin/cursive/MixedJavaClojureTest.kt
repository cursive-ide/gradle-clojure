/*
 * Copyright 2017 Colin Fleming
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

import org.assertj.core.api.KotlinAssertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class MixedJavaClojureTest : IntegrationTestBase() {
  val javaOutputDir = testProjectDir.resolve("build/classes/javaSS")
  val javaExample1SourceFile = testProjectDir.resolve("src/javaSS/java/javaSS/Example1.java")
  val javaExample1ClassFile = javaOutputDir.resolve("javaSS/Example1.class")
  val javaExample2ClassFile = javaOutputDir.resolve("javaSS/Example2.class")

  val cljSourceDir = testProjectDir.resolve("src/cljSS/clojure")
  val cljCoreNsFile = cljSourceDir.resolve("cljSS/core.clj")
  val cljOutputDir = testProjectDir.resolve("build/classes/cljSS")

  @Test
  fun `Compilation with Clojure code depending on Java code`() {
    // when
    val result = projectBuildRunner().withArguments("compileCljSSClojure").build()

    // then
    assertThat(result.task(":compileJavaSSJava").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileCljSSClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)

    assertThat(javaExample1ClassFile.exists()).isTrue()
    assertThat(javaExample2ClassFile.exists()).isTrue()
    assertSourceFileIsOnlyCompiledToOutputDir(cljCoreNsFile, cljSourceDir, cljOutputDir)
  }

  @Test
  fun `Incremental compilation with Clojure code depending on Java code when Java source unchanged`() {
    // when
    val firstResult = projectBuildRunner().withArguments("compileCljSSClojure").build()

    // then
    assertThat(firstResult.task(":compileJavaSSJava").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstResult.task(":compileCljSSClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)

    assertThat(javaExample1ClassFile.exists()).isTrue()
    assertThat(javaExample2ClassFile.exists()).isTrue()
    assertSourceFileIsOnlyCompiledToOutputDir(cljCoreNsFile, cljSourceDir, cljOutputDir)

    // when
    val secondResult = projectBuildRunner().withArguments("compileCljSSClojure").build()

    // then
    println(secondResult.output)
    assertThat(secondResult.task(":compileJavaSSJava").outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondResult.task(":compileCljSSClojure").outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

    assertThat(javaExample1ClassFile.exists()).isTrue()
    assertThat(javaExample2ClassFile.exists()).isTrue()
    assertSourceFileIsOnlyCompiledToOutputDir(cljCoreNsFile, cljSourceDir, cljOutputDir)
  }

  @Test
  fun `Incremental compilation with Clojure code depending on Java code when Java source changes`() {
    // when
    val firstResult = projectBuildRunner().withArguments("compileCljSSClojure").build()

    // then
    assertThat(firstResult.task(":compileJavaSSJava").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstResult.task(":compileCljSSClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)

    assertThat(javaExample1ClassFile.exists()).isTrue()
    assertThat(javaExample2ClassFile.exists()).isTrue()
    assertSourceFileIsOnlyCompiledToOutputDir(cljCoreNsFile, cljSourceDir, cljOutputDir)

    // when
    javaExample1SourceFile.delete()
    val secondResult = projectBuildRunner().withArguments("compileCljSSClojure").buildAndFail()

    // then
    assertThat(secondResult.task(":compileJavaSSJava").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(secondResult.task(":compileCljSSClojure").outcome).isEqualTo(TaskOutcome.FAILED)

    assertThat(secondResult.output).contains("java.lang.ClassNotFoundException: javaSS.Example1")
  }
}