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
import de.dk.bininja.server.opt.ParsedArguments;
import de.dk.util.opt.ex.ArgumentParseException;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
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
      LOGGER.debug("BiNinjaServer initializing.");

      LOGGER.debug("parsing arguments.");
      ParsedArguments args;
      try {
         args = ParsedArguments.parse(context.getArguments());
      } catch (ArgumentParseException e) {
         e.printStackTrace();
         throw new DaemonInitException("Error parsing command line arguments", e);
      }
      LOGGER.debug("Arguments successfully parsed.");
      int port = args.isPortSet() ? args.getPort() : Base64Connection.PORT;

      LOGGER.info("Initialising server socket on port " + port + ".");
      try {
         this.serverSocket = new ServerSocket(port);
         serverSocket.setSoTimeout(SERVERSOCKET_TIMEOUT);
      } catch (IOException e) {
         throw new DaemonInitException("Could not initiate the server", e);
      }
      LOGGER.debug("Serversocket initialized.");
      if (args.getSecurityArgs() != null)
         this.server = new Server(this, args.getSecurityArgs().getKeys());
      else
         this.server = new Server(this, null);

      this.thread = new Thread(this);
      LOGGER.debug("BiNinjaServer initialized.");
   }

   @Override
   public void start() {
      LOGGER.debug("BiNinjaServer starting up...");
      thread.start();
   }

   @Override
   public void run() {
      running = true;
      LOGGER.info("Waiting for downloadClients to connect...");
      while (running) {
         try {
            server.newConnection(serverSocket.accept());
         } catch (SocketTimeoutException e) {

         } catch (IOException e) {
            if (!serverSocket.isClosed())
               LOGGER.error(e.getMessage(), e);
         }
      }
      LOGGER.debug("Serversocket stopped.");
   }

   @Override
   public void shutdown() {
      destroy();
   }

   @Override
   public void stop() throws InterruptedException, TimeoutException {
      LOGGER.info("Stopping BiNinjaServer");
      if (serverSocket == null || serverSocket.isClosed())
         return;

      LOGGER.debug("Stopping the server socket");
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
         } catch (InterruptedException | IOException e) {
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
      if (serverSocket != null && !serverSocket.isClosed()) {
         try {
            serverSocket.close();
         } catch (IOException e) {
            LOGGER.warn("Error closing the server socket.");
         }
      }
      LOGGER.debug("BiNinjaServer out.");
   }
}
