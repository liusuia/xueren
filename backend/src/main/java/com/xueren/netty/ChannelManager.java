package com.xueren.netty;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChannelManager {

    private final ConcurrentHashMap<Long, Channel> userChannels = new ConcurrentHashMap<>();

    public void bind(Long userId, Channel channel) {
        Channel old = userChannels.put(userId, channel);
        if (old != null && old != channel && old.isActive()) {
            old.close();
        }
    }

    public void unbind(Channel channel) {
        userChannels.entrySet().removeIf(entry -> entry.getValue() == channel);
    }

    public Channel getChannel(Long userId) {
        return userChannels.get(userId);
    }

    public boolean isOnline(Long userId) {
        Channel channel = userChannels.get(userId);
        return channel != null && channel.isActive();
    }
}
