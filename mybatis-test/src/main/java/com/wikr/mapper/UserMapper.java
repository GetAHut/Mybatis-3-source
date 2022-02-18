package com.wikr.mapper;

import com.wikr.User;

import java.util.List;

/**
 * @className: UserMapper
 * @Description:
 * @Author: abby
 * @Date: 2021/7/20 13:57
 */
public interface UserMapper {

    List<User> selectUserList();

    List<User> selectUserList1(Integer id);

    User selectById(Integer id);
}
