package com.wen.netty.nio.classicNio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author: 7wen
 * @Date: 2023-04-18 15:49
 * @description:
 * 同步异步(是否依赖上一个任务,main和 main中声明一个thread1的关系)
 * 阻塞非阻塞(线程是否在任务执行的时候干别的事)
 * bio 初级版本 -  一个人 待在工作台前准备烧10壶水 先烧第一壶水 等水开了再烧第2壶 同步(依赖上一个任务)+阻塞(只能处于当前任务中不管线程是不是闲置状态)
 *     改进版本 -  分配10个人 分别待在工作台前烧10壶水 虽然解决了初级版本的效率问题 但是创建线程是很消耗系统资源的行为
 * nio 初级版本(本版本) - 一个人 先烧第一壶水 烧上了在去烧第二壶 等十壶水都烧上后 依次轮询水烧开没有 烧开了则完成一个烧水任务 也就是同步+非阻塞 解决了可以一个线程处理多个客户端的请求 但是该版本也有问题 如果有10w个连接 上千w个消息这种方式效率太慢 且
 */
public class Nio_version1 {
    static List<SocketChannel> channelList = new ArrayList<SocketChannel>();

    public static void main(String[] args) throws IOException {
        nioServer();
    }

    /**
     * nio服务器
     * @author 7wen
     * @date 2023-04-18 21:07
     */
    public static void nioServer() throws IOException {
        //设置Nio的管道
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.socket().bind(new InetSocketAddress(9001));
        //设置管道为非阻塞式
        channel.configureBlocking(false);
        System.out.println("-----服务启动成功---");

        //开始监听,无限监听客户端是否有事件处理
        while (true) {
            //监听客户端的连接
            SocketChannel accept = channel.accept();
            if (accept != null) {
                //如果客户端不为空
                System.out.println("-----客户端连接成功-----");
                //设置客户端为非阻塞
                accept.configureBlocking(false);
                //保存用户至客户端连接列表中
                channelList.add(accept);
            }

            //遍历连接开始读取消息
            Iterator<SocketChannel> iterator = channelList.iterator();
            //如果有管道则进行信息读取
            while (iterator.hasNext()) {
                SocketChannel listOfSocket = iterator.next();
                //获取搬运工容器
                ByteBuffer allocate = ByteBuffer.allocate(128);
                //开始读取消息,非阻塞式模式read方法不会阻塞,否则会阻塞
                int read = listOfSocket.read(allocate);
                if (read > 0) {
                    System.out.println(Thread.currentThread().getName() + "已经读取到来自 "+listOfSocket.getLocalAddress()+" 消息:" + new String(allocate.array(),"utf-8"));
                } else if (read == -1) {
                    iterator.remove();
                    System.out.println("-----客户端已断开连接-----");
                }
            }
        }
    }
}
