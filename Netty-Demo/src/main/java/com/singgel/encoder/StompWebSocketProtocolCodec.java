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
package com.singgel.encoder;


import com.singgel.handler.DealerPublisherHandler;
import com.singgel.handler.StompWebSocketClientPageHandler;
import com.singgel.server.StompVersion;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.handler.codec.stomp.StompSubframe;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@Sharable
public class StompWebSocketProtocolCodec extends MessageToMessageCodec<WebSocketFrame, StompSubframe> {


    private final StompWebSocketFrameEncoder stompWebSocketFrameEncoder = new StompWebSocketFrameEncoder();
    //    @Resource
//    private  StompChatHandler stompChatHandler;
    @Resource
    private DealerPublisherHandler dealerPublisherHandler ;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HandshakeComplete) {
            StompVersion stompVersion = StompVersion.findBySubProtocol(((HandshakeComplete) evt).selectedSubprotocol());
            ctx.channel().attr(StompVersion.CHANNEL_ATTRIBUTE_KEY).set(stompVersion);
            ctx.pipeline()
                    .addLast(new WebSocketFrameAggregator(65536))
                    .addLast(new StompSubframeDecoder())
                    .addLast(new StompSubframeAggregator(65536))
                    .addLast(dealerPublisherHandler)
                    .remove(StompWebSocketClientPageHandler.INSTANCE);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, StompSubframe stompFrame, List<Object> out) throws Exception {
        stompWebSocketFrameEncoder.encode(ctx, stompFrame, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame, List<Object> out) {
        System.out.println("---------------------->进行解码");
        if (webSocketFrame instanceof TextWebSocketFrame || webSocketFrame instanceof BinaryWebSocketFrame) {
            String t  =  ((TextWebSocketFrame)webSocketFrame).text().trim();
            //前端 心跳发送 空消息 后端返回
            if(t.equals("")){
                ctx.writeAndFlush(new TextWebSocketFrame(webSocketFrame.content().retain()));
                return;
            }
            out.add(webSocketFrame.content().retain());
            return;
        }
        if (webSocketFrame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(webSocketFrame.content().retain()));
            return;
        }
        if (webSocketFrame instanceof CloseWebSocketFrame) {
            ctx.writeAndFlush(webSocketFrame.retainedDuplicate()).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        if (webSocketFrame instanceof PongWebSocketFrame) {
            return;
        }

        ctx.close();
    }
}
