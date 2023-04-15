package model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class File {
    private int size;
    private List<FilePart> data;
    public byte[] rawData;

    public File(int sizeOfData) {
        this.size = sizeOfData;
        this.data = new ArrayList<>();
    }

    public List<FilePart> getData() {
        return data;
    }

    public void setData(List<FilePart> data) {
        this.data = data;
    }

    public void addFilePart(int index ,FilePart filePart){
        data.add(index, filePart);
    }

    public void initRawData(){
        rawData = new byte[size];
        int index = 0;
        for (int i=0; i<data.size(); i++){
                for (int j=0; j<data.get(i).getResponseType().getData().length; j++){
                    //System.out.println("Data in raw data: index= " + index + " / data= " + data.get(i).getResponseType().getData()[j] + " data length = " + data.get(i).getResponseType().getData().length );
                    rawData[index] = data.get(i).getResponseType().getData()[j];
                    index++;
                }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String generateMd5Hash() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        //byte[] theMD5digest = md.digest("test file1".getBytes(StandardCharsets.UTF_8));
        byte[] theMD5digest = md.digest(rawData);
        return bytesToHex(theMD5digest);
    }



}
