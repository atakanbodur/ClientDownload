package model;

public class FileDataResponseType extends ResponseType {

	public FileDataResponseType(int responseType, int file_id, long start_byte, long end_byte,byte[] data) {
		super(responseType, file_id, start_byte, end_byte, data);
	}
	
	public FileDataResponseType(byte[] rawData){
		super(rawData);
	}
	
	public void setStartByte(long start_byte){
		this.start_byte=start_byte;
	}
	
	public void setEndByte(long end_byte){
		this.end_byte=end_byte;
	}
	
	public void setData(byte[] data){
		this.data=data;
	}
	
	@Override
	public String toString() {
		StringBuffer resultBuf=new StringBuffer("\nresponse_type:"+this.getResponseType());
		resultBuf.append("\nfile_id:"+this.getFile_id());
		resultBuf.append("\nstart_byte:"+this.getStart_byte());
		resultBuf.append("\nend_byte:"+this.getEnd_byte());
		return resultBuf.toString();
	}
}
