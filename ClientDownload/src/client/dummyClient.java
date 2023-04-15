package client;


import model.*;
import model.ResponseType.RESPONSE_TYPES;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;


public class dummyClient {
    static ClientConnection clientConnection1;
    static ClientConnection clientConnection2;
    static File file;
    static List<ClientConnection> connections = new ArrayList<>();
    static ClientConnection currentConnection;
    static Boolean timerExpired = false;



    private FileListResponseType getFileList(String ip, int port) throws IOException{
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
        //System.out.println(response);
        //loggerManager.getInstance(this.getClass()).debug(response.toString());
        return response;
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
            FileDataResponseType response = clientConnection.getFilePartFromSocket(file_id,filePart.getStartByte(),filePart.getEndByte());
            if (response==null) {
                clientConnection = switchConnection(clientConnection);
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
        if (con.getPort()==clientConnection1.getPort())
            return clientConnection2;
        else
            return clientConnection1;
    }

    private void startDownload(String ip, int port, int file_id, long end_byte) throws IOException, NoSuchAlgorithmException {
        System.out.println("Starting to download.");
        long fileSize = getFileSize(ip, port, file_id);
        int maxResponseSize = ResponseType.MAX_DATA_SIZE;
        int partListSize = (int) ((fileSize / maxResponseSize) + 1);

        file = new File((int) fileSize);
        long start = System.currentTimeMillis();
        List<FilePart> partList = new ArrayList<>();
        for (int i = 0; i < partListSize; i++) {
            partList.add(new FilePart());
        }
        assignBytesToFileParts(partList);


        for (int i = 0; i < partList.size(); i++) {
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    timerExpired = true;
                    System.out.println("Timeout occurred. Switching connection...");
                    currentConnection = switchConnection(currentConnection);
                }
            };
            timer.schedule(timerTask, 500);

            if (i != partList.size() - 1) {
                if (!timerExpired) {
                    currentConnection = decideConnection(clientConnection1, clientConnection2);
                }
                getFileDataTest(file_id, partList.get(i), currentConnection);
                partList.get(i).setStatus(checkIfFilePartCorrupted(partList.get(i)) != -1);
                currentConnection.timesConHasBeenUsed++;
                timerExpired = false;
            } else {
                //last byte[]

                partList.get(partList.size() - 1).setEndByte(end_byte);
                if (!timerExpired) {
                    currentConnection = decideConnection(clientConnection1, clientConnection2);
                }

                getFileDataTest(file_id, partList.get(partList.size() - 1), currentConnection);
                partList.get(partList.size() - 1).setStatus(checkIfFilePartCorrupted(partList.get(partList.size() - 1)) != -1);
                currentConnection.timesConHasBeenUsed++;
                timerExpired = false;
            }

            timer.cancel();
        }

        resendLostPackets(file_id, file, decideConnection(clientConnection1, clientConnection2));

        sumFileParts(partList, file);
        long end = System.currentTimeMillis();
        System.out.println("MD5 is: " + file.generateMd5Hash());
        System.out.println("Elapsed time: " + (end - start) + "ms");
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
        System.out.println("con1 speed: " +con1.getSpeed() + " con2:speed: "+con2.getSpeed());

        if (con1.getTimesConHasBeenUsed()-con2.getTimesConHasBeenUsed()>4){
            System.out.println("Connection 1 has been used too many times, switching to other conn");
            return con2;
        }
        else if (con2.getTimesConHasBeenUsed()-con1.getTimesConHasBeenUsed()>4){
            System.out.println("Connection 2 has been used too many times, switching to other conn");
            return con1;
        }

        //speed
        if(con1.getSpeed()>con2.getSpeed()){
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
        if(args.length<2){
            System.out.println("Invalid use should be in the form of: \"my_client server_IP1:5000 server_IP2:5001\"");
            return;
        }

        String[] arg = args[0].split(":");
        String ip1 = arg[0];
        int port1 = Integer.parseInt(arg[1]);
        arg = args[1].split(":");
        String ip2 = arg[0];
        int port2 = Integer.parseInt(arg[1]);

        dummyClient client = new dummyClient();
        String input = "";

        clientConnection1 = new ClientConnection(ip1, port1);
        clientConnection2 = new ClientConnection(ip2, port2);
        connections.add(clientConnection1);
        connections.add(clientConnection2);
        System.out.println("Available files:");
        FileListResponseType response = client.getFileList(ip1,port1);
        int fileCount = response.getFile_id();

        Scanner console = new Scanner(System.in);
        while (true){
            System.out.println(response);
            System.out.println("To exit the application please enter x");
            System.out.print("Enter a number: ");
            input = console.next();

            if (input.equalsIgnoreCase("x")){
                System.out.println("Exiting.");
                break;
            }
            try{
                int choice = Integer.parseInt(input);
                if(choice>0 && choice<=fileCount){
                    System.out.println("File "+choice+" has been selected. Getting the file size.");
                    long size=client.getFileSize(ip1,port1,choice);
                    System.out.println("File size is " + size);
                    client.startDownload(ip1,port1,choice ,size);
                }else{
                    System.out.println("Invalid number.");
                }
            }catch(Exception exception){
                System.out.println("Invalid number.");
            }
        }
    }
}
