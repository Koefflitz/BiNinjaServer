package de.dk.bininja.server.controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.bininja.net.Base64Connection;
import de.dk.bininja.server.net.Server;
import de.dk.bininja.server.net.ServerController;

public class MasterControlProgram implements Daemon, ServerController, Runnable {
   private static final Logger LOGGER = LoggerFactory.getLogger(MasterControlProgram.class);

   private static final int SERVERSOCKET_TIMEOUT = 1024;

   private ServerSocket serverSocket;

   private boolean running;
   private Thread thread;

   private Server server;

   public MasterControlProgram() {

   }

   @Override
   public void init(DaemonContext context) throws DaemonInitException {
      LOGGER.debug("BiNinjaServer initializing...");

      LOGGER.debug("parsing arguments");
      String[] args = context.getArguments();
      int port;
      if (args != null && args.length > 0) {
         try {
            port = Integer.parseInt(args[0]);
            if (port < 0 || port > 0xffff)
               throw new NumberFormatException("Port number " + port + " out of range.");
         } catch (NumberFormatException e) {
            String msg = "The first argument was not a valid port: " + args[0];
            LOGGER.error(msg, e);
            throw new DaemonInitException(msg, e);
         }
      } else {
         port = Base64Connection.PORT;
      }

      LOGGER.info("Initialising server socket on port " + port);
      try {
         this.serverSocket = new ServerSocket(port);
         serverSocket.setSoTimeout(SERVERSOCKET_TIMEOUT);
      } catch (IOException e) {
         throw new DaemonInitException("Could not initiate the server", e);
      }
      this.server = new Server(this);
      this.thread = new Thread(this);
   }

   @Override
   public void start() {
      LOGGER.debug("BiNinjaServer starting up...");
      thread.start();
   }

   @Override
   public void run() {
      LOGGER.info("Waiting for downloadClients to connect...");
      running = true;
      while (running) {
         try {
            server.newConnection(serverSocket.accept());
         } catch (SocketTimeoutException e) {

         } catch (IOException e) {
            if (!serverSocket.isClosed())
               LOGGER.error(e.getMessage(), e);
         }
      }
   }

   @Override
   public void shutdown() {
      destroy();
   }

   @Override
   public void stop() throws InterruptedException, TimeoutException {
      LOGGER.info("Stopping BiNinja server");
      if (serverSocket == null || serverSocket.isClosed())
         return;

      LOGGER.debug("Closing the server socket");
      running = false;

      if (thread != null && thread != Thread.currentThread())
         thread.join();
      LOGGER.info("BiNinja server stopped.");
   }

   @Override
   public void destroy() {
      LOGGER.info("Shutting down the BiNinja server application");
      running = false;

      if (server != null) {
         try {
            server.destroy();
         } catch (InterruptedException e) {
            LOGGER.warn("An exception occured while terminating the server", e);
         }
         server = null;
      }

      if (thread != null && thread != Thread.currentThread()) {
         try {
            thread.join();
         } catch (InterruptedException e) {
            LOGGER.warn("An exception occured while terminating", e);
         }
      }
      LOGGER.debug("BiNinjaServer out.");
   }
}