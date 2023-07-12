package com.singgel;

import com.singgel.server.StompWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Resource;

@SpringBootApplication
@Slf4j
public class NettyDemoApplication  implements ApplicationContextAware, CommandLineRunner {

	private static ApplicationContext applicationContext;
	private static DefaultListableBeanFactory defaultListableBeanFactory;

	@Resource
	private StompWebSocketServer stompWebSocketServer;

	public static void main(String[] args) throws Exception {
		SpringApplication springApplication = new SpringApplication(NettyDemoApplication.class);
//        springApplication.setAllowCircularReferences(Boolean.TRUE);
		springApplication.run(args);
	}
	/**
	 * 获取容器
	 *
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public static <T> T getBean(Class<T> clazz) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
		String className = clazz.getName();
		defaultListableBeanFactory.registerBeanDefinition(className, beanDefinitionBuilder.getBeanDefinition());
		return (T) applicationContext.getBean(className);
	}

	/**
	 * 释放容器
	 *
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public static <T> void destroyBean(Class<T> clazz) {
		String className = clazz.getName();
		defaultListableBeanFactory.removeBeanDefinition(className);
		System.out.println("destroy " + className);
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		defaultListableBeanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
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
