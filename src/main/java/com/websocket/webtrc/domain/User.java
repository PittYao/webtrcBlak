package com.websocket.webtrc.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Class description
 *  聊天对象
 *
 * @version        1.0, 18/09/03
 * @author         pitt
 */
public class User {

    private String id;

    private String name;

    private String ip;

    public User() {}

    /**
     * Constructs
     * @param ip
     */
    public User( String ip) {
        this.id   = UUID.randomUUID().toString();
        this.name = ip;
    }

    @Override
    public String toString() {
        return "User{" + "id='" + id + '\'' + ", name='" + name + '\'' + '}';
    }


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}

