package com.wikr.proxys;

/**
 * @Author: xyt
 * @Description:
 * @Date: 2021/8/17 16:01
 */
public class UserServiceImpl implements UserService{


    @Override
    public String getUserName(String s) {
        System.out.println("proxy.print-> userName");
        return null;
    }
}
