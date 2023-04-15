package client;


import model.*;
import model.ResponseType.RESPONSE_TYPES;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class dummyClient {
    static ClientConnection clientConnection1;
    static ClientConnection clientConnection2;
    static File file;
    static int timesCon1HasBeenUsed = 0;
    static int timesCon2HasBeenUsed = 0;
    static List<ClientConnection> connections = new ArrayList<>();
    static ClientConnection currentConnection;


    private void sendInvalidRequest(String ip, int port) throws IOException{
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(4, 0, 0, 0, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
        dsocket.receive(receivePacket);
        ResponseType response=new ResponseType(receivePacket.getData());
        loggerManager.getInstance(this.getClass()).debug(response.toString());
    }

    private void getFileList(String ip, int port) throws IOException{
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_LIST, 0, 0, 0, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
        dsocket.receive(receivePacket);
        FileListResponseType response=new FileListResponseType(receivePacket.getData());
        System.out.println(response);
        //loggerManager.getInstance(this.getClass()).debug(response.toString());
    }

    private long getFileSize(String ip, int port, int file_id) throws IOException{
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_SIZE, file_id, 0, 0, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
        dsocket.receive(receivePacket);
        FileSizeResponseType response=new FileSizeResponseType(receivePacket.getData());
        return response.getFileSize();
    }

    private void getFileDataTest(int file_id,FilePart filePart, ClientConnection clientConnection) throws IOException{
        long maxReceivedByte=-1;

        while(maxReceivedByte<filePart.getEndByte()){
            FileDataResponseType response = currentConnection.getFilePartFromSocket(file_id,filePart.getStartByte(),filePart.getEndByte());
            if (response==null) {
                currentConnection = switchConnection(currentConnection);
            }
            else {
                if (response.getResponseType()!=RESPONSE_TYPES.GET_FILE_DATA_SUCCESS){
                    break;
                }
                if (response.getEnd_byte()>maxReceivedByte){
                    filePart.setResponseType(response);
                    maxReceivedByte=response.getEnd_byte();
                }
            }

        }
    }

    private ClientConnection switchConnection(ClientConnection con){
        if (con.getPort()==clientConnection1.getPort()) return clientConnection2;
        else return clientConnection1;
    }

    private void startDownload(String ip, int port, int file_id, long end_byte) throws IOException, NoSuchAlgorithmException {
        System.out.println("Starting to download.");
        long fileSize = getFileSize(ip,port,file_id);
        int maxResponseSize = ResponseType.MAX_DATA_SIZE;
        int partListSize = (int) ((fileSize / maxResponseSize) + 1);

        file = new File((int)fileSize);
        long start = System.currentTimeMillis();
        List<FilePart> partList = new ArrayList<>();
        for (int i=0; i<partListSize; i++){
            partList.add(new FilePart());
        }
        assignBytesToFileParts(partList);

        for (int i=0; i<partList.size(); i++){
            if (i!=partList.size()-1) {
                currentConnection = decideConnection(clientConnection1, clientConnection2);
                getFileDataTest(file_id, partList.get(i), currentConnection);
                partList.get(i).setStatus(checkIfFilePartCorrupted(partList.get(i)) != -1);
            }
            else {
                //last byte[]
                partList.get(partList.size()-1).setEndByte(end_byte);
                currentConnection = decideConnection(clientConnection1, clientConnection2);
                getFileDataTest(file_id,partList.get(partList.size()-1), currentConnection);
                partList.get(partList.size()-1).setStatus(checkIfFilePartCorrupted(partList.get(partList.size()-1)) != -1);
            }
        }
        resendLostPackets(file_id,file,clientConnection1);

        sumFileParts(partList, file);
        long end = System.currentTimeMillis();
        System.out.println("MD5 is: " + file.generateMd5Hash());
        System.out.println("Elapsed time: "+ (end-start)+"ms");
    }

    private void resendLostPackets(int file_id, File file, ClientConnection clientConnection) throws IOException {

        for (int i=0; i<file.getData().size();i++) {
            if (!file.getData().get(i).isStatus()){
                System.out.println("Packet loss!");
                getFileDataTest(file_id,file.getData().get(i),clientConnection);
            }
        }
    }

    private int checkIfFilePartCorrupted(FilePart filePart){
        return checkIfDataCorrupted(filePart.getResponseType(),filePart.getStartByte(),filePart.getEndByte());
    }

    private void assignBytesToFileParts(List<FilePart> partList){
        for (int i=0; i < partList.size(); i++) {
            partList.get(i).assignByteValues(1 + ((long) i * ResponseType.MAX_DATA_SIZE), (long) (i + 1) * ResponseType.MAX_DATA_SIZE);
        }
    }

    private int checkIfDataCorrupted(ResponseType responseType, long start, long end){
        if (responseType != null){
            if (responseType.getData().length>0){
                if (responseType.getData().length != end-start+1){
                    return -1;
                }
                else  return 1;
            }
            else return -1;
        }
        else return -1;
    }

    private ClientConnection decideConnection(ClientConnection con1, ClientConnection con2){
        if (con1.getTimesConHasBeenUsed()-con2.getTimesConHasBeenUsed()>8){
            return con2;
        }
        else if (con2.getTimesConHasBeenUsed()-con1.getTimesConHasBeenUsed()>8){
            return con1;
        }

        //speed
        else if(con1.getSpeed()>con2.getSpeed()){
            return con1;
        }
        else if(con2.getSpeed()>con1.getSpeed()){
            return con2;
        }
        else return con1;
    }

    private void sumFileParts(List<FilePart> fileParts, File file) {
        for (int i=0; i<fileParts.size();i++){
            if (checkIfFilePartCorrupted(fileParts.get(i))!=1){
                System.out.println("file corrupt");
            }
            else {
                file.addFilePart(i ,fileParts.get(i));
            }
        }
        file.initRawData();
    }



    public static void main(String[] args) throws Exception{

        Scanner sc = new Scanner(System.in);
        String ip = "192.168.0.152";
        String ports = "5000:5001";
        dummyClient inst=new dummyClient();
        String input = "";
        boolean exit = true;

        System.out.println("Please enter the ip of the server you want to connect to.");
        //ip = sc.next();
        System.out.println("Please enter the ports you will use.");
        System.out.println("e.g -> 5000:5001");
        //ports = sc.next();
        String[] adr1=ports.split(":");
        int port1=Integer.valueOf(adr1[0]);
        int port2=Integer.valueOf(adr1[1]);

        clientConnection1 = new ClientConnection(ip, port1);
        clientConnection2 = new ClientConnection(ip, port2);
        connections.add(clientConnection1);
        connections.add(clientConnection2);
        System.out.println("Available files:");
        inst.getFileList(ip,port1);

        while (exit){
            System.out.println("To exit the application please enter X");
            System.out.println("Enter a number: ");
            input = sc.next();
            if (input.equalsIgnoreCase("x")){
                exit = false;
            }
            else if (Integer.parseInt(input)==1){
                System.out.println("File 1 has been selected. Getting the file size.");
                long size=inst.getFileSize(ip,port1,1);
                System.out.println("File size is " + size);
                inst.startDownload(ip,port1,1 ,size);
            }
            if (Integer.parseInt(input)==2){
                System.out.println("File 2 has been selected. Getting the file size.");
                long size=inst.getFileSize(ip,port1,2);
                System.out.println("File size is " + size);
                inst.startDownload(ip,port1,2 ,size);
            }
        }
    }
}
