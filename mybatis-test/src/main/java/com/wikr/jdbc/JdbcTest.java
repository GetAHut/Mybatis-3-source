package com.wikr.jdbc;

import com.wikr.User;

import java.sql.*;

/**
 * @className: JdbcTest
 * @Description:
 * @Author: abby
 * @Date: 2021/7/20 13:28
 */
public class JdbcTest {

    public static void main(String[] args) {

        Connection connection = null;
        PreparedStatement statement = null;

        try {
            //1.加载啊驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            //2.创建连接
            connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/mybatis", "root", "123456");
            //sql语句
            String  sql = "select id, username, sex, phone from user where id = ?";
            //获得sql执行者
            statement = connection.prepareStatement(sql);
            statement.setInt(1, 1);
            //执行查询
            statement.execute();
            ResultSet rs = statement.getResultSet();
            rs.next();

            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setSex(rs.getInt("sex"));
            user.setPhone(rs.getString("phone"));

            System.out.println(user.toString());


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭资源
            try {
                if (connection != null){
                    connection.close();
                }
                if (statement != null){
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

    }
}
