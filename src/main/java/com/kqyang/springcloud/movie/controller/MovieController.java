package com.kqyang.springcloud.movie.controller;

import com.kqyang.springcloud.movie.entity.User;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author kqyang
 */
@RestController
public class MovieController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovieController.class);

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    @Value("${user.userServiceUrl}")
    private String userServiceUrl;

    /**
     * 配置远程服务访问容错机制
     * 1. 访问超时时间为5秒
     * 2. 最大连接时间为10秒
     * 3. 配置容错回调方法
     * 4. 最大执行线程数量为1
     * 5. 最大线程队列等待为10
     *
     * @param id
     * @return
     */
    @GetMapping("/user/{id}")
    @HystrixCommand(fallbackMethod = "findByIdFallback", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000"),
            @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "10000")
    }, threadPoolProperties = {
            @HystrixProperty(name = "coreSize", value = "1"),
            @HystrixProperty(name = "maxQueueSize", value = "10")
    })
    public User findById(@PathVariable Long id) {
        return restTemplate.getForObject(userServiceUrl + id, User.class);
    }

    @GetMapping("/log-user-instance")
    public void logUserInstance() {
        ServiceInstance instance = loadBalancerClient.choose("springcloud-provider-user");
        LOGGER.info("{}:{}:{}", instance.getServiceId(), instance.getHost(), instance.getPort());
    }

    public User findByIdFallback(Long id) {
        User user = new User();
        user.setId(-1L);
        user.setName("默认用户");
        return user;
    }
}