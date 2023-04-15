package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import model.*;
import model.ResponseType.RESPONSE_TYPES;


public class dummyClient {
    static ClientConnection clientConnection1;
    static ClientConnection clientConnection2;

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
	
	private int getFileList(String ip, int port) throws IOException{
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
        loggerManager.getInstance(this.getClass()).debug(response.toString());
        return response.getFile_id();
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
        loggerManager.getInstance(this.getClass()).debug(response.toString());
        return response.getFileSize();
	}

    private void getFileDataTest(String ip, int port, int file_id,FilePart filePart, ClientConnection clientConnection) throws IOException{

        long maxReceivedByte=-1;

        while(maxReceivedByte<filePart.getEndByte()){

        FileDataResponseType response = clientConnection.getFilePartFromSocket(file_id,filePart.getStartByte(),filePart.getEndByte());
        loggerManager.getInstance(this.getClass()).debug(clientConnection.getPort());

        if (response.getResponseType()!=RESPONSE_TYPES.GET_FILE_DATA_SUCCESS){
            break;
        }
        if (response.getEnd_byte()>maxReceivedByte){
            maxReceivedByte=response.getEnd_byte();
        }
        }
    }

    private void startDownload(String ip, int port, int file_id, long end_byte) throws IOException {
        ClientConnection mainConnection;

        long fileSize = getFileSize(ip,port,file_id);
        int maxResponseSize = ResponseType.MAX_DATA_SIZE;
        int partListSize = (int) ((fileSize / maxResponseSize) + 1);

        List<FilePart> partList = new ArrayList<>();
        for (int i=0; i<partListSize; i++){
            partList.add(new FilePart());
        }

        assignBytesToFileParts(partList);


        for (int i=0; i<partList.size(); i++){
            if (i!=partList.size()-1){
                mainConnection = decideConnection(clientConnection1, clientConnection2);
                getFileDataTest(ip,port,file_id,partList.get(i),mainConnection);
            }
            else partList.get(partList.size()-1).setEndByte(end_byte);
        }

    }

    private void assignBytesToFileParts(List<FilePart> partList){
        for (int i=0; i < partList.size(); i++) {
            partList.get(i).assignByteValues(1 + ((long) i * ResponseType.MAX_DATA_SIZE), (long) (i + 1) * ResponseType.MAX_DATA_SIZE);
        }
    }

    private int checkIfFileCorrupted(ResponseType responseType, long start, long end){
        if (responseType.getData()!=null){
            if (responseType.getData().length == (int) end - start)
            {
                return 1;
            }
            else return -1;
        }
        else return -1;
    }

    private ClientConnection decideConnection(ClientConnection connection1, ClientConnection connection2){
        loggerManager.getInstance(this.getClass()).debug(connection1.toString());
        loggerManager.getInstance(this.getClass()).debug(connection2.toString());
        if (connection1.getSpeed()>connection2.getSpeed())
            return connection1;
        else return connection2;
    }



	public static void main(String[] args) throws Exception{
		if (args.length<2){//TODO CHECK IF THE ARGUMENT IS CORRECT
			throw new IllegalArgumentException("ip:port is mandatory");
		}
        try{
            String[] arg = args[0].split(":");
            String ip1 = arg[0];
            int port1 = Integer.parseInt(arg[1]);
            arg = args[1].split(":");
            String ip2 = arg[0];
            int port2 = Integer.parseInt(arg[1]);

            dummyClient inst = new dummyClient();
            clientConnection1 = new ClientConnection(ip1,port1);
            clientConnection2 = new ClientConnection(ip2,port2);
            inst.sendInvalidRequest(ip1,port1);

//		    inst.getFileSize(ip1,port1,0);
//		    long size=inst.getFileSize(ip1,port1,1);
//		    inst.getFileData(ip1,port1,0,0,1);
//		    inst.getFileData(ip1,port1,1,30,20);
            System.out.println("File List: ");
            int count = inst.getFileList(ip1,port1);
            Scanner userInput = new Scanner(System.in);
            System.out.println("Enter a number: ");
            int choice = userInput.nextInt();
            long size = inst.getFileSize(ip1,port1,choice);
            inst.startDownload(ip1,port1,choice,size);
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
	}
}
