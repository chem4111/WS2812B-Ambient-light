package com.example.colorled;

import android.os.AsyncTask;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSenderTask extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... params) {
        String ipAddress = params[0];
        int port = Integer.parseInt(params[1]);
        String message = params[2];

        try {
            // 创建DatagramSocket
            DatagramSocket socket = new DatagramSocket();

            // 转换消息为字节数组
            byte[] data = message.getBytes();

            // 通过UDP发送数据
            InetAddress serverAddress = InetAddress.getByName(ipAddress);
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
            socket.send(packet);

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
