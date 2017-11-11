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

class MultipleSourceSetsTest : IntegrationTestBase()  {
  @Test
  fun `Compilation with multiple source sets`() {
    // given
    val ss1SourceDir = testProjectDir.resolve("src/ss1/clojure")
    val ss1CoreNsFile = ss1SourceDir.resolve("ss1/core.clj")
    val ss1OutputDir = testProjectDir.resolve("build/classes/ss1")

    val ss2SourceDir = testProjectDir.resolve("src/ss2/clojure")
    val ss2CoreNsFile = ss2SourceDir.resolve("ss2/core.clj")
    val ss2OutputDir = testProjectDir.resolve("build/classes/ss2")

    // when
    val result = projectBuildRunner().withArguments("check").build()

    // then
    assertThat(result.task(":compileSs1Clojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileSs2Clojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileTestClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)

    assertSourceFileIsOnlyCopiedToOutputDir(ss1CoreNsFile, ss1SourceDir, ss1OutputDir)
    assertSourceFileIsOnlyCompiledToOutputDir(ss2CoreNsFile, ss2SourceDir, ss2OutputDir)

    assertThat(result.output).contains("Test1 SourceSet1")
    assertThat(result.output).contains("Test2 SourceSet2 SourceSet1")
  }
}
