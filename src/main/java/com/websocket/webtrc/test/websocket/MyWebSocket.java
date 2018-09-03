package com.websocket.webtrc.test.websocket;

import com.websocket.webtrc.domain.Room;
import com.websocket.webtrc.domain.User;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 测试socket后端连接
 */
@ServerEndpoint(value = "/MyWebsocket/{userCode}")
@Component
public class MyWebSocket {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    private static CopyOnWriteArraySet<MyWebSocket> webSocketSet = new CopyOnWriteArraySet<MyWebSocket>();


    /**与某个客户端的连接会话，需要通过它来给客户端发送数据*/
    private Session session;

    /**
    * 每个房间的用户列表
    * */
    private Map<Room, List<User>> users = new HashMap<>();
    /**
     * 房间集合
     */
    private List<Room> rooms = new ArrayList<>();

    /**
     * 当前发消息的人员编号
     * */
    private String currentUser ;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(@PathParam(value = "userCode") String userCode,Session session) {
        System.out.println(userCode);
        //接收到发送消息的人员编号
        this.currentUser = userCode;
        this.session = session;
        webSocketSet.add(this);
        //在线数加1
        addOnlineCount();
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
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
    }
    /**
     * 收到客户端消息后调用的方法
     *      聊天室逻辑：
     *          1. 传输参数目的地ip，就创建房间
     *              1.1 创建房间
     *              1.2 用户添加进房间
     *              1.3 给目的地ip发送请求
     *              1.4 等待目的地ip发送sdp ice过来
     *              1.5 把目的地用户添加进房间
     *          2. 传输参数房间id，就加入房间
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("来自客户端的消息:" + message);
        /*// 判断是否有目的地ip
        if (message.startsWith("descIP:")){
            // 目的地ip
            String descIP = message.substring(message.indexOf("descIP:")+1);
            // 创建房间
            Room room = new Room();
            // 创建用户
            User user = new User();
            // 设置用户的源IP,在ICE 中

            // 给目的地ip发送请求
        }*/




        try {
            this.sendInfo(message);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

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
     * 发送给指定用户
     * @param message
     * @param userCode
     * @throws IOException
     */
    public static void sendMessageTo(String message,String userCode) throws IOException {
        for (MyWebSocket item : webSocketSet) {
            if(item.currentUser.equals(userCode)){
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
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()){ return false;}
        MyWebSocket that = (MyWebSocket) o;
        return Objects.equals(session, that.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session);
    }
}