package com.geccocrawler.socks5.handler.ss5;

import com.geccocrawler.socks5.ProxyServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author smilex
 * @date 2023/4/2/9:36
 */
@Slf4j
public class Socks5InitialRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {

    private final ProxyServer proxyServer;

    public Socks5InitialRequestHandler(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) {
        if (log.isDebugEnabled()) {
            log.debug("初始化ss5连接 {}", msg);
        }

        if (msg.decoderResult().isFailure()) {
            if (log.isDebugEnabled()) {
                log.debug("不是ss5协议");
            }

            ctx.fireChannelRead(msg);
        } else {
            if (msg.version().equals(SocksVersion.SOCKS5)) {
                if (proxyServer.isAuth()) {
                    Socks5InitialResponse initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD);
                    ctx.writeAndFlush(initialResponse);
                } else {
                    Socks5InitialResponse initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
                    ctx.writeAndFlush(initialResponse);
                }
            }
        }
    }

}
