package com.websocket.webtrc.domain;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Objects;
import java.util.UUID;

/**
 * Class description
 * 聊天对象
 *
 * @author pitt
 * @version 1.0, 18/09/03
 */
public class User {


    private String name;
    @JSONField(serialize = false)
    private String ip;

    public User() {
    }

    public User(String name) {
        this.name = name;

    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * TODO 暂时根据name来判断在线用户列表是否重复
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

