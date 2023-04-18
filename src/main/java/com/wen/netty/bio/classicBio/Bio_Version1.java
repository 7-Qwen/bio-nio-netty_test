package com.wen.netty.bio.classicBio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author: 7wen
 * @Date: 2023-04-18 21:05
 * @description: bio 初级版本(本版本) -  一个人 待在工作台前准备烧10壶水 先烧第一壶水 等水开了再烧第2壶 同步(依赖上一个任务)+阻塞(只能处于当前任务中不管线程是不是闲置状态)
 */
public class Bio_Version1 {
    public static void main(String[] args) throws IOException {
        bioServer();
    }


    /**
     * bio服务器
     *
     * @author 7wen
     * @date 2023-04-18 21:07
     */
    static void bioServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(9001);
        while (true) {
            System.out.println("等待连接...");
            //等待中阻塞
            Socket accept = serverSocket.accept();
            //如果有客户端连接则执行后续逻辑
            System.out.println("客户端连接!");
            handler(accept);
        }
    }


    /**
     * 处理器
     *
     * @author 7wen
     * @date 2023-04-18 21:16
     */
    static void handler(Socket clientSocket) throws IOException {
        //声明传输容器
        while (true) {
            //获取管道流信息
            byte[] bytes = new byte[1024];
            if (clientSocket.getInputStream().read(bytes) > 0) {
                System.out.println("接收到的消息为:" + new String(bytes, "utf-8"));
            } else {
                System.out.println("客户端退出...");
                break;
            }
        }
    }
}
