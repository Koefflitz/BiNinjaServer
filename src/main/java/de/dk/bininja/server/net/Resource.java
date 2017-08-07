package de.dk.bininja.server.net;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface Resource {
   public void close(long timeout) throws InterruptedException;
   public void destroy();
}
