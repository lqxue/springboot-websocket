package com.lqx.dudu.springboot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : lqx
 * @description: : WebSocketServer 服务器
 * @date : 20201227
 **/
@ServerEndpoint("/relay")
@Component
public class WebSocketServer {
    /**
     * 记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 静态变量，用来记录当前在线连接数。线程安全。
     */
    private static AtomicInteger onlineCount = new AtomicInteger(0);
    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     */
    private static ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    /**
     * 维护did对应的所有链接url  检测到有重复的时候就关闭
     */
    private static ConcurrentHashMap<String, String> didUrlMap = new ConcurrentHashMap<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 接收sid
     */
    private String sid = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        //ws://192.168.123.104/relay?ttm.JUynkSkTQzKRtYyRl_rofR3Js04AAAAAbm9kZS50b2tlbnRtLm5ldA.OTUyMWY3OWMtY2M5Yi00OTg5
        // LWFjNjMtOWEwZjQzNjgzNjlk.MvHFNPqdePj_mRPD0_4sdJoOhyzqxA_3XqXbbnq7G5YJdQSJo96MRGl5AGeYR7mmftKCgIiNXDoYvXXYycPOdAA
        sid = session.getRequestURI().toString();
        webSocketMap.put(sid, this);
        // 在线数加1
        addOnlineCount();
        //   path内容是"/relay"
        String path = session.getRequestURI().getPath();
        //query 内容是:ttm.JUynkSkTQzKRtYyRl_rofR3Js04AAAAAbm9kZS50b2tlbnRtLm5ldA.OTUyMWY3OWMtY2M5Yi00OTg5LWFjNjMtOWEwZjQ
        // zNjgzNjlk.MvHFNPqdePj_mRPD0_4sdJoOhyzqxA_3XqXbbnq7G5YJdQSJo96MRGl5AGeYR7mmftKCgIiNXDoYvXXYycPOdAA
        String query = session.getRequestURI().getQuery();
        //判断当前新的连接对应的did是否已经存在了,如果存在了就关闭那个链接然后替换成新的
        String did = query.substring(4, 31);
        if (didUrlMap.containsKey(did)) {
            String url = didUrlMap.get(did);
            WebSocketServer webSocketServer = webSocketMap.get(url);
            if (null != webSocketServer) {
                CloseReason.CloseCode closeCode = new CloseReason.CloseCode() {
                    @Override
                    public int getCode() {
                        return 4011;
                    }
                };
                CloseReason closeReason = new CloseReason(closeCode, url);
                try {
                    webSocketServer.session.close(closeReason);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        //保存新的链接
        didUrlMap.put(did, sid);
        logger.info("有新窗口开始监听: " + sid + ",当前在线人数为" + getOnlineCount());
        logger.info("有新窗口开始监听: " + path + ",当前在线人数为" + getOnlineCount());
        logger.info("有新窗口开始监听: " + query + ",当前在线人数为" + getOnlineCount());

    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        String did = sid.substring(4, 31);
        didUrlMap.remove(did);
        //从set中删除
        webSocketMap.remove(sid);
        //在线数减1
        subOnlineCount();
        logger.info("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("收到来自窗口" + sid + "的信息:" + message);
        //sid是链接 通过连接推送消息
    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        logger.info("发生错误");
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) {
        webSocketMap.get(sid).sendMessage(message);
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            logger.info("消息推送失败");
            e.printStackTrace();
        }
    }

    public static int getOnlineCount() {
        return onlineCount.get();
    }

    public static void addOnlineCount() {
        WebSocketServer.onlineCount.addAndGet(1);
    }

    public static void subOnlineCount() {
        WebSocketServer.onlineCount.decrementAndGet();
    }
}
