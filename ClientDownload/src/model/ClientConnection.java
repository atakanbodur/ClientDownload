package model;

import java.io.IOException;
import java.net.*;

public class ClientConnection {
    private String ip;
    private int port;
    private Double speed;
    private Double packetLossRate;
    private DatagramSocket dsocket;

    public ClientConnection(String ip, int port) {
        this.ip = ip;
        this.port = port;
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
            byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            dsocket.receive(receivePacket);
            //timer end
            //this.speed=datasize/end-start
            response = new FileDataResponseType(receivePacket.getData());
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
