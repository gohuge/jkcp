package test;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPServer {
    public static void main(String[] args) {
        int port = 12345; // 服务器监听的端口号
        try (DatagramSocket serverSocket = new DatagramSocket(port)) {
            System.out.println("UDP server is listening on port " + port);

            // 缓冲区大小
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            while (true) {
                // 阻塞等待接收数据包
                serverSocket.receive(receivePacket);

                // 获取发送者地址和端口
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                // 打印接收到的数据
                String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Received message from " + clientAddress.getHostAddress() + ":" + clientPort + ": " + receivedData);

                // 创建响应数据包
                String response = "Echo: " + receivedData;
                byte[] sendData = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);

                // 发送响应
                serverSocket.send(sendPacket);
            }
        } catch (SocketException e) {
            System.err.println("Cannot open socket on port " + port);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error occurred while receiving or sending packets.");
            e.printStackTrace();
        }
    }
}