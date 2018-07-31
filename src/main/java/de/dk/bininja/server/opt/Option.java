package de.dk.bininja.server.opt;

import java.util.Objects;

import de.dk.opt.ArgumentParserBuilder;
import de.dk.opt.ExpectedOption;
import de.dk.opt.OptionBuilder;

public enum Option {
   PORT('p', "port", "The port to look for connections.", true, false);

   private final char key;
   private final String longKey;
   private final String description;

   private final boolean expectsValue;
   private final boolean mandatory;

   private Option(char key, String longKey, String description, boolean expectsValue, boolean mandatory) {
      this.key = key;
      this.longKey = longKey;
      this.description = Objects.requireNonNull(description);
      this.expectsValue = expectsValue;
      this.mandatory = mandatory;
   }

   private Option(char key, String longKey, String description) {
      this(key, longKey, description, false, false);
   }

   private Option(char key, String longKey, String description, boolean expectsValue) {
      this(key, longKey, description, expectsValue, false);
   }

   private Option(String longKey, String description) {
      this(ExpectedOption.NO_KEY, longKey, description, false, false);
   }

   private Option(String longKey, String description, boolean expectsValue) {
      this(ExpectedOption.NO_KEY, longKey, description, expectsValue, false);
   }

   private Option(String longKey, String description, boolean expectsValue, boolean mandatory) {
      this(ExpectedOption.NO_KEY, longKey, description, expectsValue, mandatory);
   }

   public ArgumentParserBuilder build(ArgumentParserBuilder builder) {
      OptionBuilder oBuilder;
      if (key != ExpectedOption.NO_KEY) {
         oBuilder = builder.buildOption(key)
                           .setLongKey(longKey);
      } else {
         oBuilder = builder.buildOption(longKey);
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
