package com.websocket.webtrc.test.websocket;

import java.io.IOException;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

import domain.User;

/**
 * 测试socket后端连接
 */
@ServerEndpoint(value = "/MyWebsocket/{username}")
@Component
public class MyWebSocket {

    /**
     * 协议头
     */

    /**
     * 邀请连接头
     */
    private static final String CALL = "call:";

    /**
     * sdp 和 ice的tag头
     */

    /**
     * 发起电话邀请头
     */
    private static final String SRC_USER = "srcUser:";

    /**
     * Field description
     */
    private static final String LOCAL_USER = "localUser:";

    /**
     * Field description
     */
    public static final String REMOTE_USER = "remoteUser:";

    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     */
    private static CopyOnWriteArraySet<MyWebSocket> webSocketSet = new CopyOnWriteArraySet<MyWebSocket>();

    /**
     * 在线用户
     */
    private static CopyOnWriteArrayList<User> liveUsers = new CopyOnWriteArrayList<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 当前访问服务的用户
     */
    private String currentUser;

    /**
     * Method description
     */
    public static synchronized void addOnlineCount() {
        MyWebSocket.onlineCount++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        MyWebSocket that = (MyWebSocket) o;

        return Objects.equals(session, that.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {

        /** 从set中删除 */
        webSocketSet.remove(this);

        /** 在线数减1 */
        subOnlineCount();
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * Method description
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("来自客户端的消息:" + message);

        // 接收发出连接的请求
        if (message.startsWith(CALL)) {

            // 取下头tag
            String descUser = message.substring(message.indexOf(":") + 1);

            // 发送给descUser
            send2SomeOne(CALL + currentUser, descUser);
        }

        // 接收给local->remote的sdp ice
        if (message.startsWith(SRC_USER)) {
            sendSdpOrIce(LOCAL_USER, message);
        }

        // 接收remote->local的sdp ice
        if (message.startsWith(LOCAL_USER)) {
            sendSdpOrIce(REMOTE_USER, message);
        }
    }

    /**
     * 连接建立成功调用的方法
     *
     * @param username
     * @param session
     */
    @OnOpen
    public void onOpen(@PathParam(value = "username") String username, Session session) {
        System.out.println(username);
        this.session = session;

        // 加入在线用户列表
        this.currentUser = username;

        // 用户去重
        User user = new User(username);

        if (liveUsers.contains(user)) {
            return;
        }

        liveUsers.add(user);

        /** 加入set中 */
        webSocketSet.add(this);

        // 通知所有用户在线列表,liveUsers头部tag
        String liveUser = "liverUsers:";

        send2All(liveUser + JSON.toJSONString(liveUsers));
        System.out.println(JSON.toJSONString(liveUsers));

        /** 在线数加1 */
        addOnlineCount();
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
    }

    /**
     * 广播消息
     *
     * @param msg
     */
    public void send2All(String msg) {
        for (MyWebSocket item : webSocketSet) {
            try {
                item.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 指定发送消息
     *
     * @param msg
     * @param userName
     */
    public void send2SomeOne(String msg, String userName) {
        for (MyWebSocket item : webSocketSet) {
            if (item.currentUser.equals(userName)) {
                try {
                    item.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 广播消息,除了自己
     *
     * @param message
     * @throws IOException
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
     * Method description
     *
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 接收sdp ice并表示是谁的sdp和ice
     *
     * @param answerUser 谁把sdp发送过去
     * @param message    sdp或ice
     */
    public void sendSdpOrIce(String answerUser, String message) {

        // 取下头tag,发送给谁
        String sendToUser = message.substring(message.indexOf(":") + 1, message.indexOf(";"));

        // 获取sdp或ice信息
        String sdpOrIce = message.substring(message.indexOf(";") + 1);

        // 发送给descUser
        send2SomeOne(answerUser + currentUser + ";" + sdpOrIce, sendToUser);
    }

    /**
     * Method description
     */
    public static synchronized void subOnlineCount() {
        MyWebSocket.onlineCount--;
    }

    /**
     * Method description
     *
     * @return
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }
}


