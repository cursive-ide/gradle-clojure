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

class JavaAndClojureInOneSourceSetTest : IntegrationTestBase() {
  val coreNsSourceFile = testProjectDir.resolve("src/main/clojure/clj_example/core.clj")
  val utilsNsSourceFile = testProjectDir.resolve("src/main/clojure/clj_example/utils.clj")
  val javaClassFile = testProjectDir.resolve("build/classes/main/javaExample/Example.class")

  @Test
  fun `Incremental compile task does not remove Java class files`() {
    // when
    val firstRunResult = projectBuildRunner().withArguments("build").build()

    // then
    assertThat(firstRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRunResult.task(":compileJava").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertSourceFileIsOnlyCompiledToOutputDir(coreNsSourceFile)
    assertThat(javaClassFile.exists()).isTrue()

    coreNsSourceFile.delete()
    utilsNsSourceFile.writeText("""(ns clj-example.utils) (defn ping [] "pong")""")

    // when
    val secondRunResult = projectBuildRunner().withArguments("build").build()

    // then
    assertThat(secondRunResult.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(secondRunResult.task(":compileJava").outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertSourceFileNotCopiedToOutputDir(coreNsSourceFile)
    assertSourceFileNotCompiledToOutputDir(coreNsSourceFile)
    assertSourceFileIsOnlyCompiledToOutputDir(utilsNsSourceFile)
    assertThat(javaClassFile.exists()).isTrue()
  }
}
