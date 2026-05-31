package com.xueren.netty;

import com.xueren.config.NettyProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class NettyServerStarter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NettyServerStarter.class);

    private final NettyProperties nettyProperties;
    private final WebSocketFrameHandler webSocketFrameHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyServerStarter(NettyProperties nettyProperties,
                              WebSocketFrameHandler webSocketFrameHandler) {
        this.nettyProperties = nettyProperties;
        this.webSocketFrameHandler = webSocketFrameHandler;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String wsPath = nettyProperties.getPath();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new WebSocketServerProtocolHandler(
                                WebSocketServerProtocolConfig.newBuilder()
                                        .websocketPath(wsPath)
                                        .checkStartsWith(true)
                                        .allowExtensions(false)
                                        .build()
                        ));
                        pipeline.addLast(webSocketFrameHandler);
                    }
                });
        ChannelFuture future = bootstrap.bind(nettyProperties.getPort()).addListener(f -> {
            if (f.isSuccess()) {
                log.info("Netty WebSocket started on port {}, path {}", nettyProperties.getPort(), wsPath);
            } else {
                log.error("Netty bind failed on port {}", nettyProperties.getPort(), f.cause());
            }
        }).sync();
        serverChannel = future.channel();
        log.info("Netty WebSocket started on port {}, path {}", nettyProperties.getPort(), wsPath);
    }

    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
