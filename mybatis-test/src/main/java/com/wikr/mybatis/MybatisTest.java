package com.wikr.mybatis;

import com.wikr.User;
import com.wikr.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * @className: MybatisTest
 * @Description:
 * @Author: abby
 * @Date: 2021/7/20 13:43
 */
public class MybatisTest {

    public static void main(String[] args) {

        String resource = "mybatis-config.xml";

        Reader reader;

        try {
            //将resource读入configuration配置类中
            reader = Resources.getResourceAsReader(resource);
            //构建 SqlSessionFactory
            SqlSessionFactory sqlMapper = new SqlSessionFactoryBuilder().build(reader);
            //数据源 执行器 sql执行
            SqlSession sqlSession = sqlMapper.openSession();

            //执行查询 底层执行jdbc
//            User user = sqlSession.selectOne("com.wikr.mapper.UserMapper.selectById", 1);
//            System.out.println(user.toString()); 

            try {
                //获取mapper映射文件
                UserMapper mapper = sqlSession.getMapper(UserMapper.class);
                System.out.println(mapper.getClass());
                //执行查询
                User user = mapper.selectById(1);
                System.out.println(user.getUsername());
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
