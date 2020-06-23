package com.weimin.client;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author weimin
 * @ClassName SocketClient
 * @Description TODO
 * @date 2020/6/23 16:20
 */
public class SocketClient {
    public static void main(String[] args) throws IOException {
        Socket socket = null;
        Scanner input = new Scanner(System.in);
        socket = new Socket("127.0.0.1", 8888);
        socket.getOutputStream().write("客户端消息发送".getBytes());
        while (true) {
            String str = input.next();
            socket.getOutputStream().write(str.getBytes());
        }
    }
}
