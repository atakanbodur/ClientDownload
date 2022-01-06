package model;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Arrays;

public class FileDescriptor {
	private int file_id;
	private String file_name;
	
	public FileDescriptor(int file_id, String file_name) {
		this.file_id=file_id;
		this.file_name=file_name;
	}
	
	public int getFile_id() {
		return file_id;
	}
	
	public void setFile_id(int file_id) {
		this.file_id = file_id;
	}
	
	public String getFile_name() {
		return file_name;
	}
	
	public void setFile_name(String file_name) {
		this.file_name = file_name;
	}
	
	public byte[] toByte(){
		byte[] fileName=getFile_name().getBytes();
		byte[] rawData=new byte[fileName.length+2];
		rawData[0]=(byte)(file_id & 0xFF);
		System.arraycopy(fileName, 0, rawData, 1, fileName.length);
		rawData[rawData.length-1]='\0';
		return rawData;
	}
	
	@Override
	public String toString() {
		return file_id+"-"+file_name;
	}
	
}
