package com.weimin.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author weimin
 * @ClassName NettySocketServer
 * @Description TODO
 * @date 2020/6/23 14:53
 */
@Component
@Slf4j
public class NettySocketServer implements CommandLineRunner, ApplicationContextAware {

    private EventLoopGroup bossGroup = new NioEventLoopGroup();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private ServerBootstrap serverBootstrap = new ServerBootstrap();
    private ChannelFuture future;
    private ChannelInboundHandler channelInboundHandler;
    private WebSocketHandler webSocketHandler;

    @Value("${netty.port}")
    private Integer port;

    @Autowired
    public NettySocketServer(ChannelInboundHandler channelInboundHandler, WebSocketHandler webSocketHandler) {
        this.channelInboundHandler = channelInboundHandler;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void run(String... args) throws Exception {
        nettyStart();
    }

    private void nettyStart(){
        try {
            ServerBootstrap group = serverBootstrap.group(bossGroup, workerGroup);
            group.channel(NioServerSocketChannel.class);
            group.childOption(ChannelOption.SO_REUSEADDR, true);
            group.childHandler(new ChannelInitializer<SocketChannel>(){
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("initConnectionHandler",channelInboundHandler);
                    pipeline.addLast("socketDecoder",new SocketDecoder());
                    pipeline.addLast("socketHandler",webSocketHandler);
                }
            });
            future = serverBootstrap.bind(port).sync();
            if(future.isSuccess()){
                log.info("Netty 服务已启动,启动端口 {}",port);
            }
        }catch (Exception e){
            e.printStackTrace();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    @PreDestroy
    public void nettyStop() {
        if (future != null) {
            future.channel().close().addListener(ChannelFutureListener.CLOSE);
            future.awaitUninterruptibly();
            future = null;
            log.info("Netty 服务端关闭");
        }
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }


    void websocketAdd(ChannelHandlerContext ctx){
        ctx.pipeline().addBefore("socketHandler","http-codec",new HttpServerCodec());
        ctx.pipeline().addBefore("socketHandler","aggregator",new HttpObjectAggregator(65535));
        ctx.pipeline().addBefore("socketHandler","http-chunked",new ChunkedWriteHandler());
        ctx.pipeline().addBefore("socketHandler","WebSocketAggregator",new WebSocketFrameAggregator(65535));
    }

    static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        NettySocketServer.applicationContext = applicationContext;
    }
}
