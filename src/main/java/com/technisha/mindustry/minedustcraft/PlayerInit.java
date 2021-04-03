package com.technisha.mindustry.minedustcraft;

import arc.Core;
import com.technisha.mindustry.minedustcraft.generator.CChunkGenerator;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.benchmark.BenchmarkManager;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.CustomBlock;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.ping.ResponseDataConsumer;
import net.minestom.server.utils.Position;
import net.minestom.server.utils.Vector;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import net.minestom.server.utils.time.TimeUnit;
import net.minestom.server.world.DimensionType;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerInit {

    private static final InstanceContainer instanceContainer;
    private static final Inventory inventory;

    static {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        //StorageLocation storageLocation = MinecraftServer.getStorageManager().getLocation("instance_data", new StorageOptions().setCompression(true));
        CChunkGenerator chunkGenerator = new CChunkGenerator();
        instanceContainer = instanceManager.createInstanceContainer(DimensionType.OVERWORLD);
        instanceContainer.enableAutoChunkLoad(true);
        instanceContainer.setChunkGenerator(chunkGenerator);

        inventory = new Inventory(InventoryType.CHEST_1_ROW, "Test inventory");
        /*inventory.addInventoryCondition((p, slot, clickType, inventoryConditionResult) -> {
            p.sendMessage("click type inventory: " + clickType);
            System.out.println("slot inv: " + slot)0;
            inventoryConditionResult.setCancel(slot == 3);
        });*/
        //inventory.setItemStack(3, new ItemStack(Material.DIAMOND, (byte) 34));
    }

    public static void init() {
        ConnectionManager connectionManager = MinecraftServer.getConnectionManager();
        BenchmarkManager benchmarkManager = MinecraftServer.getBenchmarkManager();
        MinecraftServer.getSchedulerManager().buildTask(() -> {

            Collection<net.minestom.server.entity.Player> players = connectionManager.getOnlinePlayers();

            if (players.isEmpty())
                return;
            final Component header = Component.text("Playing on: "+ Administration.Config.name.string());
            String tmp;
            if (Vars.netServer.admins.getPlayerLimit() == 0) {
                tmp = "∞";
            } else {
                tmp = String.valueOf(Vars.netServer.admins.getPlayerLimit());
            }
            final Component footer = Component.text("There are currently "+Groups.player.size()+players.size()+" out of "+tmp+" players");
            Core.settings.put("totalPlayers", Groups.player.size()+players.size());
            Audiences.players().sendPlayerListHeaderAndFooter(header, footer);

        }).repeat(10, TimeUnit.TICK).schedule();

        connectionManager.onPacketReceive((player, packetController, packet) -> {
            // Listen to all received packet
            //System.out.println("PACKET: "+packet.getClass().getSimpleName());
            packetController.setCancel(false);
        });

        connectionManager.onPacketSend((players, packetController, packet) -> {
            // Listen to all sent packet
            //System.out.println("PACKET: " + packet.getClass().getSimpleName());
            packetController.setCancel(false);
        });

        // EVENT REGISTERING

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addEventCallback(EntityAttackEvent.class, event -> {
            final Entity source = event.getEntity();
            final Entity entity = event.getTarget();
            if (entity instanceof net.minestom.server.entity.Player) {
                net.minestom.server.entity.Player target = (net.minestom.server.entity.Player) entity;
                Vector velocity = source.getPosition().clone().getDirection().multiply(4);
                velocity.setY(3.5f);
                target.setVelocity(velocity);
                target.damage(DamageType.fromEntity(source), 5);
            } else {
                Vector velocity = source.getPosition().clone().getDirection().multiply(3);
                velocity.setY(3f);
                entity.setVelocity(velocity);
            }

            if (source instanceof net.minestom.server.entity.Player) {
                ((net.minestom.server.entity.Player) source).sendMessage(Component.text("You attacked something!"));
            }
        });

        globalEventHandler.addEventCallback(PlayerDeathEvent.class, event -> {
            event.setChatMessage(Component.text("custom death message"));
        });

        globalEventHandler.addEventCallback(PlayerBlockPlaceEvent.class, event -> {
            if (event.getHand() != net.minestom.server.entity.Player.Hand.MAIN)
                return;
            final Block block = Block.fromStateId(event.getBlockStateId());
        });

        globalEventHandler.addEventCallback(PlayerBlockInteractEvent.class, event -> {
            if (event.getHand() != net.minestom.server.entity.Player.Hand.MAIN)
                return;
            final net.minestom.server.entity.Player player = event.getPlayer();

            final short blockStateId = player.getInstance().getBlockStateId(event.getBlockPosition());
            final CustomBlock customBlock = player.getInstance().getCustomBlock(event.getBlockPosition());
            final Block block = Block.fromStateId(blockStateId);
            player.sendMessage(Component.text("You clicked at the block " + block + " " + customBlock));
            player.sendMessage(Component.text("CHUNK COUNT " + instanceContainer.getChunks().size()));
        });

        globalEventHandler.addEventCallback(PickupItemEvent.class, event -> {
            final Entity entity = event.getLivingEntity();
            if (entity instanceof net.minestom.server.entity.Player) {
                // Cancel event if player does not have enough inventory space
                final ItemStack itemStack = event.getItemEntity().getItemStack();
                event.setCancelled(!((net.minestom.server.entity.Player) entity).getInventory().addItemStack(itemStack));
            }
        });

        globalEventHandler.addEventCallback(ItemDropEvent.class, event -> {
            final net.minestom.server.entity.Player player = event.getPlayer();
            ItemStack droppedItem = event.getItemStack();

            Position position = player.getPosition().clone().add(0, 1.5f, 0);
            ItemEntity itemEntity = new ItemEntity(droppedItem, position);
            itemEntity.setPickupDelay(500, TimeUnit.MILLISECOND);
            itemEntity.setInstance(player.getInstance());
            Vector velocity = player.getPosition().clone().getDirection().multiply(6);
            itemEntity.setVelocity(velocity);
        });

        List<mindustry.gen.Player> mPlayers = new ArrayList<mindustry.gen.Player>();
        
        globalEventHandler.addEventCallback(PlayerDisconnectEvent.class, event -> {
            net.minestom.server.entity.Player player = event.getPlayer();
            // mindustry.gen.Player mPlayer = mindustry.gen.Player.create();
            mPlayers.forEach(mPlayer -> {
                if (mPlayer.name().equals("MC|"+player.getUsername())) {
                    mPlayer.remove();
                }
            });
            System.out.println("DISCONNECTION " + player.getUsername());
        });

        globalEventHandler.addEventCallback(PlayerLoginEvent.class, event -> {
            final net.minestom.server.entity.Player player = event.getPlayer();
            mindustry.gen.Player mPlayer = mindustry.gen.Player.create();
            mPlayer.add();
            mPlayer.name("MC|"+player.getUsername());
            mPlayer.name = player.getUsername();
            mPlayers.add(mPlayer);
            // Groups.player.add(mPlayer);

            event.setSpawningInstance(instanceContainer);
            int x = (int) mPlayer.closestCore().x;
            int z = (int) mPlayer.closestCore().y;
            player.setRespawnPoint(new Position(x, 42f, z));

            player.getInventory().addInventoryCondition((p, slot, clickType, inventoryConditionResult) -> {
                if (slot == -999)
                    return;
                //ItemStack itemStack = p.getInventory().getItemStack(slot);
                //System.out.println("test " + itemStack.getIdentifier() + " " + itemStack.getData());
            });
        });

        globalEventHandler.addEventCallback(PlayerSpawnEvent.class, event -> {
            final net.minestom.server.entity.Player player = event.getPlayer();
            player.setGameMode(GameMode.SURVIVAL);

            player.setPermissionLevel(0);
            mindustry.gen.Player mPlayer = mindustry.gen.Player.create();
            mPlayers.forEach(i -> {
                if (i.name().equals("MC|"+player.getUsername())) {
                    mPlayer.set(i);
                }
            });

            player.teleport(new Position(mPlayer.x, 42f, mPlayer.y));

            PlayerInventory inventory = player.getInventory();
            ItemStack itemStack = new ItemStack(Material.STONE, (byte) 64);
            inventory.addItemStack(itemStack);

            {
                ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE, (byte) 1);
                inventory.setChestplate(item);
                item.setDisplayName(Component.text("test"));

                inventory.refreshSlot((short) PlayerInventoryUtils.CHESTPLATE_SLOT);

            }

            //player.getInventory().addItemStack(new ItemStack(Material.STONE, (byte) 32));
        });

        globalEventHandler.addEventCallback(PlayerBlockBreakEvent.class, event -> {
            final short blockStateId = event.getBlockStateId();
            System.out.println("broke " + blockStateId + " " + Block.fromStateId(blockStateId));
        });

        globalEventHandler.addEventCallback(PlayerUseItemEvent.class, useEvent -> {
            final net.minestom.server.entity.Player player = useEvent.getPlayer();
            player.sendMessage("Using item in air: " + useEvent.getItemStack().getMaterial());
        });

        globalEventHandler.addEventCallback(PlayerUseItemOnBlockEvent.class, useEvent -> {
            final net.minestom.server.entity.Player player = useEvent.getPlayer();
            player.sendMessage("Main item: " + player.getInventory().getItemInMainHand().getMaterial());
            player.sendMessage("Using item on block: " + useEvent.getItemStack().getMaterial() + " at " + useEvent.getPosition() + " on face " + useEvent.getBlockFace());
        });

        globalEventHandler.addEventCallback(PlayerChunkUnloadEvent.class, event -> {
            final net.minestom.server.entity.Player player = event.getPlayer();
            final Instance instance = player.getInstance();

            Chunk chunk = instance.getChunk(event.getChunkX(), event.getChunkZ());

            if (chunk == null)
                return;

            // Unload the chunk (save memory) if it has no remaining viewer
            if (chunk.getViewers().isEmpty()) {
                //player.getInstance().unloadChunk(chunk);
            }
        });
    }

    public static ResponseDataConsumer getResponseDataConsumer() {
        return (playerConnection, responseData) -> {
            responseData.setMaxPlayer(0);
            responseData.setOnline(MinecraftServer.getConnectionManager().getOnlinePlayers().size());
            responseData.addPlayer("A name", UUID.randomUUID());
            responseData.addPlayer("Could be some message", UUID.randomUUID());
            responseData.setDescription("IP test: " + playerConnection.getRemoteAddress());
        };
    }

}