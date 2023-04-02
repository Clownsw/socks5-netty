package com.geccocrawler.socks5.handler.ss5;

import com.geccocrawler.socks5.auth.PasswordAuth;
import com.geccocrawler.socks5.handler.ProxyChannelTrafficShapingHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * @author smilex
 * @date 2023/4/2/9:36
 */
@Slf4j
public class Socks5PasswordAuthRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {
    private final PasswordAuth passwordAuth;

    public Socks5PasswordAuthRequestHandler(PasswordAuth passwordAuth) {
        this.passwordAuth = passwordAuth;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) {
        if (log.isDebugEnabled()) {
            log.debug("用户名密码 {}, {} ", msg.username(), msg.password());
        }

        if (passwordAuth.auth(msg.username(), msg.password())) {
            if (log.isDebugEnabled()) {
                log.debug("认证成功 {}", msg.username());
            }
            ProxyChannelTrafficShapingHandler.username(ctx, msg.username());
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
            ctx.writeAndFlush(passwordAuthResponse);
        } else {
            if (log.isErrorEnabled()) {
                log.error("认证失败 {}", msg.username());
            }
            ProxyChannelTrafficShapingHandler.username(ctx, "unauthorized");
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            //发送鉴权失败消息，完成后关闭channel
            ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE);
        }
    }

}
