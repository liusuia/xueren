package com.xueren.netty;

import lombok.Data;

@Data
public class WsPacket {

    private String type;
    private Object data;
}
