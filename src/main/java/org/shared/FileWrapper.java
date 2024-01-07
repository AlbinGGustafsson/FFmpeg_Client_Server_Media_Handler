package org.shared;

import java.io.Serializable;

/**
 * Klass som används när filer skickas via servern och klienten.
 */
public class FileWrapper implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String command;

    private String outputFileName;
    private byte[] fileBytes;


    /**
     * Konstruktor för FileWrapper-klassen.
     *
     * @param fileName      Filnamnet som ska överföras.
     * @param command       ffmpeg kommandot.
     * @param fileBytes     Byte-representationen av filen.
     * @param outputFileName output-namnet för den bearbetade filen.
     */
    public FileWrapper(String fileName, String command, byte[] fileBytes, String outputFileName) {
        this.fileName = fileName;
        this.command = command;
        this.fileBytes = fileBytes;
        this.outputFileName = outputFileName;
    }

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
}