package com.geccocrawler.socks5.log;

import com.geccocrawler.socks5.handler.ProxyChannelTrafficShapingHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.Enumeration;

/**
 * @author smilex
 * @date 2023/4/2/9:38
 */
@Slf4j
public class ProxyFlowLog4j implements ProxyFlowLog {

    public void log(ChannelHandlerContext ctx) {
        ProxyChannelTrafficShapingHandler trafficShapingHandler = ProxyChannelTrafficShapingHandler.get(ctx);
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        long readByte = trafficShapingHandler.trafficCounter().cumulativeReadBytes();
        long writeByte = trafficShapingHandler.trafficCounter().cumulativeWrittenBytes();

        if (log.isInfoEnabled()) {
            log.info("{},{},{},{}:{},{}:{},{},{},{}",
                    trafficShapingHandler.getUsername(),
                    trafficShapingHandler.getBeginTime(),
                    trafficShapingHandler.getEndTime(),
                    getLocalAddress(),
                    localAddress.getPort(),
                    remoteAddress.getAddress().getHostAddress(),
                    remoteAddress.getPort(),
                    readByte,
                    writeByte,
                    (readByte + writeByte));
        }
    }

    /**
     * 获取本机的IP
     *
     * @return Ip地址
     */
    private static String getLocalAddress() {
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                if (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error when getting host ip address: <{}>.", e.getMessage());
            }
        }
        return "127.0.0.1";
    }

}
