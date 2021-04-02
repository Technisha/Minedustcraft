package com.technisha.mindustry.minedustcraft;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import net.minestom.server.MinecraftServer;

public class Minedustcraft extends Plugin {
    @Override
    public void init(){
        MinecraftServer minecraftServer = MinecraftServer.init();
        Events.on(EventType.BuildSelectEvent.class, event -> {
            Player player = event.builder.getPlayer();
            Call.sendMessage("[scarlet]ALERT![] " + player.name + " has begun building at " + event.tile.x + ", " + event.tile.y);
        });
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
