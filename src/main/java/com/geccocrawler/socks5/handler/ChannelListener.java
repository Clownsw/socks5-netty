package com.geccocrawler.socks5.handler;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author smilex
 * @date 2023/4/2/9:36
 */
public interface ChannelListener {

    void inActive(ChannelHandlerContext ctx);

    void active(ChannelHandlerContext ctx);
}
