package domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class description
 *  登录用户
 *
 * @version        1.0, 18/09/07
 * @author         pitt
 */
public class User implements Serializable {

    /** 用户名 */
    private String name;

    /**
     * Constructs
     *
     */
    public User() {}

    /**
     * Constructs
     *
     *
     * @param name
     */
    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        User user = (User) o;

        return Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                '}';
    }
}


