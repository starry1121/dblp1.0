package com.dblp;


import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

class Result {
    public static int articleNum;
    public static int count;

    public Result(int articleNum,int count){
        this.articleNum=articleNum;
        this.count=count;
    }
}


public class client {

    static Result result = new Result(0,0);
    public static String input;
    public static Socket[] socketList;

    static {
        try {
            socketList = new Socket[]{
                    new Socket(InetAddress.getLocalHost(), 9996),
                    new Socket(InetAddress.getLocalHost(), 9997),
                    new Socket(InetAddress.getLocalHost(), 9998),
                    new Socket(InetAddress.getLocalHost(), 9999),
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        System.out.println("请输入作者姓名：");
        Scanner author = new Scanner(System.in);

        input = author.next();

        LinkManager linkManager1 = new LinkManager(socketList,0,result);
        LinkManager linkManager2 = new LinkManager(socketList,1,result);
        LinkManager linkManager3 = new LinkManager(socketList,2,result);
        LinkManager linkManager4 = new LinkManager(socketList,3,result);
        Thread thread1 = new Thread(linkManager1);
        Thread thread2 = new Thread(linkManager2);
        Thread thread3 = new Thread(linkManager3);
        Thread thread4 = new Thread(linkManager4);
        thread1.start();//启动第1个线程
        thread2.start();//启动第2个线程
        thread3.start();//启动第3个线程
        thread4.start();//启动第4个线程

        while(result.count<5){
            Thread.sleep(1000);
            System.out.println("主线程输出count="+result.count);
            System.out.println("主线程输出articleNum="+result.articleNum);
            if(result.count==4)
                break;
        }
    }
}

class LinkManager implements Runnable {

    Socket[] socket;
    int serverNum;
    Result result;

    public LinkManager(Socket[] socketList, int serverNum, Result result){
        this.socket=socketList;
        this.serverNum=serverNum;
        this.result=result;
    }

    public void linkServer() throws IOException{
        //1. 连接服务端 (ip , 端口）
        //解读: 连接本机的 9999端口, 如果连接成功，返回Socket对象
        System.out.println("客户端 socket返回=" + socket[serverNum].getClass());
        //2. 连接上后，生成Socket, 通过socket.getOutputStream()
        OutputStream outputStream = socket[serverNum].getOutputStream();

        //3. 通过输出流，写入数据到 数据通道
        outputStream.write(client.input.getBytes());
        //   设置结束标记
        socket[serverNum].shutdownOutput();

        //4. 获取和socket关联的输入流. 读取数据(字节)，并显示
        InputStream inputStream = socket[serverNum].getInputStream();
        DataInputStream dis = new DataInputStream(inputStream);
        int number = dis.readInt();
        System.out.println("number=" + number);
        System.out.println(Thread.currentThread().getName()+"准备修改count " + result.articleNum);

        //处理同步问题，加锁
        synchronized (result) {
            result.count+=1;
            result.articleNum +=number;
        }

        System.out.println(Thread.currentThread().getName()+"已修改count " + result.articleNum);
//        byte[] buf = new byte[1024];
//        int readLen = 0;
//        while ((readLen = inputStream.read(buf)) != -1) {
//            System.out.println(new String(buf, 0, readLen));
//        }

        //5. 关闭流对象和socket
        inputStream.close();
        outputStream.close();
        socket[serverNum].close();
        System.out.println("客户端退出.....");
    }

    @Override
    public void run() {

        try {
            linkServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
