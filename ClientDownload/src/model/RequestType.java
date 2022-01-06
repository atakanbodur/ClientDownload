package model;

import java.security.InvalidParameterException;
import java.util.Arrays;

public class RequestType {
	
	public class REQUEST_TYPES{
		public static final int GET_FILE_LIST=1;
		public static final int GET_FILE_SIZE=2;
		public static final int GET_FILE_DATA=3;
	}
	
	//1 byte
	private int requestType;
	//1 byte
	private int file_id;
	//4 byte
	private long start_byte;
	//4 byte
	private long end_byte;
	byte[] data;
	
	public RequestType(int requestType, int file_id, long start_byte, long end_byte, byte[] data){
		this.requestType=requestType;
		this.file_id=file_id;
		this.start_byte=start_byte;
		this.end_byte=end_byte;
		this.data=data;
	}
	
	public RequestType(byte[] rawData) {
		//request_type:1 byte|file_id:1 byte|start_byte 4 bytes|end_byte 4 bytes
		if (rawData.length<10){
			throw new InvalidParameterException("Invalid Header");
		}
		requestType=(int)rawData[0] & 0xFF;
		file_id=(int)rawData[1] & 0xFF;
		start_byte=0;
		for(int i=2;i<6;i++){
			start_byte=(start_byte << 8)|((int)rawData[i] & 0xFF);
		}
		end_byte=0;
		for(int i=6;i<10;i++){
			end_byte=(end_byte << 8)|((int)rawData[i] & 0xFF);
		}
		data=Arrays.copyOfRange(rawData, 10, rawData.length);
	}
	
	public byte[] toByteArray(){
		int dataLength=0;
		if (data!=null){
			dataLength=data.length;
		}
		byte[] rawData=new byte[10+dataLength];
		rawData[0]=(byte)(requestType & 0xFF);
		rawData[1]=(byte)(file_id & 0xFF);
		long tmp=start_byte;
		for(int i=5;i>1;i--){
			rawData[i]=(byte)(tmp & 0xFF);
			tmp>>=8;
		}
		tmp=end_byte;
		for(int i=9;i>5;i--){
			rawData[i]=(byte)(tmp & 0xFF);
			tmp>>=8;
		}
		if (data!=null){
			System.arraycopy(data, 0, rawData, 10, dataLength);
		}
		return rawData;
	}
	
	public int getRequestType() {
		return requestType;
	}

	public int getFile_id() {
		return file_id;
	}

	public long getStart_byte() {
		return start_byte;
	}

	public long getEnd_byte() {
		return end_byte;
	}

	public byte[] getData() {
		return data;
	}
	
	@Override
	public String toString() {
		StringBuffer resultBuf=new StringBuffer("\nrequest_type:"+requestType);
		resultBuf.append("\nfile_id:"+file_id);
		resultBuf.append("\nstart_byte:"+start_byte);
		resultBuf.append("\nend_byte:"+end_byte);
		resultBuf.append("\ndata:");
		if (data!=null){
			for(byte b:data){
				resultBuf.append(b);
			}
		}
		return resultBuf.toString();
	}

	public static void main(String[] args) {
		byte[] raw=new byte[]{0x1,0x2, 0x3, 0x4, 0x5, 0x6,0x3, 0x4, 0x5, 0x7, (byte) 0xAB, 0x01};
		RequestType a=new RequestType(raw);
		byte[] raw2=a.toByteArray();
		for (byte b:raw){
			System.out.print(b+" ");
		}
		System.out.println();
		for (byte b:raw2){
			System.out.print(b+" ");
		}
		System.out.println();
	}
}
