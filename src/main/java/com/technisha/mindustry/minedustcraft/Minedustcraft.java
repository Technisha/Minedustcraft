package com.technisha.mindustry.minedustcraft;

import arc.Core;
import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.extras.optifine.OptifineSupport;

public class Minedustcraft extends Plugin {
    @Override
    public void init(){
        Core.settings.put("totalPlayers", 0);
        MinecraftServer minecraftServer = MinecraftServer.init();
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        OptifineSupport.enable();
        PlayerInit.init();
        // MojangAuth.init();

        globalEventHandler.addEventCallback(net.minestom.server.event.player.PlayerChatEvent.class, e -> {
            Groups.player.forEach(player -> {
                try {
                    player.sendMessage("[#b47144][[#ffffff]" + e.getPlayer().getUsername() + "|Minecraft[#be6647]][#ba5f3f]: [#ffffff]" + e.getMessage()); // Doesn't send any message at all, probably due to the custom created player units
                } catch (java.lang.NullPointerException exception) {}
            });
        });

        Events.on(EventType.PlayerChatEvent.class, e -> {
            if (e.player == null) {
                return;
            }
            Audiences.players().sendMessage(Component.text("<"+e.player.name()+"|Mindustry> "+e.message));
        });

        Events.on(EventType.GameOverEvent.class, e -> {
            Audiences.players().sendMessage(Component.text("Game over.", TextColor.color(0xff1111)));
        });

        Events.on(EventType.PlayerJoin.class, e -> {
            if (e.player == null) {
                return;
            }
            Audiences.players().sendMessage(Component.text(e.player.name()+" has connected.",TextColor.color(0xf3cc7a)));
        });

        Events.on(EventType.PlayerLeave.class, e -> {
            if (e.player == null) {
                return;
            }
            Audiences.players().sendMessage(Component.text(e.player.name()+" has disconnected.", TextColor.color(0xf3cc7a)));
        });

        minecraftServer.start("0.0.0.0", 25565, PlayerInit.getResponseDataConsumer());
        Runtime.getRuntime().addShutdownHook(new Thread(MinecraftServer::stopCleanly));
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
