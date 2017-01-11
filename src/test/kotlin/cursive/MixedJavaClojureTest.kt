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
  @Test
  fun compilationWithClojureCodeDependingOnJavaCode() {
    // given
    val javaOutputDir = testProjectDir.resolve("build/classes/javaSS")
    val javaClassFile = javaOutputDir.resolve("javaSS/Example.class")

    val cljSourceDir = testProjectDir.resolve("src/cljSS/clojure")
    val cljCoreNsFile = cljSourceDir.resolve("cljSS/core.clj")
    val cljOutputDir = testProjectDir.resolve("build/classes/cljSS")

    // when
    val result = projectBuildRunner().withArguments("compileCljSSClojure").build()

    // then
    assertThat(result.task(":compileJavaSSJava").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileCljSSClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)

    assertThat(javaClassFile.exists()).isTrue()
    assertSourceFileIsOnlyCompiledToOutputDir(cljCoreNsFile, cljSourceDir, cljOutputDir)
  }
}