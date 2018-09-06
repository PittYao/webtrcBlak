package com.websocket.webtrc.test.websocket;

import com.alibaba.fastjson.JSON;
import com.websocket.webtrc.domain.Room;
import com.websocket.webtrc.domain.User;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 测试socket后端连接
 *
 * @author pitt
 * @date 2018/09/5
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
    private static List<Room> rooms = new ArrayList<>();

    /**
     * 当前发消息的人员编号
     */
    private String currentUser;

    /**
     * 当前服务器在线用户
     */
    private static CopyOnWriteArrayList<User> liveUsers = new CopyOnWriteArrayList<>();

    /**
     * 远端选择在线列表中要和谁连线的userName
     */
    private static final String DESC_NAME = "descName:";

    /**
     * local端给remote端发送sdp和ice时指定的remote用户名
     */
    private static final String REMOTE_USER = "remoteUser:";

    /**
     * remote端给local端发送sdp和ice时指定的local端用户名
     */
    private static final String LOCAL_USER = "descUser:";

    /**
     * 当local端获取远端音频成功时，会发送连接成功的头信息
     */
    private static final String CONNECTION_SUCCESS = "connectionSuccess|";


    static {
        liveUsers.add(new User("head"));
    }


    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(@PathParam(value = "userName") String userName, Session session) {
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
            // 在有人访问服务器时就把在线用户列表发送给所有在线用户
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
        // 发起电话邀请 remote->local
        String descUser = "";
        if (message.startsWith(DESC_NAME)) {
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
        if (message.startsWith(REMOTE_USER)) {
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
        if (message.startsWith(LOCAL_USER)) {
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
        // 连接成功后创建房间等后续操作
        if (message.startsWith(CONNECTION_SUCCESS)) {
            // 获取localUser
            String localUser = this.currentUser;
            // 获取remoteUser
            String remoteUser = message.substring(message.indexOf(":") + 1);
            // 创建房间
            Room room = new Room();
            // 将用户分配到房间中
            List<User> users = room.getUsers();
            User localUsers = new User(localUser);
            User remoteUsers = new User(remoteUser);

            users.add(localUsers);
            users.add(remoteUsers);
            // 将该房间加入房间列表中
            rooms.add(room);
            // 分发给所有用户当前房间列表的状况
            try {
                sendInfo2All("liveRooms:" + JSON.toJSONString(rooms));
                System.out.println("rooms:" + JSON.toJSONString(rooms));
                // 连接成功后通知给相应的已连接的用户
                sendMessageTo(CONNECTION_SUCCESS + remoteUser, localUser);
                sendMessageTo(CONNECTION_SUCCESS + localUser, remoteUser);
            } catch (IOException e) {
                System.out.println("房间列表Json转换失败:" + e.getMessage());
            }
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
            System.out.println("onClose中更新在线人数Json转换异常:" + e.getMessage());
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("socket发生错误");
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