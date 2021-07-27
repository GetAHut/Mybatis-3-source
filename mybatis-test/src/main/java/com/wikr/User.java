package com.wikr;

import lombok.Data;

/**
 * @className: User
 * @Description:
 * @Author: abby
 * @Date: 2021/7/20 13:34
 */
@Data
public class User {

    private Long id;
    private String username;
    private Integer sex;
    private String phone;
}
