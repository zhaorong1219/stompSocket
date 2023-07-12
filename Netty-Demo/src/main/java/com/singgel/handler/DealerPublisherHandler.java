package com.singgel.handler;

import com.singgel.server.StompSubscription;
import com.singgel.server.StompVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.netty.handler.codec.stomp.StompHeaders.*;

/**
 * @author zr
 * @date 2023/01/13 11:22
 **/
@Slf4j
@Component
@ChannelHandler.Sharable
public class DealerPublisherHandler extends SimpleChannelInboundHandler<StompFrame> {


    private final ConcurrentMap<String, Set<StompSubscription>> dealerDestinations =
            new ConcurrentHashMap<String, Set<StompSubscription>>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, StompFrame inboundFrame) throws Exception {
        DecoderResult decoderResult = inboundFrame.decoderResult();
        if (decoderResult.isFailure()) {
            sendErrorFrame("rejected frame", decoderResult.toString(), ctx);
            return;
        }

        switch (inboundFrame.command()) {
            case STOMP:
            case CONNECT:
                onConnect(ctx, inboundFrame);
                break;
            case SUBSCRIBE:
                onSubscribe(ctx, inboundFrame);
                break;
            case SEND:
                onSend(ctx, inboundFrame);
                break;
            case UNSUBSCRIBE:
                onUnsubscribe(ctx, inboundFrame);
                break;
            case DISCONNECT:
                onDisconnect(ctx, inboundFrame);
                break;
            default:
                sendErrorFrame("unsupported command",
                        "Received unsupported command " + inboundFrame.command(), ctx);
        }
    }

    private void onSend(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String destination = inboundFrame.headers().getAsString(DESTINATION);

        System.out.println("1.1.1 inboundFrame ---------------------------------> " + inboundFrame + " destination " + destination);

        if (destination == null) {
            sendErrorFrame("missed header", "required 'destination' header missed", ctx);
            return;
        }

        Set<StompSubscription> subscriptions = dealerDestinations.get(destination);

        System.out.println("1.1.2 subscriptions -------------------------------> " + subscriptions.toString());

        for (StompSubscription subscription : subscriptions) {

            System.out.println("1.1.3 subscription -------------------------------> " + transformToMessage(inboundFrame, subscription));

            subscription.channel().writeAndFlush(transformToMessage(inboundFrame, subscription));

        }
    }

    private static void onConnect(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String acceptVersions = inboundFrame.headers().getAsString(ACCEPT_VERSION);
        StompVersion handshakeAcceptVersion = ctx.channel().attr(StompVersion.CHANNEL_ATTRIBUTE_KEY).get();
        if (acceptVersions == null || !acceptVersions.contains(handshakeAcceptVersion.version())) {
            sendErrorFrame("invalid version",
                    "Received invalid version, expected " + handshakeAcceptVersion.version(), ctx);
            return;
        }
        StompFrame connectedFrame = new DefaultStompFrame(StompCommand.CONNECTED,inboundFrame.content());
        connectedFrame.headers()
                .set(SERVER, "Websocket-Server")
                .set(VERSION, handshakeAcceptVersion.version())
                .set(HEART_BEAT, "5000,5000");
        String contentType= inboundFrame.headers().getAsString(CONTENT_TYPE);
        if(contentType!=null){
            connectedFrame.headers().set(CONTENT_TYPE, contentType);
        } else {
            connectedFrame.headers().set(CONTENT_TYPE, "text/html;charset=utf-8");
        }

        ctx.writeAndFlush(connectedFrame.retain());
    }

    private static void sendErrorFrame(String message, String description, ChannelHandlerContext ctx) {
        StompFrame errorFrame = new DefaultStompFrame(StompCommand.ERROR);
        errorFrame.headers().set(MESSAGE, message);

        if (description != null) {
            errorFrame.content().writeCharSequence(description, CharsetUtil.UTF_8);
        }

        ctx.writeAndFlush(errorFrame).addListener(ChannelFutureListener.CLOSE);
    }

    private void onSubscribe(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String destination = inboundFrame.headers().getAsString(DESTINATION);

        String subscriptionId = inboundFrame.headers().getAsString(ID);

        System.out.println("1.2.1 inboundFrame.content ---------------------------------> " + inboundFrame.content() + " destination " + destination + " and " + subscriptionId);
        if (destination == null || subscriptionId == null) {
            sendErrorFrame("missed header", "Required 'destination' or 'id' header missed", ctx);
            return;
        }

        Set<StompSubscription> subscriptions = dealerDestinations.get(destination);
        if (subscriptions == null) {
            subscriptions = new HashSet<StompSubscription>();
            Set<StompSubscription> previousSubscriptions = dealerDestinations.putIfAbsent(destination, subscriptions);
            if (previousSubscriptions != null) {
                subscriptions = previousSubscriptions;
            }
        }

        final StompSubscription subscription = new StompSubscription(subscriptionId, destination, ctx.channel());
        if (subscriptions.contains(subscription)) {
            sendErrorFrame("duplicate subscription",
                    "Received duplicate subscription id=" + subscriptionId, ctx);
            return;
        }

        subscriptions.add(subscription);
        ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                dealerDestinations.get(subscription.destination()).remove(subscription);
            }
        });

        String receiptId = inboundFrame.headers().getAsString(RECEIPT);
        if (receiptId != null) {
            //StompFrame receiptFrame = new TextStompFrame(StompCommand.RECEIPT);
            StompFrame receiptFrame = new DefaultStompFrame(StompCommand.RECEIPT);
            receiptFrame.headers().set(RECEIPT_ID, receiptId);

            System.out.println("1.2.2 receiptFrame ---------------------------------> " + receiptFrame);
            ctx.writeAndFlush(receiptFrame);

        }

