package com.singgel;

import com.singgel.server.StompWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;

@SpringBootApplication
@Slf4j
public class NettyDemoApplication implements CommandLineRunner {

	@Resource
	private StompWebSocketServer stompWebSocketServer;

	public static void main(String[] args) {
			SpringApplication.run(NettyDemoApplication.class, args);
		}

	@Override
	public void run(String... args) throws Exception {
		try {
			stompWebSocketServer.start();
		} catch (Exception e) {
			log.error("服务启动异常：{}", e);
		}
	}

}
