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

import org.assertj.core.api.KotlinAssertions
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class TestFailureFailsBuildTest : IntegrationTestBase() {
  @Test
  fun `testClojure task failure fails build`() {
    // when
    val result = projectBuildRunner().withArguments("check").buildAndFail()

    // then
    KotlinAssertions.assertThat(result.task(":compileClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    KotlinAssertions.assertThat(result.task(":compileTestClojure").outcome).isEqualTo(TaskOutcome.SUCCESS)
    KotlinAssertions.assertThat(result.task(":testClojure").outcome).isEqualTo(TaskOutcome.FAILED)
  }
}
