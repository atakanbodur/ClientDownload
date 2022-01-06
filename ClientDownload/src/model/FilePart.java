package model;

public class FilePart {
    ResponseType responseType;
    private long startByte;
    private long endByte;


    public FilePart() {
        startByte = -1;
        endByte = -1;
    }

    public void assignByteValues(long start, long end){
        startByte = start;
        endByte = end;
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
