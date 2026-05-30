package com.xueren.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "xueren.netty")
public class NettyProperties {

    private int port = 8081;
    private String path = "/ws";
}
