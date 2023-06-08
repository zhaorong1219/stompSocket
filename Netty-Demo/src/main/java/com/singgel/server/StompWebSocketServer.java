/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.singgel.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.UnknownHostException;

@Component
@Slf4j
public class StompWebSocketServer {
    @Value("${websocket.server.port}")
    private int port;

    @Resource
    private StompWebSocketServerInitializer stompWebSocketServerInitializer;

    public void start() throws Exception {
        NioEventLoopGroup boosGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(boosGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(stompWebSocketServerInitializer);

            bootstrap.bind(port).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws UnknownHostException {
                    if (future.isSuccess()) {
                        log.info("启动WebSocket服务器:" + port);
                    } else {
                        log.error("WebSocket服务器启动失败 " + future.cause());
                    }
                }
            }).channel().closeFuture().sync();
        } finally {
            boosGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

/*    public static void main(String[] args) throws Exception {
        new StompWebSocketChatServer().start(PORT);
    }*/
}