//        Map<String,Object> fake=new HashMap<String, Object>();
//        fake.put("detectionVideoUrl","webrtc://srs.htsmartpay.com:443/live/baccarat01");
//        fake.put("dealCountdownTime",5);
      //  ctx.writeAndFlush(transformToMessage(JSONObject.toJSONString(fake),subscription));



    }

    public void send(String message, String routing) {

        String destination  = String.format("/topic/%s", routing);


        Set<StompSubscription> subscriptions = dealerDestinations.get(destination);

        if(subscriptions!=null && !subscriptions.isEmpty()) {
            for (StompSubscription subscription : subscriptions) {

                subscription.channel().writeAndFlush(transformToMessage(message, subscription));

            }
        }
    }

    private void onUnsubscribe(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String subscriptionId = inboundFrame.headers().getAsString(SUBSCRIPTION);
        for (Map.Entry<String, Set<StompSubscription>> entry : dealerDestinations.entrySet()) {
            Iterator<StompSubscription> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                StompSubscription subscription = iterator.next();
                if (subscription.id().equals(subscriptionId) && subscription.channel().equals(ctx.channel())) {
                    iterator.remove();
                    return;
                }
            }
        }
    }

    private static void onDisconnect(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String receiptId = inboundFrame.headers().getAsString(RECEIPT);
        if (receiptId == null) {
            ctx.close();
            return;
        }

        StompFrame receiptFrame = new DefaultStompFrame(StompCommand.RECEIPT);
        receiptFrame.headers().set(RECEIPT_ID, receiptId);
        ctx.writeAndFlush(receiptFrame).addListener(ChannelFutureListener.CLOSE);
    }


    private  static StompFrame transformToMessage(String message, StompSubscription subscription) {

        Charset charset = StandardCharsets.UTF_8;

        byte[] bytes = message.getBytes(charset);
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        StompFrame messageFrame = new DefaultStompFrame(StompCommand.MESSAGE, buf);
        String id = UUID.randomUUID().toString();
        messageFrame.headers()
                .set(MESSAGE_ID, id)
                .set(SUBSCRIPTION, subscription.id())
                .set(CONTENT_LENGTH, Integer.toString(messageFrame.content().readableBytes()));

       messageFrame.headers().set(CONTENT_TYPE, "application/json");


        return messageFrame;
    }

    private  static StompFrame transformToMessage(StompFrame sendFrame, StompSubscription subscription) {

        Charset charset = StandardCharsets.UTF_8;

        //System.out.println("1.3 textContent----------------------------->" + sendFrame.content().retainedDuplicate().toString(CharsetUtil.UTF_8));
        //StompFrame messageFrame = new TextStompEntity(StompCommand.MESSAGE,sendFrame.content().retainedDuplicate(), sendFrame.content().retainedDuplicate().toString());
        StompFrame messageFrame = new DefaultStompFrame(StompCommand.MESSAGE, sendFrame.content().retainedDuplicate());
        String id = UUID.randomUUID().toString();
        messageFrame.headers()
                .set(MESSAGE_ID, id)
                .set(SUBSCRIPTION, subscription.id())
                .set(CONTENT_LENGTH, Integer.toString(messageFrame.content().readableBytes()));

        CharSequence contentType = sendFrame.headers().get(CONTENT_TYPE);
        if (contentType != null) {
            messageFrame.headers().set(CONTENT_TYPE, contentType);
        }

        return messageFrame;
    }



}
