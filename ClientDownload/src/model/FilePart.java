package model;

public class FilePart {
    private ResponseType responseType;
    private long startByte;
    private long endByte;
    private boolean status;


    public FilePart() {
        responseType = null;
        startByte = -1;
        endByte = -1;
        status = false;
    }

    public void assignByteValues(long start, long end){
        startByte = start;
        endByte = end;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public long getStartByte() {
        return startByte;
    }

    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    public long getEndByte() {
        return endByte;
    }

    public void setEndByte(long endByte) {
        this.endByte = endByte;
    }

}
