package br.com.projetos.cameraremote

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import org.json.JSONObject
import kotlin.concurrent.thread

class LocalCommandServer(
    private val port: Int,
    private val onCommand: (String, Int) -> Unit,
) {
    @Volatile
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var running = false
    private var generation = 0

    @Synchronized
    fun start() {
        if (running) return
        running = true
        val currentGeneration = ++generation
        thread(name = "camera-command-server", isDaemon = true) {
            var createdSocket: ServerSocket? = null
            try {
                val socket = ServerSocket().also {
                    it.reuseAddress = true
                    it.bind(InetSocketAddress("127.0.0.1", port), 8)
                }
                createdSocket = socket
                synchronized(this) {
                    if (!running || generation != currentGeneration) {
                        socket.close()
                        return@thread
                    }
                    serverSocket = socket
                }
                while (running && generation == currentGeneration) {
                    val client = try {
                        socket.accept()
                    } catch (exception: SocketException) {
                        if (running) throw exception
                        break
                    }
                    try {
                        handle(client)
                    } catch (_: Exception) {
                        // Uma requisição malformada não deve derrubar o servidor.
                    }
                }
            } catch (_: Exception) {
                // O socket é local e vive apenas enquanto a Activity está em primeiro plano.
            } finally {
                createdSocket?.close()
                synchronized(this) {
                    if (serverSocket === createdSocket) serverSocket = null
                }
            }
        }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 5_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
            val requestLine = reader.readLine()
            if (requestLine == null) {
                writeResponse(client, 400, "Bad Request")
                return
            }

            val requestParts = requestLine.split(' ', limit = 3)
            val method = requestParts.getOrNull(0)
            val path = requestParts.getOrNull(1)?.substringBefore('?')
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: run {
                    writeResponse(client, 400, "Bad Request")
                    return
                }
                if (line.isEmpty()) break
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(':').trim().toIntOrNull() ?: -1
                }
            }

            if (contentLength < 0 || contentLength > 8_192) {
                writeResponse(client, 400, "Invalid Content-Length")
                return
            }
            val body = CharArray(contentLength)
            var offset = 0
            while (offset < body.size) {
                val read = reader.read(body, offset, body.size - offset)
                if (read < 0) {
                    writeResponse(client, 400, "Incomplete Request")
                    return
                }
                offset += read
            }

            val bodyText = String(body)
            val payload = runCatching { JSONObject(bodyText) }.getOrNull()
            val commandName = payload?.optString("name").orEmpty()
            val delaySeconds = payload?.optInt("delaySeconds", 0)?.coerceIn(0, 30) ?: 0
            if (method == "POST" && path == "/command" &&
                (commandName == "TAKE_PHOTO" || bodyText.contains("TAKE_PHOTO"))
            ) {
                onCommand("TAKE_PHOTO", delaySeconds)
                writeResponse(client, 200, "OK")
            } else if (path != "/command") {
                writeResponse(client, 404, "Not Found")
            } else {
                writeResponse(client, 400, "Unsupported Command")
            }
        }
    }

    private fun writeResponse(client: Socket, statusCode: Int, body: String) {
        val reason = when (statusCode) {
            200 -> "OK"
            404 -> "Not Found"
            else -> "Bad Request"
        }
        val bodyBytes = body.toByteArray(StandardCharsets.UTF_8)
        val response = "HTTP/1.1 $statusCode $reason\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n" +
            "Content-Length: ${bodyBytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        client.getOutputStream().use { output ->
            output.write(response.toByteArray(StandardCharsets.UTF_8))
            output.write(bodyBytes)
            output.flush()
        }
    }

    @Synchronized
    fun stop() {
        running = false
        generation++
        serverSocket?.close()
        serverSocket = null
    }
}
