package com.singgel.service;

import com.singgel.handler.DealerPublisherHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 服务
 *
 * @version 1.0
 * @Author : zr
 * @date 2023/6/6 15:39
 */
@Slf4j
@Service
public class WebsocketServiceImpl {

    @Autowired
    private DealerPublisherHandler dealerPublisherHandler;

    public void sendMessage(String message) {
        dealerPublisherHandler.send(message, "casino-websocket");
    }
}
