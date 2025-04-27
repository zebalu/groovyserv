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

import groovyx.groovyserv.utils.IOUtils
import groovyx.groovyserv.utils.LogUtils

/**
 * GroovyServ's client implemented by Groovy.
 *
 * @author Yasuharu NAKANO
 */
class GroovyClient {

    // in response, output of "println" depends on "line.separator" system property
    private static final String SERVER_SIDE_SEPARATOR = System.getProperty("line.separator")

    private String host
    private int port

    private String authtoken
    private Socket socket
    private InputStream ins
    private OutputStream out

    List<String> environments = []

    private List<Byte> outBytes = []
    private List<Byte> errBytes = []
    Integer exitStatus

    GroovyClient(String host = "localhost", int port = GroovyServer.DEFAULT_PORT) {
        this.host = host
        this.port = port
    }

    GroovyClient run(String... args) {
        checkActive()
        def headers = [
            Cwd: System.getProperty("java.io.tmpdir"),
            Auth: authtoken,
        ]
        if (args) headers.Arg = args.collect { String arg -> arg.bytes.encodeBase64() }
        if (environments) headers.Env = environments
        out << ClientProtocols.formatAsHeader(headers)
        return this // for method-chain
    }

    GroovyClient input(String text) {
        checkActive()
        text += SERVER_SIDE_SEPARATOR
        out << """\
                |Size: ${text.size()}
                |
                |${text}""".stripMargin()
        return this // for method-chain
    }

    String getOutText() {
        return new String(outBytes.flatten() as byte[])
    }

    String getErrText() {
        return new String(errBytes.flatten() as byte[])
    }

    int getExitStatus() {
        exitStatus
    }

    GroovyClient interrupt() {
        checkActive()
        out << ClientProtocols.formatAsHeader(
            Auth: authtoken,
            Cmd: "interrupt",
        )
        return this // for method-chain
    }

    GroovyClient shutdown() {
        checkActive()
        out << ClientProtocols.formatAsHeader(
            Auth: authtoken,
            Cmd: "shutdown",
        )
        return this // for method-chain
    }

    GroovyClient ping() {
        checkActive()
        out << ClientProtocols.formatAsHeader(
            Auth: authtoken,
            Cmd: "ping",
        )
        return this // for method-chain
    }

    GroovyClient waitForResponse() {
        while (exitStatus == null && outBytes.empty && errBytes.empty) {
            sleep 200 // wait for server operation
            readAllAsPossible()
        }
        return this // for method-chain
    }

    GroovyClient waitForExit() {
        while (exitStatus == null) {
            sleep 200 // wait for server operation
            readAllAsPossible()
        }
        return this // for method-chain
    }

    boolean isServerAvailable() {
        try {
            if (!connected) connect()
            ping()
            waitForExit()
        }
        catch (ConnectException e) {
            LogUtils.debugLog "Caught exception when health-checking", e
            return false
        }
        catch (Exception e) {
            LogUtils.errorLog "Caught unexpected exception when health-checking", e
            return false
        }
        finally {
            disconnect()
        }
        if (exitStatus != ExitStatus.SUCCESS.code) {
            LogUtils.errorLog "Exit status for ping seems invalid: $exitStatus"
        }
        return true
    }

    // NOTE: isServerAvailable() isn't negative isServerShutdown()
    // Because a complete shutdown status must be port closed.
    // On the other hand, that a server is available means that a server can handle a request rightly.
    // That a server cannot handle a request rightly means that a server isn't available even if a port is opened.
    boolean isServerShutdown() {
        return !canConnect()
    }

    boolean canConnect() {
        try {
            new Socket(host, port).close()
        }
        catch (ConnectException e) {
            return false
        }
        catch (Exception e) {
            LogUtils.errorLog "Caught unexpected exception when health-checking", e
            return false
        }
        return true
    }

    GroovyClient clearBuffer() {
        outBytes.clear()
        errBytes.clear()
        exitStatus = null

        return this // for method-chain
    }

    GroovyClient connect() {
        checkInactive()
        if (!WorkFiles.AUTHTOKEN_FILE.exists()) {
            throw new IllegalStateException("No authtoken file")
        }
        authtoken = WorkFiles.AUTHTOKEN_FILE.text
        socket = new Socket(host, port)
        ins = socket.inputStream
        out = socket.outputStream

        return this // for method-chain
    }

    GroovyClient disconnect() {
        if (connected) {
            socket.close()
        }
        socket = null
        ins = null
        out = null

        return this // for method-chain
    }

    private void readAllAsPossible() {
        checkActive()

        String availableText = IOUtils.readAvailableText(ins)
        if (availableText.empty) return

        def availableInputStream = new ByteArrayInputStream(availableText.bytes)
        while (true) {
            def headers = ClientProtocols.parseHeaders(availableInputStream)
            if (!headers) break

            if (headers.Channel?.getAt(0) ==~ /out|err/) {
                def buff = (headers.Channel?.get(0) == "out") ? outBytes : errBytes
                int size = headers.Size?.getAt(0) as int
                buff.addAll ClientProtocols.readBody(availableInputStream, size).toList()
            } else if (headers.Status) {
                exitStatus = headers.Status?.getAt(0) as int
            }
        }

        if (exitStatus) disconnect()
    }

    @Override
    protected void finalize() throws Throwable {
        disconnect()
        super.finalize()
    }

    private void checkActive() {
        if (!connected) throw new IllegalStateException("Connection is disabled")
    }

    private void checkInactive() {
        if (connected) throw new IllegalStateException("Already connected to server")
    }

    private boolean isConnected() {
        socket != null && !socket.closed
    }
}
