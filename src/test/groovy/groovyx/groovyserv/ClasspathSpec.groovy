/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.groovyserv

import groovyx.groovyserv.test.IntegrationTest
import groovyx.groovyserv.test.TestUtils
import spock.lang.Specification

@IntegrationTest
class ClasspathSpec extends Specification {

    def "passing classpath from environment variables"() {
        given:
        def args = ["-e", '"new EnvEcho().echo(\'hello\')"']
        def env = [CLASSPATH: resolvePath('ForClasspathIT_env.jar')]

        when:
        def result = TestUtils.executeClientCommandWithEnv(args, env)

        then:
        result.out.contains("Env:hello")
        result.err == ""
    }

    def "passing classpath from arguments"() {
        given:
        def args = ["--classpath", resolvePath("ForClasspathIT_arg.jar"), "-e", '"new ArgEcho().echo(\'hello\')"']

        when:
        def result = TestUtils.executeClientCommand(args)

        then:
        result.out.contains("Arg:hello")
        result.err == ""
    }

    def "using argument when classpath is passed from both environment variables and arguments"() {
        given:
        def args = ["--classpath", resolvePath("ForClasspathIT_arg.jar"), "-e", '"new ArgEcho().echo(\'hello\')"']
        def env = [CLASSPATH: resolvePath('ForClasspathIT_env.jar')]

        when:
        def result = TestUtils.executeClientCommandWithEnv(args, env)

        then:
        result.out.contains("Arg:hello")
        result.err == ""
    }

    def "propagated classpath is disposed each invocation (except for shell client)"() {
        given:
        def args = ["-e", '"new ArgEcho().echo(\'hello\')"']

        when:
        def result = TestUtils.executeClientCommand(args)

        then:
        result.out == ""
        result.err.contains("org.codehaus.groovy.control.MultipleCompilationErrorsException")
    }

    private static resolvePath(jarFileName) {
        def per = File.separatorChar
        "${System.properties.'user.dir'}${per}src${per}test${per}resources${per}${jarFileName}"
    }
}
