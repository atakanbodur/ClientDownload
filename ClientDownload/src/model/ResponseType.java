package model;

import java.security.InvalidParameterException;
import java.util.Arrays;

public class ResponseType {
	
	public static final int HEADER_SIZE=10;
	public static final int MAX_DATA_SIZE=1000;
	public static final int MAX_RESPONSE_SIZE=HEADER_SIZE+MAX_DATA_SIZE;
	
	public class RESPONSE_TYPES{
		public static final int GET_FILE_LIST_SUCCESS=1;
		public static final int GET_FILE_SIZE_SUCCESS=2;
		public static final int GET_FILE_DATA_SUCCESS=3;
		
		public static final int INVALID_REQUEST_TYPE=100;
		public static final int INVALID_FILE_ID=101;
		public static final int INVALID_START_OR_END_BYTE=102;
	}
	
	//1 byte
	private int responseType;
	//1 byte
	private int file_id;
	//4 byte
	protected long start_byte;
	//4 byte
	protected long end_byte;
	protected byte[] data;
	
	public ResponseType(int responseType, int file_id, long start_byte, long end_byte,byte[] data){
		this.responseType=responseType;
		this.file_id=file_id;
		this.start_byte=start_byte;
		this.end_byte=end_byte;
		this.data=data;
	}
	
	public ResponseType(byte[] rawData) {
		//request_type:1 byte|file_id:1 byte|start_byte 4 bytes|end_byte 4 bytes
		if (rawData.length<10){
			throw new InvalidParameterException("Invalid Header");
		}
		responseType=(int)rawData[0] & 0xFF;
		file_id=(int)rawData[1] & 0xFF;
		start_byte=0;
		for(int i=2;i<6;i++){
			start_byte=(start_byte << 8)|((int)rawData[i] & 0xFF);
		}
		end_byte=0;
		for(int i=6;i<10;i++){
			end_byte=(end_byte << 8)|((int)rawData[i] & 0xFF);
		}
		int dataLength=(int)(end_byte-start_byte+1);
		if (responseType==RESPONSE_TYPES.GET_FILE_LIST_SUCCESS){
			data=Arrays.copyOfRange(rawData, 10, rawData.length);
		}
		else if (responseType==RESPONSE_TYPES.GET_FILE_SIZE_SUCCESS){
			data=Arrays.copyOfRange(rawData, 10, 14);
		}
		else if (responseType==RESPONSE_TYPES.GET_FILE_DATA_SUCCESS){
			if ((dataLength+10)>rawData.length){
				throw new InvalidParameterException("Data length does not match with the header");
			}
			data=Arrays.copyOfRange(rawData, 10, 10+dataLength);
		}
	}
	
	public byte[] toByteArray(){
		int dataLength=0;
		if (data!=null){
			dataLength=data.length;
		}
		byte[] rawData=new byte[10+dataLength];
		rawData[0]=(byte)(responseType & 0xFF);
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
	
	public int getResponseType() {
		return responseType;
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
		StringBuffer resultBuf=new StringBuffer("\nresponse_type:"+responseType);
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
		ResponseType a=new ResponseType(raw);
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
