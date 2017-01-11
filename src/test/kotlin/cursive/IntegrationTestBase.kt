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

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*

open class IntegrationTestBase {
  @Rule
  @JvmField
  val tempDir: TemporaryFolder = initTempDir()
  val testProjectDir: File = tempDir.root

  @Before
  fun setup() {
    val sourceDirectory = File("src/test/int-tests-projects", this.javaClass.name.replace('.', '/'))

    if (sourceDirectory.exists()) {
      sourceDirectory.copyRecursively(target = testProjectDir, overwrite = true)
    } else {
      println("Source directory ${sourceDirectory.absolutePath} doesn't exist")
    }
  }

  fun projectBuildRunner(): GradleRunner {
    return GradleRunner.create().withPluginClasspath().withProjectDir(testProjectDir)
  }

  fun projectGradleFile(): File {
    return testProjectDir.resolve("build.gradle")
  }

  fun buildDirFiles(): List<File> {
    return testProjectDir.resolve("build").walkTopDown().toList()
  }

  fun assertSourceFileCopiedToOutputDir(sourceFile: File, sourceBaseDir: File = testProjectDir.resolve("src/main/clojure"), destinationDir: File = testProjectDir.resolve("build/classes/main")) {
    val expectedDestinationFile = destinationFileForSourceFile(sourceFile, sourceBaseDir, destinationDir)
    if (!expectedDestinationFile.exists()) {
      fail("Expected destination file doesn't exist for source file ${sourceFile}")
    }
    val sourceFileContents = sourceFile.readText()
    val destinationFileContents = expectedDestinationFile.readText()
    assertEquals("Source and destination files content", sourceFileContents, destinationFileContents)
  }

  fun assertSourceFileCompiledToOutputDir(sourceFile: File, sourceBaseDir: File = testProjectDir.resolve("src/main/clojure"), destinationDir: File = testProjectDir.resolve("build/classes/main")) {
    val classFiles = compiledClassFilesForSourceFile(sourceFile, sourceBaseDir, destinationDir)
    if (classFiles.isEmpty()) {
      fail("No compiled files have been found for source file ${sourceFile}")
    }
  }

  fun destinationFileForSourceFile(sourceFile: File, sourceBaseDir: File = testProjectDir.resolve("src/main/clojure"), destinationDir: File = testProjectDir.resolve("build/classes/main")): File {
    val relativeSourceFile = sourceFile.relativeTo(sourceBaseDir)
    val expectedDestinationFile = destinationDir.resolve(relativeSourceFile)
    return expectedDestinationFile
  }

  fun compiledClassFilesForSourceFile(sourceFile: File, sourceBaseDir: File = testProjectDir.resolve("src/main/clojure"), destinationDir: File = testProjectDir.resolve("build/classes/main")): List<File> {
    val relativeSourceFile = sourceFile.relativeTo(sourceBaseDir)
    val relativeSourceFileDir = relativeSourceFile.parentFile
    val sourceFileName = sourceFile.nameWithoutExtension

    return destinationDir.resolve(relativeSourceFileDir).listFiles { file -> file.nameWithoutExtension.startsWith(sourceFileName) }?.toList() ?: Collections.emptyList()
  }

  private fun initTempDir(): TemporaryFolder {
    val tmpDir = TemporaryFolder()
    tmpDir.create()
    return tmpDir
  }
}
