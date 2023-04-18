package com.wen.netty.nio.multNio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author: 7wen
 * @Date: 2023-04-18 15:49
 * @description:
 *
 * 同步异步(是否依赖上一个任务, main和 main中声明一个thread1的关系)
 * 阻塞非阻塞(线程是否在任务执行的时候干别的事)
 *
 * bio 初级版本 -  一个人 待在工作台前准备烧10壶水 先烧第一壶水 等水开了再烧第2壶 同步(依赖上一个任务)+阻塞(只能处于当前任务中不管线程是不是闲置状态)
 * 改进版本 -  分配10个人 分别待在工作台前烧10壶水 虽然解决了初级版本的效率问题 但是创建线程是很消耗系统资源的行为
 * nio 初级版本 - 一个人 先烧第一壶水 烧上了在去烧第二壶 等十壶水都烧上后 依次轮询水烧开没有 烧开了则完成一个烧水任务 也就是同步+非阻塞 解决了可以一个线程处理多个客户端的请求 但是该版本也有问题 如果有10w个连接 上千w个消息这种方式效率太慢 且
 * 改进版本(本版本) - 一个人A 请了另一个人B把十个壶的名字记住 然后开始监视烧水壶烧水的情况 如果某两个烧开了就标记这两个壶的状态告诉B 然后B把这两个壶拿起来任务结束 也就是同步+非阻塞+多路复用的版本 改进了初级版本不断轮询
 *
 * 主要方法解析:
 *
 * ServerSocketChannel 服务器管道 需要open
 *
 * Selector 多路复用器 需要open
 *
 * serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); 管道注册事件 作通知多路复用器用
 *
 * selector.select() 监听事件
 *
 * selector.selectedKeys() 获取事件key群
 *
 * selectionKey 单个事件体
 *  1.可以获取状态 selectionKey.isAcceptable()
 *  2.可以根据状态获取当前事件的管道类型(boss管道还是work管道)
 *      (ServerSocketChannel) selectionKey.channel() or (SocketChannel) selectionKey.channel()
 *
 * boss管道通过获取连接获取work管道 SocketChannel workChannel = eventBossChannel.accept()
 * work管道可以根据缓冲区直接进行读写IO操作
 */
public class Nio_version2 {
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
        //获取服务器管道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //绑定端口
        serverSocketChannel.socket().bind(new InetSocketAddress(9001));
        //设置服务器管道为非阻塞式
        serverSocketChannel.configureBlocking(false);
        //声明多路复用器
        Selector selector = Selector.open();
        //注册服务器管道的获取连接事件到多路复用器中 如果该管道发生了获取连接事件将会通知多路复用器
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("-----服务启动成功---");

        //开始监听,无限监听客户端是否有事件处理
        while (true) {
            //监听客户端的连接事件发生,底层实现为内核轮询机制,如果监听到注册事件发生则执行后面逻辑
            selector.select();
            //获取所有复用器检测到的事件keys
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            //获取迭代器
            Iterator<SelectionKey> selectIterator = selectionKeys.iterator();
            //如果有获取连接事件则进入循环依次处理事件
            while (selectIterator.hasNext()) {
                //获取其中一个事件key
                SelectionKey selectionKey = selectIterator.next();
                //对key的状态分别处理
                if (selectionKey.isAcceptable()) {
                    //如果key的状态是获取连接状态则完成对该socket连接工作
                    //获取boss管道
                    ServerSocketChannel eventBossChannel = (ServerSocketChannel) selectionKey.channel();
                    //获取work管道连接
                    SocketChannel workChannel = eventBossChannel.accept();
                    //work管道声明非阻塞式
                    workChannel.configureBlocking(false);
                    //注册work管道为读事件到多路复用器中,如果该管道发生了读事件将会通知多路复用器
                    workChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("客户端连接成功");
                } else if (selectionKey.isReadable()) {
                    //如果key为读状态 则直接使用buffer(缓冲区)获取work管道中的数据
                    ByteBuffer allocate = ByteBuffer.allocate(128);
                    //获取work管道
                    SocketChannel workChannel = (SocketChannel) selectionKey.channel();
                    //开始读取work管道中的数据到缓冲区中
                    int read = workChannel.read(allocate);
                    if (read > 0) {
                        System.out.println(Thread.currentThread().getName() + ":" + new String(allocate.array(), "utf-8"));
                    } else if (read == -1) {
                        workChannel.close();
                        System.out.println("客户端断开连接");
                    }
                }
                //删除掉本次处理的事件,防止多路复用器重复处理
                selectIterator.remove();
            }
        }
    }
}
