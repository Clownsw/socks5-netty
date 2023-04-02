package com.geccocrawler.socks5.auth;

/**
 * @author smilex
 * @date 2023/4/2/9:37
 */
public interface PasswordAuth {

    boolean auth(String user, String password);

}
