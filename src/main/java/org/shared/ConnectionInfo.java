package org.shared;

import java.util.Objects;

/**
 * Klass som representerar information om en klientanslutning, inklusive IP-adress och filnamn.
 */
public class ConnectionInfo {

    private String ip;
    private String fileName;

    /**
     * Konstruktor för ConnectionInfo-klassen.
     *
     * @param ip       IP-adressen för klienten.
     * @param fileName Filnamnet associerat med anslutningen.
     */
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

    /**
     * Metod för att jämföra två ConnectionInfo-objekt och avgöra om de är lika.
     * Lika om ip och filename är lika.
     *
     * @param o Det objekt som ska jämföras med detta objekt.
     * @return true om objekten är lika, annars false.
     */
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
