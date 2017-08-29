package de.dk.bininja.server.net;

import static de.dk.bininja.net.DownloadState.CANCELLED;
import static de.dk.bininja.net.DownloadState.COMPLETE;
import static de.dk.bininja.net.DownloadState.RUNNING;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.net.Download;
import de.dk.bininja.net.DownloadState;
import de.dk.bininja.net.packet.download.DownloadCancelPacket;
import de.dk.bininja.net.packet.download.DownloadCompletePacket;
import de.dk.bininja.net.packet.download.DownloadDataPacket;
import de.dk.bininja.net.packet.download.DownloadHeaderPacket;
import de.dk.bininja.net.packet.download.DownloadPacket;
import de.dk.bininja.net.packet.download.DownloadReadyPacket;
import de.dk.bininja.net.packet.download.DownloadRequestPacket;
import de.dk.util.channel.Channel;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class ServerDownload extends Download {
   private static final Logger LOGGER = LoggerFactory.getLogger(ServerDownload.class);

   private final Channel<DownloadPacket> channel;
   private URLConnection connection;

   private int bufferSize = Base64Connection.DEFAULT_BUFFER_SIZE;

   public ServerDownload(Channel<DownloadPacket> channel) {
      this.channel = channel;
   }

   @Override
   public void run() {
      if (connection == null) {
         LOGGER.error("Could not begin to download because "
                      + "the url-connection was not established.");
         return;
      }

      InputStream in;
      try {
         in = connection.getInputStream();
      } catch (IOException e) {
         LOGGER.error("Could not get an inputstream from URLConnection " + connection.getURL(), e);
         cancel(e.getMessage());
         return;
      }

      setState(RUNNING);
      LOGGER.debug("Starting the Download from " + connection.getURL());
      byte[] buffer = new byte[bufferSize];
      while (getDownloadState() == RUNNING) {
         try {
            int readBytes = in.read(buffer);
            if (readBytes == -1) {
               finished();
               continue;
            }

            received(readBytes);
            DownloadDataPacket packet = new DownloadDataPacket(Arrays.copyOf(buffer, readBytes));
            channel.send(packet);
            written(readBytes);
         } catch (IOException e) {
            if (getDownloadState() == RUNNING || getDownloadState() == DownloadState.LOADING_FINISHED) {
               LOGGER.error("Error while sending data to client", e);
               cancel(e.getMessage());
            }
         }
      }
   }

   private void finished() {
      LOGGER.info("Download complete");
      try {
         channel.send(new DownloadCompletePacket());
      } catch (IOException e) {
         LOGGER.warn("Could not send finish packet.", e);
      }
      setState(COMPLETE);
   }

   @Override
   protected void request(DownloadRequestPacket packet) {
      LOGGER.debug("DownloadRequest received " + packet);
      URL url = packet.getUrl();
      LOGGER.debug("Trying to get a connection to " + url);
      try {
         this.connection = url.openConnection();
         connection.connect();
         LOGGER.debug("URL-Connection to " + url + " successfully established");
      } catch (IOException e) {
         LOGGER.debug("Could not establish connection to " + url, e);
         cancel(e.getMessage());
         return;
      }
      LOGGER.debug("Requesting meta information about the download");
      this.length = connection.getContentLengthLong();

      String filename = null;
      try {
         filename = getFilename(connection);
      } catch (NoSuchFieldException e) {
         LOGGER.debug("Could not read filename from url", e);
      }
      LOGGER.debug("Meta information about the download received. Length of the download: " + getLength() + " bytes");
      LOGGER.debug("Telling the client the download metadata by sending a DownloadHeaderPacket");
      try {
         channel.send(new DownloadHeaderPacket(getLength(), filename));
      } catch (IOException e) {
         LOGGER.warn("Could not send Downloadheader to client", e);
         terminate();
      }
   }

   @Override
   protected void cancel(DownloadCancelPacket packet) {
      String msg = packet.getMsg() != null ? packet.getMsg() : "";
      LOGGER.info("The client wants to cancel the download: " + msg);
      terminate();
   }

   @Override
   protected void header(DownloadHeaderPacket packet) {
      LOGGER.warn("A DownloadHeaderPacket was received by " + toString() + ". The packet was " + packet);
   }

   @Override
   protected void ready(DownloadReadyPacket packet) {
      LOGGER.debug("Client is ready: " + packet);
      LOGGER.debug("Initialising the download.");
      start();
   }

   @Override
   protected void data(DownloadDataPacket packet) {
      LOGGER.warn("A DownloadDataPacket was received by " + toString() + ". The packet was " + packet);
   }

   @Override
   protected void finish() {
      LOGGER.warn("A DownloadCompletePacket was received by " + toString());
   }

   public void cancel(String msg, long timeout) throws InterruptedException {
      try {
         channel.send(new DownloadCancelPacket(msg));
      } catch (IOException e) {
         LOGGER.warn("Could not send Cancelpacket to client.", e);
      }
      terminate(timeout);
   }

   private void cancel(String msg) {
      try {
         cancel(msg, 0);
      } catch (InterruptedException e) {
         LOGGER.warn("Interrupted while waiting for " + this + " to cancel.", e);
      }
   }

   private void terminate() {
      try {
         terminate(0);
      } catch (InterruptedException e) {
         LOGGER.warn("Interrupted while waiting for ServerDownload " + this + " to terminate.", e);
      }
   }

   public void terminate(long timeout) throws InterruptedException {
      setState(CANCELLED);
      interrupt();
      if (Thread.currentThread() != this)
         join(timeout);
   }

   @Override
   public String toString() {
      return "{ServerDownload from " + connection.getURL() + "}";
   }
}
