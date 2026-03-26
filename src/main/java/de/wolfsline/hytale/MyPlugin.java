package de.wolfsline.hytale;


import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import de.wolfsline.hytale.command.HelloCommand;
import de.wolfsline.hytale.command.InspectorCommand;
import de.wolfsline.hytale.command.TestCommand;
import de.wolfsline.hytale.listener.SoilAddedListener;
import de.wolfsline.hytale.listener.WakeUpHealSystem;

import javax.annotation.Nonnull;

public class MyPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public MyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCommandRegistry().registerCommand(new HelloCommand("hello", "An example command", false));
        this.getCommandRegistry().registerCommand(new TestCommand("test", "A test command", false));
        this.getCommandRegistry().registerCommand(new InspectorCommand("inspect", "Display Block data", false));
        this.getEntityStoreRegistry().registerSystem(new WakeUpHealSystem());
        //this.getChunkStoreRegistry().registerSystem(new FarmlandDrySystem());
        this.getChunkStoreRegistry().registerSystem(new SoilAddedListener());
    }

    @Override protected void start() { LOGGER.atInfo().log("Started!"); }
    @Override protected void shutdown() { LOGGER.atInfo().log("Stopped!"); }

}
