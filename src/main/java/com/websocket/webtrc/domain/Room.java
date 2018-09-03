package com.websocket.webtrc.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 *  聊天房间
 *
 * @version        1.0, 18/09/03
 * @author         pitt
* */
public class Room {
    private  String id;
    private String name;
    /** 房间的用户列表 */
    private List<User> users = new ArrayList<>();


    public Room() {
        this.id = UUID.randomUUID().toString();
        this.name = "房间"+this.id.substring(0,3);
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

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "Room{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", users=" + users +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        Room room = (Room) o;
        return Objects.equals(id, room.id) &&
                Objects.equals(name, room.name) &&
                Objects.equals(users, room.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, users);
    }
}

