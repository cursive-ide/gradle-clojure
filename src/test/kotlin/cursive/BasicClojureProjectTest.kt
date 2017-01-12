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

class BasicClojureProjectTest : IntegrationTestBase() {
  val coreNsSourceFile = testProjectDir.resolve("src/main/clojure/basic_project/core.clj")

  @Test
  fun `Build without AOT only copies clj files to output directory`() {
    // when
    val result = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(result.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileTestClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":testClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)

    assertSourceFileIsOnlyCopiedToOutputDir(coreNsSourceFile)

    assertThat(result.output).contains("Generating message for World")
  }

  @Test
  fun `Build with AOT compiles to class files without copying clj files to output directory`() {
    // given
    projectGradleFile().appendText("compileClojure.aotCompile = true")

    // when
    val result = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(result.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileTestClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":testClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)

    assertSourceFileIsOnlyCompiledToOutputDir(coreNsSourceFile)

    assertThat(result.output).contains("Generating message for World")
  }
}
