package model;

import client.loggerManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

//TODO: added a timer but it shows speed as 0
public class ClientConnection {
    private String ip;
    private int port;
    private long speed;
    private Double packetLossRate;
    private DatagramSocket dsocket;
    private int timeout = 5000;
    public int timesConHasBeenUsed = 0;


    public ClientConnection(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.speed = 0L;
    }

    public FileDataResponseType getFilePartFromSocket(int file_id, long start, long end) {
        System.out.println("Trying to get part with start_byte: "+ start + ",and end_byte: "+end+" ,from port #: "+ port);
        InetAddress IPAddress = null;
        FileDataResponseType response = null;
        try {
            IPAddress = InetAddress.getByName(ip);
            RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
            byte[] sendData = req.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            dsocket = new DatagramSocket();
            dsocket.setSoTimeout(timeout);
            dsocket.send(sendPacket);
            //timer start
            long startTime = System.nanoTime();
            byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                dsocket.receive(receivePacket);
            }
            catch (SocketTimeoutException exception){
                System.out.println("Failed to get part with start_byte: "+ start + ",and end_byte: "+end+" ,from port #: "+ port);
                return null;
            }
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

    public Double getPacketLossRate() {
        return packetLossRate;
    }

    public void setPacketLossRate(Double packetLossRate) {
        this.packetLossRate = packetLossRate;
    }

    public DatagramSocket getDsocket() {
        return dsocket;
    }

    public void setDsocket(DatagramSocket dsocket) {
        this.dsocket = dsocket;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimesConHasBeenUsed() {
        return timesConHasBeenUsed;
    }

    public void setTimesConHasBeenUsed(int timesConHasBeenUsed) {
        this.timesConHasBeenUsed = timesConHasBeenUsed;
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
