package client;


import model.*;
import model.ResponseType.RESPONSE_TYPES;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;


public class dummyClient {
    static ClientConnection clientConnection1;
    static ClientConnection clientConnection2;
    static File file;
    static List<ClientConnection> connections = new ArrayList<>();


    class RunDownload implements Runnable {
        ClientConnection clientConnection;
        Boolean timerExpired = false;
        int timesHaveBeenUsed = 0;
        int file_id;
        CompletableFuture<Boolean> downloadFuture = new CompletableFuture<>();
        AtomicBoolean futureCompleted = new AtomicBoolean(false);

        CopyOnWriteArrayList<FilePart> filePartList = new CopyOnWriteArrayList <>();


        @Override
        public void run() {
            while (!filePartList.isEmpty()) {
                FilePart part = getNextFilePart();
                if (part != null) {
                    try {
                        startDownloadFilePart(file_id, part, downloadFuture);
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (futureCompleted.compareAndSet(false, true)) {
                            downloadFuture.complete(false); // Handle exception, complete the future with false
                        }
                    }
                }
            }
        }

        private synchronized FilePart getNextFilePart() {
            if (!filePartList.isEmpty()) {
                FilePart part = filePartList.get(0);
                filePartList.remove(0);
                return part;
            } else {
                return null;
            }
        }


        void startDownloadFilePart(int file_id, FilePart filePart, CompletableFuture<Boolean> downloadFuture) throws IOException {
            Timer timer = new Timer();
            AtomicBoolean futureCompleted = new AtomicBoolean(false);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    timerExpired = true;
                    System.out.println("Timeout occurred in: " + clientConnection.getIp() + ":" + clientConnection.getPort());
                    if (futureCompleted.compareAndSet(false, true)) {
                        downloadFuture.complete(false); // Timer expired, complete the future with false
                    }
                }
            };
            timer.schedule(timerTask, 50);
            getFileDataTest(file_id, filePart, clientConnection);
            timerExpired = false;
            timer.cancel();
            if (futureCompleted.compareAndSet(false, true)) {
                downloadFuture.complete(true); // Download succeeded, complete the future with true
            }
        }
    }

    private void startDownloadFile(String ip, int port, int file_id) throws IOException, NoSuchAlgorithmException, InterruptedException {
        RunDownload runDownloadConn1 = new RunDownload();
        runDownloadConn1.clientConnection = clientConnection1;
        runDownloadConn1.file_id = file_id;

        RunDownload runDownloadConn2 = new RunDownload();
        runDownloadConn2.clientConnection = clientConnection2;
        runDownloadConn2.file_id = file_id;

        Thread conn1Thread = new Thread(runDownloadConn1, "Connection-1-Thread");
        Thread conn2Thread = new Thread(runDownloadConn2, "Connection-2-Thread");


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

        runDownloadConn1.filePartList.add(partList.get(0));
        runDownloadConn2.filePartList.add(partList.get(1));
        partList.remove(0);
        partList.remove(1);

        for (int i = 0; i < partList.size(); i++) {
            conn1Thread.start();
            conn2Thread.start();

            RunDownload decidedConnection = decideRunDownload(runDownloadConn1, runDownloadConn2);

            if (decidedConnection == null) {
                throw new NullPointerException();
            } else {
                decidedConnection.filePartList.add(partList.get(i));
            }
        }

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        futures.add(runDownloadConn1.downloadFuture);
        futures.add(runDownloadConn2.downloadFuture);

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            combinedFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        conn1Thread.join();
        conn2Thread.join();

        resendLostPackets(file_id, file, decideRunDownload(runDownloadConn1, runDownloadConn2));

        sumFileParts(partList, file);
        long end = System.currentTimeMillis();
        System.out.println("MD5 is: " + file.generateMd5Hash());
        System.out.println("Elapsed time: " + (end - start) + "ms");
    }

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

    private void resendLostPackets(int file_id, File file, RunDownload runDownload) throws IOException {
        for (int i=0; i<file.getData().size();i++) {
            if (!file.getData().get(i).isStatus()){
                System.out.println("Packet loss!");
                getFileDataTest(file_id,file.getData().get(i),runDownload.clientConnection);
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

    private RunDownload decideRunDownload(RunDownload runDownload1, RunDownload runDownload2){
        System.out.println("con1 speed: " +clientConnection1.getSpeed() + " con2:speed: "+ clientConnection2.getSpeed());

        //used times
        if (runDownload1.timesHaveBeenUsed-runDownload2.timesHaveBeenUsed > 4){
            System.out.println("Connection " + clientConnection1.getPort() + " used too many times, switching to " + clientConnection2.getPort());
            return runDownload2;
        }
        else if (runDownload2.timesHaveBeenUsed-runDownload1.timesHaveBeenUsed > 4){
            System.out.println("Connection " + clientConnection2.getPort() + " used too many times, switching to " + clientConnection1.getPort());
            return runDownload1;
        }

        //speed
        if(runDownload1.clientConnection.getSpeed()>runDownload2.clientConnection.getSpeed()){
            return runDownload1;
        }
        else if(runDownload2.clientConnection.getSpeed()>runDownload1.clientConnection.getSpeed()){
            return runDownload2;
        }

        //timeout occured
        if(runDownload1.timerExpired) {
            return runDownload2;
        }
        else if(runDownload2.timerExpired) {
            return runDownload1;
        }
        return runDownload1;
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
                    client.startDownloadFile(ip1,port1,choice);
                    break;
                }else{
                    System.out.println("Invalid number.");
                }
            }catch(Exception exception){
                System.out.println(exception.getMessage());
            }
        }
    }
}
