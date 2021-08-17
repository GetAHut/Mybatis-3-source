package com.wikr.proxys;

import com.wikr.invocations.ProxyInvocations;

import java.lang.reflect.Proxy;

/**
 * @Author: xyt
 * @Description:
 * @Date: 2021/8/17 16:11
 */
public class ProxyTest {

    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();

        UserService proxy = (UserService)Proxy.newProxyInstance(userService.getClass().getClassLoader(),
                userService.getClass().getInterfaces(),
                new ProxyInvocations(userService));
        proxy.getUserName("user");
    }
}
