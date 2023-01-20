package ws.logv.fernsprecher

import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.stream.ChunkedStream
import org.apache.commons.compress.utils.IOUtils
import org.jetbrains.ide.HttpRequestHandler
import org.jetbrains.io.FileResponses
import org.jetbrains.io.addCommonHeaders
import org.jetbrains.io.addKeepAliveIfNeeded
import org.jetbrains.io.send
import java.io.ByteArrayInputStream
import java.util.*

class RootPageHandler : HttpRequestHandler() {
    override fun isSupported(request: FullHttpRequest): Boolean {
        return request.method() == HttpMethod.GET && (request.uri().startsWith("/fernsprecher/"))
    }

    override fun process(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): Boolean {
        val resourceName = when {
            urlDecoder.path().endsWith("/fernsprecher/") -> {
                "index.html"
            }
            urlDecoder.path()
                .endsWith("/fernsprecher/script.js") -> {
                "script.js"
            }
            else -> {
                null
            }
        } ?: return false

        val data = IOUtils.toByteArray(RootPageHandler::class.java.getResourceAsStream(resourceName))
        return sendData(data, resourceName, request, context.channel(), request.headers())
    }
}