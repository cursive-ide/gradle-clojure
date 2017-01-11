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

class MixedClojureJavaTest : IntegrationTestBase()  {
  @Test
  fun compilationWithJavaCodeDependingOnClojureCode() {
    // given
    val cljSourceDir = testProjectDir.resolve("src/cljSS/clojure")
    val cljExampleNsFile = cljSourceDir.resolve("cljSS/Example.clj")
    val cljOutputDir = testProjectDir.resolve("build/classes/cljSS")

    val javaOutputDir = testProjectDir.resolve("build/classes/javaSS")
    val javaClassFile = javaOutputDir.resolve("javaSS/Test.class")

    // when
    val result = projectBuildRunner().withArguments("compileJavaSSJava").build()

    // then
    assertThat(result.task(":compileCljSSClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileJavaSSJava").outcome).isEqualTo(TaskOutcome.SUCCESS)

    assertSourceFileIsOnlyCompiledToOutputDir(cljExampleNsFile, cljSourceDir, cljOutputDir)
    assertThat(javaClassFile.exists()).isTrue()
  }
}
