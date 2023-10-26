package org.shared;

import java.util.Objects;

public class ConnectionInfo {

    private String ip;
    private String fileName;

    public ConnectionInfo(String ip, String fileName) {
        this.ip = ip;
        this.fileName = fileName;
    }

    public String getIp() {
        return ip;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return ip + " " + fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionInfo that = (ConnectionInfo) o;
        return Objects.equals(ip, that.ip) && Objects.equals(fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, fileName);
    }
}
