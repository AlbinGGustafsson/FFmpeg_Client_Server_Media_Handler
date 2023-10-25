import java.io.Serializable;

public class FileWrapper implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String command;
    private byte[] fileBytes;

    public FileWrapper(String fileName, String command, byte[] fileBytes) {
        this.fileName = fileName;
        this.command = command;
        this.fileBytes = fileBytes;
    }

    // Getters and setters here
    public String getFileName() {
        return fileName;
    }

    public String getCommand() {
        return command;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }
}