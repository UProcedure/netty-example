package com.weimin.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;

/**
 * @author weimin
 * @ClassName ConnectionHandler
 * @Description TODO
 * @date 2020/6/23 14:56
 */
@Component
@Slf4j
@ChannelHandler.Sharable
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handShaker;


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object textWebSocketFrame) {
        try {
            if(textWebSocketFrame instanceof ByteBuf){
                ByteBuf byteBuf = (ByteBuf)textWebSocketFrame;
                System.out.println("byteBuf");
                System.out.println(byteBuf.toString(CharsetUtil.UTF_8).trim());
            }
            if (textWebSocketFrame instanceof FullHttpRequest) {
                System.out.println("http消息");
                handleHttpRequest(channelHandlerContext, (FullHttpRequest) textWebSocketFrame);
            }
            if (textWebSocketFrame instanceof WebSocketFrame) {
                System.out.println("webSocket消息");
                if (textWebSocketFrame instanceof CloseWebSocketFrame) {
                    CloseWebSocketFrame close = ((CloseWebSocketFrame) textWebSocketFrame).retain();
                    System.out.println("关闭1");
                    handShaker.close(channelHandlerContext.channel(), close);
                    return;
                }
                if (textWebSocketFrame instanceof BinaryWebSocketFrame) {
                    ByteBuf content = ((BinaryWebSocketFrame) textWebSocketFrame).content();
                    byte[] reqContent = new byte[content.readableBytes()];
                    content.readBytes(reqContent);
                    FileOutputStream out = new FileOutputStream(System.currentTimeMillis() + ".jpg", true);
                    out.write(reqContent);
                    out.close();
                    ByteBuf byteBuf = Unpooled.copiedBuffer(reqContent);
                    channelHandlerContext.channel().writeAndFlush(new BinaryWebSocketFrame(byteBuf));
                }
                if (textWebSocketFrame instanceof TextWebSocketFrame) {
                    TextWebSocketFrame frame = (TextWebSocketFrame) textWebSocketFrame;
                    System.out.println(frame.text());
                }
            }
        } catch (IllegalArgumentException e) {
            log.error("Netty 消息转换异常");
        } catch (Exception e) {
            log.error("Netty 连接异常[{}]", e.getMessage());
        }
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("连接");
        super.channelActive(ctx);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        //如果http解码失败 则返回http异常 并且判断消息头有没有包含Upgrade字段(协议升级)
        if (!request.decoderResult().isSuccess()
                || (!"websocket".equals(request.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        //构造握手响应返回
        WebSocketServerHandshakerFactory ws = new WebSocketServerHandshakerFactory("", null, false);
        handShaker = ws.newHandshaker(request);
        if (handShaker == null) {
            //版本不支持
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handShaker.handshake(ctx.channel(), request);
        }
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpRequest request, FullHttpResponse response) {
        //返回给客户端
        if (response.status().code() != HttpResponseStatus.OK.code()) {
            ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
        }
        //如果不是keepalive那么就关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(response);
        if (response.status().code() != HttpResponseStatus.OK.code()) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("关闭2");
        super.channelInactive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("flush");
        //ctx.flush();
        ctx.fireChannelReadComplete();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("异常处理");
        cause.printStackTrace();
        ctx.close();
    }
}
