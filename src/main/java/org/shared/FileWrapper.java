package org.shared;

import java.io.Serializable;

public class FileWrapper implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String command;

    private String outputFileName;
    private byte[] fileBytes;

    private String password;

    public FileWrapper(String fileName, String command, byte[] fileBytes, String outputFileName, String password) {
        this.fileName = fileName;
        this.command = command;
        this.fileBytes = fileBytes;
        this.outputFileName = outputFileName;
        this.password = password;
    }

    // Getters and setters here
    public String getFileName() {
        return fileName;
    }

    public String getOutputFileName(){
        return outputFileName;
    }

    public String getCommand() {
        return command;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public String getPassword() {
        return password;
    }
}