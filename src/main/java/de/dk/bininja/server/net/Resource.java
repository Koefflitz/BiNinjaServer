package de.dk.bininja.server.net;

public interface Resource {
   public void close(long timeout) throws InterruptedException;
   public void destroy();
}