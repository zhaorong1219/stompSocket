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

import com.singgel.encoder.StompWebSocketProtocolCodec;
import com.singgel.handler.NettyServerHandler;
import com.singgel.handler.StompWebSocketClientPageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.internal.ObjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StompWebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private final String publicPath;

    private final StompWebSocketProtocolCodec stompWebSocketProtocolCodec;

    public StompWebSocketServerInitializer(String publicPath) {
        this.publicPath = ObjectUtil.checkNotNull(publicPath, "chatPath");
        stompWebSocketProtocolCodec = new StompWebSocketProtocolCodec();
    }

    public StompWebSocketServerInitializer() {
        this("/ws/public");
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        channel.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(65536))
                 .addLast(StompWebSocketClientPageHandler.INSTANCE)
                .addLast(new WebSocketServerProtocolHandler(publicPath, StompVersion.SUB_PROTOCOLS))
                .addLast(new NettyServerHandler())
                .addLast(stompWebSocketProtocolCodec);
    }
}
