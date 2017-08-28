package de.dk.bininja.server.opt;

import java.io.IOException;
import java.util.Objects;

import de.dk.bininja.InvalidArgumentException;
import de.dk.bininja.opt.ParsedSecurityArguments;
import de.dk.util.opt.ArgumentModel;
import de.dk.util.opt.ArgumentParser;
import de.dk.util.opt.ArgumentParserBuilder;
import de.dk.util.opt.ExpectedOption;
import de.dk.util.opt.OptionBuilder;
import de.dk.util.opt.ex.ArgumentParseException;

public class ParsedArguments {
   private ParsedSecurityArguments securityArgs;
   private int port = -1;

   public ParsedArguments() {

   }

   public static ParsedArguments parse(String... args) throws ArgumentParseException {
      ArgumentParserBuilder builder = ArgumentParserBuilder.begin();
      Option.PORT.build(builder);
      ParsedSecurityArguments.build(builder);

      ArgumentParser parser = builder.buildAndGet();
      ArgumentModel result = parser.parseArguments(args);

      ParsedArguments parsedArgs = new ParsedArguments();
      if (result.isOptionPresent(Option.PORT.getKey())) {
         String portString = result.getOptionValue(Option.PORT.getKey());
         int port;
         try {
            port = Integer.parseInt(portString);
         } catch (NumberFormatException e) {
            throw new InvalidArgumentException("Invalid port: " + portString, e);
         }
         parsedArgs.setPort(port);
      }

      if (result.isCommandPresent(ParsedSecurityArguments.NAME)) {
         ArgumentModel securityResult = result.getCommandValue(ParsedSecurityArguments.NAME);
         try {
            ParsedSecurityArguments secArgs = ParsedSecurityArguments.parse(securityResult);
            parsedArgs.setSecurityArgs(secArgs);
         } catch (IOException e) {
            throw new ArgumentParseException("Could not parse securityArgs", e);
         }
      }
      return parsedArgs;
   }

   public ParsedSecurityArguments getSecurityArgs() {
      return securityArgs;
   }

   public void setSecurityArgs(ParsedSecurityArguments securityArgs) {
      this.securityArgs = securityArgs;
   }

   public boolean isPortSet() {
      return port != -1;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   private static enum Option {
      PORT('p', "port", "port", "The port to look for connections.", true, false),
      PUBLIC_KEY("public", "publik key", "The public key", true),
      PRIVATE_KEY("private", "private key", "The private key", true);

      private final char key;
      private final String longKey;
      private final String name;
      private final String description;

      private final boolean expectsValue;
      private final boolean mandatory;

      private Option(char key, String longKey, String name, String description, boolean expectsValue, boolean mandatory) {
         this.key = key;
         this.longKey = longKey;
         this.name = Objects.requireNonNull(name);
         this.description = Objects.requireNonNull(description);
         this.expectsValue = expectsValue;
         this.mandatory = mandatory;
      }

      private Option(char key, String name, String description) {
         this(key, null, name, description, false, false);
      }

      private Option(char key, String name, String description, boolean expectsValue) {
         this(key, null, name, description, expectsValue, false);
      }

      private Option(char key, String name, String description, boolean expectsValue, boolean mandatory) {
         this(key, null, name, description, expectsValue, mandatory);
      }

      private Option(String longKey, String name, String description) {
         this(ExpectedOption.NO_KEY, longKey, name, description, false, false);
      }

      private Option(String longKey, String name, String description, boolean expectsValue) {
         this(ExpectedOption.NO_KEY, longKey, name, description, expectsValue, false);
      }

      private Option(String longKey, String name, String description, boolean expectsValue, boolean mandatory) {
         this(ExpectedOption.NO_KEY, longKey, name, description, expectsValue, mandatory);
      }

      public ArgumentParserBuilder build(ArgumentParserBuilder builder) {
         OptionBuilder oBuilder;
         if (key != ExpectedOption.NO_KEY) {
            oBuilder = builder.buildOption(key, name)
                              .setLongKey(longKey);
         } else {
            oBuilder = builder.buildOption(longKey, name);
         }

         return oBuilder.setDescription(description)
                        .setMandatory(mandatory)
                        .setExpectsValue(expectsValue)
                        .build();
      }

      public char getKey() {
         return key;
      }
   }

}
