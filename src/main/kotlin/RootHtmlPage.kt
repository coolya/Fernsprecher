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

    private fun sendData(content: ByteArray, name: String, request: FullHttpRequest, channel: Channel, extraHeaders: HttpHeaders): Boolean {
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, FileResponses.getContentType(name))
        response.addCommonHeaders()
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, must-revalidate") //NON-NLS
        response.headers().set(HttpHeaderNames.LAST_MODIFIED, Date(Calendar.getInstance().timeInMillis))
        response.headers().add(extraHeaders)

        val keepAlive = response.addKeepAliveIfNeeded(request)
        if (request.method() != HttpMethod.HEAD) {
            HttpUtil.setContentLength(response, content.size.toLong())
        }

        channel.write(response)

        if (request.method() != HttpMethod.HEAD) {
            val stream = ByteArrayInputStream(content)
            channel.write(ChunkedStream(stream))
            stream.close()
        }

        val future = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE)
        }
        return true
    }
}