package com.wikr.invocations;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Author: xyt
 * @Description: 动态代理处理器
 * @Date: 2021/8/17 16:02
 */
public class ProxyInvocations implements InvocationHandler {

    //代理目标类
    private Object target;

    public ProxyInvocations(Object target){
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //代理 增强逻辑
        System.out.println("-----------正在代理-----------");
        return method.invoke(target, args);
    }
}
