package com.geccocrawler.socks5.log;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author smilex
 * @date 2023/4/2/9:37
 */
public interface ProxyFlowLog {

    void log(ChannelHandlerContext ctx);
}
