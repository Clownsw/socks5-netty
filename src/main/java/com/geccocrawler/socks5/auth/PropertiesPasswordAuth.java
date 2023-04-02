package com.geccocrawler.socks5.auth;

import io.micrometer.common.util.StringUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * @author smilex
 * @date 2023/4/2/9:37
 */
public class PropertiesPasswordAuth implements PasswordAuth {

    private static final Properties PROPERTIES;

    static {
        PROPERTIES = new Properties();
        try {
            PROPERTIES.load(PropertiesPasswordAuth.class.getResourceAsStream("/password.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean auth(String user, String password) {
        final String configPassword = PROPERTIES.getProperty(user);

        if (StringUtils.isEmpty(configPassword)) {
            return true;
        }

        if (StringUtils.isEmpty(password)) {
            return false;
        }

        return password.equals(configPassword);
    }

}
