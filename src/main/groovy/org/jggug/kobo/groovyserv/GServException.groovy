/*
 * Copyright 2009-2011 the original author or authors.
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
package org.jggug.kobo.groovyserv


/**
 * @author NAKANO Yasuharu
 */
class GServException extends RuntimeException {

    int exitStatus

    GServException(String message, Throwable e = null) {
        this(ExitStatus.UNEXPECTED_ERROR.code, message, e)
    }

    GServException(int exitStatus, String message, Throwable e = null) {
        super(message, e)
        this.exitStatus = exitStatus
    }

}
