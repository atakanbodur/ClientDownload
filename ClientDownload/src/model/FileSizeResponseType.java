package model;

public class FileSizeResponseType extends ResponseType {
	
	long fileSize=-1;
	
	public FileSizeResponseType(int responseType, int file_id, long start_byte, long end_byte,long file_size) {
		super(responseType, file_id, start_byte, end_byte, null);
		this.fileSize=file_size;
		setFileSizeToData();
	}
	
	public FileSizeResponseType(byte[] rawData){
		super(rawData);
		setFileSize();
	}
	
	private void setFileSize(){
		if (this.getResponseType()==ResponseType.RESPONSE_TYPES.GET_FILE_SIZE_SUCCESS){
			byte[] data=this.getData();
			fileSize=0;
			for(int i=0;i<4;i++){
				fileSize=(fileSize << 8)|((int)data[i] & 0xFF);
			}
		}
	}
	
	private void setFileSizeToData(){
		this.data=new byte[4];
		long tmp=fileSize;
		for(int i=3;i>=0;i--){
			this.data[i]=(byte)(tmp & 0xFF);
			tmp>>=8;
		}
	}
	
	public long getFileSize(){
		return fileSize;
	}
	
	@Override
	public String toString() {
		StringBuffer resultBuf=new StringBuffer("\nresponse_type:"+this.getResponseType());
		resultBuf.append("\nfile_id:"+this.getFile_id());
		resultBuf.append("\nstart_byte:"+this.getStart_byte());
		resultBuf.append("\nend_byte:"+this.getEnd_byte());
		resultBuf.append("\ndata:");
		if (this.getResponseType()==ResponseType.RESPONSE_TYPES.GET_FILE_SIZE_SUCCESS){
			resultBuf.append(fileSize);
		}
		return resultBuf.toString();
	}
}
