package com.websocket.webtrc.test.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.websocket.webtrc.domain.Room;
import com.websocket.webtrc.domain.User;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试socket后端连接
 */
@ServerEndpoint(value = "/MyWebsocket/{userName}")
@Component
public class MyWebSocket implements Serializable {
    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     */
    private static CopyOnWriteArraySet<MyWebSocket> webSocketSet = new CopyOnWriteArraySet<MyWebSocket>();


    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 每个房间的用户列表
     */
    private Map<Room, List<User>> users = new HashMap<>();
    /**
     * 房间集合
     */
    private List<Room> rooms = new ArrayList<>();

    /**
     * 当前发消息的人员编号
     */
    private String currentUser;

    /**
     * 当前服务器在线用户
     */
    private static CopyOnWriteArrayList<User> liveUsers = new CopyOnWriteArrayList<>();


    static {
        liveUsers.add(new User("head"));
    }


    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(@PathParam(value = "userName") String userName, Session session) {
        System.out.println(userName);
        //接收到发送消息的人员编号
        this.currentUser = userName;

        User user = new User(userName);
        // 判断用户是否己经在列表中，不在就添加到列表
        boolean flag = liveUsers.contains(user);

        if (!flag) {
            liveUsers.add(user);
        }

        this.session = session;
        webSocketSet.add(this);
        //在线数加1
        addOnlineCount();
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
        for (MyWebSocket item :
                webSocketSet) {
            System.out.println(item.getCurrentUser());
        }
        try {
            // 在有人访问服务器时就把在线用户列表(除了自己)发送给所有在线用户

            this.sendInfo2All(JSON.toJSONString(liveUsers));
        } catch (IOException e) {
            System.out.println("Json转换异常:" + e.getMessage());
        }
    }


    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        //从set中删除
        webSocketSet.remove(this);
        //在线数减1
        subOnlineCount();
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
        // 移除该用户,通知给所有用户当前的用户列表
        User user = new User();
        user.setName(this.currentUser);
        liveUsers.remove(user);

        try {
            this.sendInfo2All(JSON.toJSONString(liveUsers));
        } catch (IOException e) {
            System.out.println("Json转换异常:" + e.getMessage());
        }
    }

    /**
     * 收到客户端消息后调用的方法
     * 聊天室逻辑：
     * 1. 传输参数目的地ip，就创建房间
     * 1.1 创建房间
     * 1.2 用户添加进房间
     * 1.3 给目的地ip发送请求
     * 1.4 等待目的地ip发送sdp ice过来
     * 1.5 把目的地用户添加进房间
     * 2. 传输参数房间id，就加入房间
     *
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String message, Session session) {
//        System.out.println("来自客户端的消息:" + message);
        // 发起电话邀请 remote->local
        String descUser = "";
        if (message.startsWith("descName:")) {
            descUser = message.substring(message.indexOf(":") + 1);
            // 通知对应的用户有人找你
            try {
                sendMessageTo("call:" + currentUser, descUser);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        // 响应电话邀请 local->remote 的sdp和ice
        String answerUser = "";
        if (message.startsWith("remoteUser:")) {
            answerUser = message.substring(message.indexOf(":") + 1, message.indexOf(";"));
            System.out.println("answerUser:" + answerUser);
            // 取掉remoteUser头
            message = message.substring(message.indexOf(";") + 1);
            System.out.println("msg:" + message);
            // 给指定的远端用户发送sdp和ice
            try {
                sendMessageTo(message, answerUser);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // remote->local 的sdp和ice
        if (message.startsWith("descUser:")) {
            descUser = message.substring(message.indexOf(":") + 1, message.indexOf(";"));
            System.out.println("descUser:" + descUser);
            // 取掉remoteUser头
            message = message.substring(message.indexOf(";") + 1);
            System.out.println("msg:" + message);
            // 把远端的sdp和ice发送给本地
            try {
                sendMessageTo(message, descUser);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }


    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 群发自定义消息,除了自己
     */
    public void sendInfo(String message) throws IOException {
        for (MyWebSocket item : webSocketSet) {
            if (this.equals(item)) {
                continue;
            }
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                continue;
            }
        }
    }

    /**
     * 群发自定义消息
     */
    public void sendInfo2All(String message) throws IOException {
        for (MyWebSocket item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                continue;
            }
        }
    }

    /**
     * 发送给指定用户
     *
     * @param message
     * @param userCode
     * @throws IOException
     */
    public static void sendMessageTo(String message, String userCode) throws IOException {
        for (MyWebSocket item : webSocketSet) {
            if (item.currentUser.equals(userCode)) {
                item.session.getBasicRemote().sendText(message);
            }
        }
    }


    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        MyWebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        MyWebSocket.onlineCount--;
    }


    public static void setOnlineCount(int onlineCount) {
        MyWebSocket.onlineCount = onlineCount;
    }

    public static CopyOnWriteArraySet<MyWebSocket> getWebSocketSet() {
        return webSocketSet;
    }

    public static void setWebSocketSet(CopyOnWriteArraySet<MyWebSocket> webSocketSet) {
        MyWebSocket.webSocketSet = webSocketSet;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Map<Room, List<User>> getUsers() {
        return users;
    }

    public void setUsers(Map<Room, List<User>> users) {
        this.users = users;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MyWebSocket that = (MyWebSocket) o;
        return Objects.equals(session, that.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session);
    }
}