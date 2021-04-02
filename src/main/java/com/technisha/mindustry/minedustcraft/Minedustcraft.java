package com.technisha.mindustry.minedustcraft;

import arc.Core;
import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extras.optifine.OptifineSupport;

public class Minedustcraft extends Plugin {
    @Override
    public void init(){
        Core.settings.put("totalPlayers", 0);
        MinecraftServer minecraftServer = MinecraftServer.init();
        PlayerInit.init();
        OptifineSupport.enable();
        Events.on(EventType.BuildSelectEvent.class, event -> {
            Player player = event.builder.getPlayer();
        });
        minecraftServer.start("0.0.0.0", 25565, PlayerInit.getResponseDataConsumer());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("hello", "Says hi.", args -> {
            Log.info("Hi.");
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("echo", "<text...>", "A simple echo", (args, player) -> {
            player.sendMessage("You said: [accent] " + args[0]);
        });
    }
}
