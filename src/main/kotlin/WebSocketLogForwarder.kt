package ws.logv.fernsprecher

import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.QueryStringDecoder
import io.netty.handler.codec.http.websocketx.*
import io.netty.util.ReferenceCountUtil
import org.jetbrains.ide.HttpRequestHandler
import org.jetbrains.ide.RestService
import org.jetbrains.io.BuiltInServer
import org.jetbrains.io.NettyUtil
import java.nio.channels.ClosedChannelException
import java.util.logging.Handler
import java.util.logging.LogRecord


@Suppress("UnstableApiUsage")
class LogAppender(
    private val reqHandler: WebSocketLogForwarder
) : Handler() {

    override fun publish(record: LogRecord?) {
        if(record != null) {
            reqHandler.logMsg(record)
        }
    }

    override fun flush() {
        //noop
    }

    override fun close() {
        //noop
    }
}

class WebSocketClient(private val channel: Channel) {
    fun send(message: BufferExposingByteArrayOutputStream) {
        if (channel.isOpen) {
            channel.writeAndFlush(TextWebSocketFrame(Unpooled.wrappedBuffer(message.internalBuffer, 0, message.size())))
        } else {
            channel.writeAndFlush(ClosedChannelException())
        }
    }
}

class WebSocketChannelAdapter(val messageReceived: (TextWebSocketFrame, Channel) -> Unit, val closed: () -> Unit) :
    ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        when (msg) {
            !is WebSocketFrame, is PongWebSocketFrame -> ReferenceCountUtil.release(msg)
            is PingWebSocketFrame -> ctx.channel().writeAndFlush(PongWebSocketFrame(msg.content()))
            is CloseWebSocketFrame -> {
                closed()
                ctx.channel().close()
            }

            is TextWebSocketFrame -> messageReceived(msg, ctx.channel())
            else -> throw UnsupportedOperationException("${msg.javaClass.name} type not supported")
        }
    }
}

class WebSocketLogForwarder : HttpRequestHandler() {

    init {
        val appender = LogAppender(this)
        java.util.logging.Logger.getLogger("").addHandler(appender)
    }

    private val connectedClients = mutableListOf<WebSocketClient>()

    fun logMsg(event: LogRecord) {
        val out = BufferExposingByteArrayOutputStream()
        val writer = RestService.createJsonWriter(out)

        writer.beginObject()
        writer.name("level").value(event.level.toString())
        writer.name("message").value(event.message)
        writer.name("time").value(event.millis)
        writer.name("logger").value(event.loggerName)
        if (event.thrown != null) {
            writer.name("stacktrace").beginArray()
            event.thrown.stackTrace.forEach {
                writer.value(it.toString())
            }
            writer.endArray()
        }
        writer.endObject()
        writer.close()
        connectedClients.forEach { it.send(out) }
    }

    override fun isSupported(request: FullHttpRequest): Boolean {
        return request.method() === HttpMethod.GET && "WebSocket".equals(
            request.headers().getAsString(HttpHeaderNames.UPGRADE), ignoreCase = true
        ) && request.uri().startsWith("/fernsprecher/logs")
    }

    private fun messageReceived(client: WebSocketClient, frame: TextWebSocketFrame, channel: Channel) {
        //todo handle log level requests by client
    }

    private fun disconnected(client: WebSocketClient) {
        connectedClients.remove(client)
    }

    override fun process(
        urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext
    ): Boolean {

        val factory = WebSocketServerHandshakerFactory(
            "ws://" + request.headers().getAsString(HttpHeaderNames.HOST) + urlDecoder.path(),
            null,
            false,
            NettyUtil.MAX_CONTENT_LENGTH
        )

        val handshaker = factory.newHandshaker(request)
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(context.channel())
            return true
        }

        if (!context.channel().isOpen) {
            return false
        }

        val client = WebSocketClient(context.channel())

        handshaker.handshake(context.channel(), request).addListener(ChannelFutureListener {
            if (it.isSuccess) {
                val adapter =
                    WebSocketChannelAdapter({ frame, channel -> this.messageReceived(client, frame, channel) },
                        { this.disconnected(client) })
                BuiltInServer.replaceDefaultHandler(context, adapter)
                val channelHandlerContext = context.pipeline().context(adapter)
                channelHandlerContext.pipeline().addBefore(
                    channelHandlerContext.name(),
                    "webSocketFrameAggregator",
                    WebSocketFrameAggregator(NettyUtil.MAX_CONTENT_LENGTH)
                )
                connectedClients.add(client)
            }
        })
        return true
    }
}
