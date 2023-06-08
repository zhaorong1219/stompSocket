package com.singgel.config;

import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zr
 * @date 2023/6/6 10:07
 **/
public class ThreadPoolConfiguration {
    @Bean(name = "defaultThreadPoolExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor systemCheckPoolExecutorService() {

        return new ThreadPoolExecutor(3, 10, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(10000),
                Executors.defaultThreadFactory());
    }
}
