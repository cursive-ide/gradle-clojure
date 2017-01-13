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
import org.junit.Assert.assertFalse
import org.junit.Test

class IncrementalCompilationTest : IntegrationTestBase() {
  val coreNsSourceFile = testProjectDir.resolve("src/main/clojure/basic_project/core.clj")

  @Test
  fun `Incremental compile task without AOT is up-to-date when no input changes`() {
    // when
    val firstRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(firstRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertSourceFileIsOnlyCopiedToOutputDir(coreNsSourceFile)

    // when
    val secondRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(secondRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertSourceFileIsOnlyCopiedToOutputDir(coreNsSourceFile)
  }

  @Test
  fun `Incremental compile task with AOT is up-to-date when no input changes`() {
    // given
    projectGradleFile().appendText("compileClojure.aotCompile = true")

    // when
    val firstRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(firstRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertSourceFileIsOnlyCompiledToOutputDir(coreNsSourceFile)

    // when
    val secondRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(secondRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertSourceFileIsOnlyCompiledToOutputDir(coreNsSourceFile)
  }

  @Test
  fun `Incremental compile task without AOT processes outdated source files when input changes`() {
    // given
    val utilsNsSourceFile = testProjectDir.resolve("src/main/clojure/basic_project/utils.clj")

    // when
    val firstRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(firstRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertSourceFileIsOnlyCopiedToOutputDir(coreNsSourceFile)
    assertSourceFileNotCopiedToOutputDir(utilsNsSourceFile)
    assertSourceFileNotCompiledToOutputDir(utilsNsSourceFile)

    coreNsSourceFile.delete()
    utilsNsSourceFile.writeText("""(ns basic-project.utils) (defn ping [] "pong")""")

    // when
    val secondRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(secondRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertSourceFileNotCopiedToOutputDir(coreNsSourceFile)
    assertSourceFileNotCompiledToOutputDir(coreNsSourceFile)
    assertSourceFileIsOnlyCopiedToOutputDir(utilsNsSourceFile)
  }

  @Test
  fun `Incremental compile task with AOT processes outdated source files when input changes`() {
    // given
    projectGradleFile().appendText("compileClojure.aotCompile = true")
    val utilsNsSourceFile = testProjectDir.resolve("src/main/clojure/basic_project/utils.clj")

    // when
    val firstRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(firstRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertSourceFileCompiledToOutputDir(coreNsSourceFile)
    assertSourceFileNotCopiedToOutputDir(utilsNsSourceFile)
    assertSourceFileNotCompiledToOutputDir(utilsNsSourceFile)

    coreNsSourceFile.delete()
    utilsNsSourceFile.writeText("""(ns basic-project.utils) (defn ping [] "pong")""")

    // when
    val secondRunResult = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(secondRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertSourceFileNotCopiedToOutputDir(coreNsSourceFile)
    assertSourceFileNotCompiledToOutputDir(coreNsSourceFile)
    assertSourceFileIsOnlyCompiledToOutputDir(utilsNsSourceFile)
    assertFalse("Protocol class file doesn't exists", testProjectDir.resolve("build/classes/main/basic_project/core/ITest.class").exists())
  }
}
