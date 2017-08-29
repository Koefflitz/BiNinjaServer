package de.dk.bininja.server.net;

import java.io.IOException;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface Resource {
   public void close(long timeout) throws InterruptedException, IOException;
   public void destroy(long timeout) throws InterruptedException, IOException;
}
