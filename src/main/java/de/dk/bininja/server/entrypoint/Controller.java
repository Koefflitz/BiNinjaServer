package de.dk.bininja.server.entrypoint;

import java.util.Objects;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller implements DaemonController {
   private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

   private final Daemon daemon;

   public Controller(Daemon daemon) {
      this.daemon = Objects.requireNonNull(daemon);
   }

   @Override
   public void shutdown() throws IllegalStateException {
      try {
         daemon.stop();
      } catch (Exception e) {
         LOGGER.error("Could not stop the daemon", e);
         throw new IllegalStateException(e);
      }

      daemon.destroy();
   }

   @Override
   public void reload() throws IllegalStateException {
      try {
         daemon.stop();
      } catch (Exception e) {
         LOGGER.error("Could not stop the daemon", e);
         throw new IllegalStateException(e);
      }
      try {
         daemon.start();
      } catch (Exception e) {
         LOGGER.error("Could not start the daemon", e);
         throw new IllegalStateException(e);
      }
   }

   @Override
   public void fail() throws IllegalStateException {
      shutdown();
   }

   @Override
   public void fail(String message) throws IllegalStateException {
      LOGGER.error(message);
      shutdown();
   }

   @Override
   public void fail(Exception exception) throws IllegalStateException {
      LOGGER.error(exception.getMessage(), exception);
      shutdown();
   }

   @Override
   public void fail(String message, Exception exception) throws IllegalStateException {
      LOGGER.error(message, exception);
      shutdown();
   }
}
