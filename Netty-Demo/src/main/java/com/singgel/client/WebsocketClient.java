package com.singgel.client;

import com.singgel.service.WebsocketServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/websocket")
public class WebsocketClient {

    @Resource
    private WebsocketServiceImpl websocketService;

    @PostMapping("/sendMessage")
    public void sendMessage(String message){
        websocketService.sendMessage(message);
    }
}
