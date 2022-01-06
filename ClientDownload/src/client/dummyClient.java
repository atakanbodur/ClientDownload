package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import model.*;
import model.ResponseType.RESPONSE_TYPES;
import client.loggerManager;

public class dummyClient {

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
        loggerManager.getInstance(this.getClass()).debug(response.toString());
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
	
	private void getFileData(String ip, int port, int file_id, long start, long end) throws IOException{
		InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req=new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData=new byte[ResponseType.MAX_RESPONSE_SIZE];
        long maxReceivedByte=-1;
        while(maxReceivedByte<end){
        	DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
            dsocket.receive(receivePacket);
            FileDataResponseType response=new FileDataResponseType(receivePacket.getData());
            loggerManager.getInstance(this.getClass()).debug(response.toString());
            if (response.getResponseType()!=RESPONSE_TYPES.GET_FILE_DATA_SUCCESS){
            	break;
            }
            if (response.getEnd_byte()>maxReceivedByte){
            	maxReceivedByte=response.getEnd_byte();
            };
        }
	}

    private void getFileDataTest(String ip, int port, int file_id, long start, long end) throws IOException{

        ClientConnection clientConnection;

        ClientConnection clientConnection1 = new ClientConnection(ip,port);
        clientConnection1.setSpeed(1.0);
        ClientConnection clientConnection2 = new ClientConnection(ip,5001);
        clientConnection2.setSpeed(3.0);

        long maxReceivedByte=-1;
        long partStart=1;
        long partEnd=ResponseType.MAX_RESPONSE_SIZE;
        while(maxReceivedByte<end){

            if (clientConnection1.getSpeed()> clientConnection2.getSpeed())
                clientConnection=clientConnection1;
            else clientConnection = clientConnection2;

            FileDataResponseType response = clientConnection.getFilePartFromSocket(file_id,partStart,partEnd);

            loggerManager.getInstance(this.getClass()).debug(response.toString());
            loggerManager.getInstance(this.getClass()).debug(clientConnection.getPort());
            if (response.getResponseType()!=RESPONSE_TYPES.GET_FILE_DATA_SUCCESS){
                break;
            }
            if (response.getEnd_byte()>maxReceivedByte){
                maxReceivedByte=response.getEnd_byte();
            }
            partStart=partEnd;
            partEnd+=ResponseType.MAX_RESPONSE_SIZE;
            if(partEnd>5000)         clientConnection1.setSpeed(5.0);
        }
    }

	public static void main(String[] args) throws Exception{
		if (args.length<1){
			throw new IllegalArgumentException("ip:port is mandatory");
		}
		String[] adr1=args[0].split(":");
		String ip1=adr1[0];
		int port1=Integer.valueOf(adr1[1]);
		dummyClient inst=new dummyClient();
	/*	inst.sendInvalidRequest(ip1,port1);
		inst.getFileList(ip1,port1);
		inst.getFileSize(ip1,port1,0);
		long size=inst.getFileSize(ip1,port1,1);
		inst.getFileData(ip1,port1,0,0,1);
		inst.getFileData(ip1,port1,1,30,20);*/
        long size=inst.getFileSize(ip1,port1,1);
		//inst.getFileData(ip1,port1,1,1,size);
        inst.getFileDataTest(ip1,port1,1,1,size);
	}
}
