package com.geccocrawler.socks5;

import com.geccocrawler.socks5.auth.PasswordAuth;
import com.geccocrawler.socks5.auth.PropertiesPasswordAuth;
import com.geccocrawler.socks5.handler.ChannelListener;
import com.geccocrawler.socks5.handler.ProxyChannelTrafficShapingHandler;
import com.geccocrawler.socks5.handler.ProxyIdleHandler;
import com.geccocrawler.socks5.handler.ss5.Socks5CommandRequestHandler;
import com.geccocrawler.socks5.handler.ss5.Socks5InitialRequestHandler;
import com.geccocrawler.socks5.handler.ss5.Socks5PasswordAuthRequestHandler;
import com.geccocrawler.socks5.log.ProxyFlowLog;
import com.geccocrawler.socks5.log.ProxyFlowLog4j;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author smilex
 * @date 2023/4/2/9:38
 */
@SuppressWarnings({"unused", "AlibabaAvoidManuallyCreateThread"})
@Slf4j
public class ProxyServer {
    private static final Thread MICROMETER_THREAD = new Thread(ProxyServer::startMicrometer, "micrometer_thread");
    private static final SimpleMeterRegistry SIMPLE_METER_REGISTRY = new SimpleMeterRegistry();

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();

    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    private final int port;

    private boolean auth;

    private boolean logging;

    private ProxyFlowLog proxyFlowLog;

    private ChannelListener channelListener;

    private PasswordAuth passwordAuth;

    private ProxyServer(int port) {
        this.port = port;
    }

    public static ProxyServer create(int port) {
        return new ProxyServer(port);
    }

    public ProxyServer auth(boolean auth) {
        this.auth = auth;
        return this;
    }

    public ProxyServer logging(boolean logging) {
        this.logging = logging;
        return this;
    }

    public ProxyServer proxyFlowLog(ProxyFlowLog proxyFlowLog) {
        this.proxyFlowLog = proxyFlowLog;
        return this;
    }

    public ProxyServer channelListener(ChannelListener channelListener) {
        this.channelListener = channelListener;
        return this;
    }

    public ProxyServer passwordAuth(PasswordAuth passwordAuth) {
        this.passwordAuth = passwordAuth;
        return this;
    }

    public ProxyFlowLog getProxyFlowLog() {
        return proxyFlowLog;
    }

    public ChannelListener getChannelListener() {
        return channelListener;
    }

    public PasswordAuth getPasswordAuth() {
        return passwordAuth;
    }

    public boolean isAuth() {
        return auth;
    }

    public boolean isLogging() {
        return logging;
    }

    public void start() throws Exception {
        if (proxyFlowLog == null) {
            proxyFlowLog = new ProxyFlowLog4j();
        }

        if (passwordAuth == null) {
            passwordAuth = new PropertiesPasswordAuth();
        }

        final EventLoopGroup boss = new NioEventLoopGroup(1);
        final EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            //流量统计
                            ch.pipeline().addLast(
                                    ProxyChannelTrafficShapingHandler.PROXY_TRAFFIC,
                                    new ProxyChannelTrafficShapingHandler(3000, proxyFlowLog, channelListener)
                            );
                            //channel超时处理
                            ch.pipeline().addLast(new IdleStateHandler(3, 30, 0));
                            ch.pipeline().addLast(new ProxyIdleHandler());
                            //netty日志
                            if (logging) {
                                ch.pipeline().addLast(new LoggingHandler());
                            }
                            //Socks5MessagByteBuf
                            ch.pipeline().addLast(Socks5ServerEncoder.DEFAULT);
                            //sock5 init
                            ch.pipeline().addLast(new Socks5InitialRequestDecoder());
                            //sock5 init
                            ch.pipeline().addLast(new Socks5InitialRequestHandler(ProxyServer.this));
                            if (isAuth()) {
                                //socks auth
                                ch.pipeline().addLast(new Socks5PasswordAuthRequestDecoder());
                                //socks auth
                                ch.pipeline().addLast(new Socks5PasswordAuthRequestHandler(getPasswordAuth()));
                            }
                            //socks connection
                            ch.pipeline().addLast(new Socks5CommandRequestDecoder());
                            //Socks connection
                            ch.pipeline().addLast(new Socks5CommandRequestHandler(ProxyServer.this.getBossGroup()));
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            if (log.isDebugEnabled()) {
                log.debug("bind port {}", port);
            }
            future.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    private static void startMicrometer() {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/prometheus", handle -> {
                final String response = PrometheusMonitor.scrape();
                final byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                handle.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = handle.getResponseBody()) {
                    os.write(responseBytes);
                }
            });
            server.start();
        } catch (Exception e) {
            log.error("", e);
            System.exit(0);
        }
    }

    public static void main(String[] args) throws Exception {
        final String propertyPort = System.getProperty("port");
        final String propertyAuth = System.getProperty("auth");
        int port = 11080;
        boolean auth = false;

        try {
            port = Integer.parseInt(propertyPort);
        } catch (Exception ignore) {
            // ignore
        }

        try {
            auth = Boolean.parseBoolean(propertyAuth);
        } catch (Exception ignore) {
            // ignore
        }

        MICROMETER_THREAD.start();
        ProxyServer.create(port)
                .logging(true)
                .auth(auth)
                .start();
    }
}
