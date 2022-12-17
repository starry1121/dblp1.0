package com.client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
    public static String authorInput;
    public static String minYearInupt;
    public static String maxYearInput;
    public static String input;
    public static Socket[] socketList;
    public static String[] filesList={"output_0001.xml","output_0002.xml","output_0003.xml","output_0004.xml"};

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

    public static void main(String[] args) throws InterruptedException, IOException {

        while(true){
            System.out.println("请输入作者姓名：");
            Scanner author = new Scanner(System.in);
            authorInput = author.nextLine();
            if(authorInput=="")
                continue;

            System.out.println("请输入年份下限（可为空）");
            Scanner minYear = new Scanner(System.in);
            minYearInupt = minYear.nextLine();

            System.out.println("请输入年份上限（可为空）");
            Scanner maxYear = new Scanner(System.in);
            maxYearInput = maxYear.nextLine();
            
            if(minYearInupt==""&&maxYearInput!=""){
                input = authorInput + "~-~" + maxYearInput + "~";
            } else if (minYearInupt!=""&&maxYearInput=="") {
                input = authorInput + "~" + minYearInupt + "~-~";
            } else if (minYearInupt==""&&maxYearInput=="") {
                input = authorInput + "~";
            } else if (minYearInupt!=""&&maxYearInput!="") {
                input = authorInput + "~" + minYearInupt + "~" + maxYearInput + "~";
            }
            break;
        }
        
        LinkManager linkManager1 = new LinkManager(socketList,filesList,0,result);
        LinkManager linkManager2 = new LinkManager(socketList,filesList,1,result);
        LinkManager linkManager3 = new LinkManager(socketList,filesList,2,result);
        LinkManager linkManager4 = new LinkManager(socketList,filesList,3,result);
        Thread thread1 = new Thread(linkManager1);
        Thread thread2 = new Thread(linkManager2);
        Thread thread3 = new Thread(linkManager3);
        Thread thread4 = new Thread(linkManager4);
        long t1 = 0;
        thread1.start();//启动第1个线程
        thread2.start();//启动第2个线程
        thread3.start();//启动第3个线程
        thread4.start();//启动第4个线程

        while(result.count<5){
            Thread.sleep(1000);
            System.out.println("主线程输出count="+result.count);
            System.out.println("主线程输出articleNum="+result.articleNum);
            if(result.count==4){
                //5. 关闭流对象和socket
                socketList[0].close();
                socketList[1].close();
                socketList[2].close();
                socketList[3].close();
                System.out.println("客户端退出.....");
                long t2 = System.currentTimeMillis();
                System.out.println("查询用时：" + (t2 - t1) + "ms");
                break;
            }
        }
    }
}

class LinkManager implements Runnable {

    Socket[] socket;
    String[] file;
    int serverNum;
    boolean flag = true;
    int search = 0;
    Result result;
    String input;

    public LinkManager(Socket[] socketList,String[] file, int serverNum, Result result){
        this.socket=socketList;
        this.serverNum=serverNum;
        this.result=result;
        this.file=file;
        this.input=client.input;
    }

    public void linkServer() throws IOException{

        while(flag) {
            //1. 连接服务端 (ip , 端口）
            //连接本机的端口, 如果连接成功，返回Socket对象
            System.out.println("客户端 socket返回=" + socket[serverNum].getClass());

            socket[serverNum].setSoTimeout(5000);
            //2. 连接上后，生成Socket, 通过socket.getOutputStream()
            OutputStream outputStream = socket[serverNum].getOutputStream();

            //3. 通过输出流，写入数据到 数据通道, 使用字符流
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            System.out.println(input + file[serverNum]);
            bufferedWriter.write(input + file[serverNum]);
            bufferedWriter.newLine();//插入一个换行符，表示写入的内容结束, 注意，要求对方使用readLine()!!!!
            bufferedWriter.flush();// 如果使用的字符流，需要手动刷新，否则数据不会写入数据通道

            BufferedReader bufferedReader = null;
            try {
                //4. 获取和socket关联的输入流. 读取数据(字符)，并显示
                socket[serverNum].setSoTimeout(200);

                InputStream inputStream = socket[serverNum].getInputStream();
                byte[] buf = new byte[1024];
                int readLen = 0;
                String s = null;
                while ((readLen = inputStream.read(buf)) != -1) {
                    s = new String(buf, 0, readLen);
                }

                System.out.println(Thread.currentThread().getName() + "准备修改count " + result.articleNum);
                //处理同步问题，加锁
                synchronized (result) {
                    result.count += 1;
                    result.articleNum += Integer.parseInt(s);
                }
                System.out.println(Thread.currentThread().getName() + "已修改count " + result.articleNum);
                this.flag = false;
                System.out.println("已查询到结果！");
            } catch (SocketTimeoutException e) {
                this.search++;
                if (this.serverNum < 3) {
                    this.serverNum++;
                } else if (this.serverNum == 3) {
                    this.serverNum = 0;
                }
                if (this.search > 2) {
                    this.flag = false;
                    System.out.println("已搜索完所有副本，未查询到结果！");
                }
            }
        }
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
