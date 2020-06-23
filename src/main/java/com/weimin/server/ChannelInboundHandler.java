package com.weimin.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * @author weimin
 * @ClassName ChannelInboundHandler
 * @Description TODO
 * @date 2020/6/23 15:15
 */
@Component
@ChannelHandler.Sharable
public class ChannelInboundHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIp = socketAddress.getAddress().getHostAddress();
        String clientPort = String.valueOf(socketAddress.getPort());
        System.out.println("新的连接：" + clientIp + ":" + clientPort);
    }
}
