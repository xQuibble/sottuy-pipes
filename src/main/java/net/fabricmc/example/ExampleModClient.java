package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.example.blocks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ExampleModClient implements ClientModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("sottuy-pipes");
    @Override
    public void onInitializeClient() {
        BasicItemPipe.RegisterClient();
    }
}
