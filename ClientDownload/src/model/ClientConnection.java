package model;

import java.io.IOException;
import java.net.*;

//TODO: added a timer but it shows speed as 0
public class ClientConnection {
    private String ip;
    private int port;
    private long speed;
    private Double packetLossRate;
    private DatagramSocket dsocket;


    public ClientConnection(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.speed = 0L;
    }

    public FileDataResponseType getFilePartFromSocket(int file_id, long start, long end) {
        InetAddress IPAddress = null;
        FileDataResponseType response = null;
        try {
            IPAddress = InetAddress.getByName(ip);
            RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
            byte[] sendData = req.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            dsocket = new DatagramSocket();
            dsocket.send(sendPacket);
            //timer start
            long startTime = System.nanoTime();
            byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            dsocket.receive(receivePacket);
            //timer end
            long stopTime = System.nanoTime();
            long elapsedTime = stopTime - startTime;
            this.speed = (end-start)/elapsedTime;
            //this.speed=datasize/end-start
            response = new FileDataResponseType(receivePacket.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "ClientConnection{" +
                "port=" + port +
                ", speed=" + speed +
                ", packetLossRate=" + packetLossRate +
                '}';
    }
}
