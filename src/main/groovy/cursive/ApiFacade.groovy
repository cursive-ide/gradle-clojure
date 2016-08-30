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

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.util.GradleVersion
import org.gradle.util.ConfigureUtil
import org.jetbrains.annotations.NotNull

/**
 * This class is written in Groovy to allow multiple API versions to be handled
 * API examples from https://github.com/eriwen/gradle-js-plugin/blob/master/src/main/groovy/com/eriwen/gradle/js/source/internal/DefaultJavaScriptSourceSet.groovy
 * @author Colin Fleming
 */
class ApiFacade  {

  public static @NotNull SourceDirectorySet sourceDirectorySet(String name, FileResolver resolver) {
    if (GradleVersion.current() >= GradleVersion.version("2.12")) {
      def fileTreeFactory = Class.forName("org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory")
      def directoryFileTreeFactory = fileTreeFactory.newInstance()
      return new DefaultSourceDirectorySet(name, resolver, directoryFileTreeFactory)
    } else {
      return new DefaultSourceDirectorySet(name, resolver)
    }
  }

  public static void configureByClosure(Object object, Closure closure) {
    if (GradleVersion.current() >= GradleVersion.version("2.14")) {
      ConfigureUtil.configureSelf(closure, object)
    } else {
      ConfigureUtil.configure(closure, object, false)
    }
  }
}
