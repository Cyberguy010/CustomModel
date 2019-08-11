package com.github.gamepiaynmo.custommodel;

import com.github.gamepiaynmo.custommodel.network.PacketModel;
import com.github.gamepiaynmo.custommodel.network.PacketQuery;
import com.github.gamepiaynmo.custommodel.network.PacketReload;
import com.github.gamepiaynmo.custommodel.util.ModelPack;
import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomModel implements ModInitializer {
    public static final String MODID = "custommodel";
    public static final String MODEL_DIR = "custom-models";

    private static void sendPacket(PlayerEntity player, Identifier id, Packet<?> packet) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            packet.write(buf);
            ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, id, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInitialize() {
        CommandRegistry.INSTANCE.register(false, dispatcher -> {
            dispatcher.register(CommandManager.literal("custommodel").requires((source) -> {
                return source.hasPermissionLevel(2);
            }).then(CommandManager.literal("reload").then(CommandManager.argument("targets", EntityArgumentType.players()).executes(
                    context -> {
                        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "targets");
                        sendPacket(context.getSource().getPlayer(), PacketReload.ID, new PacketReload(players));
                        return 1;
                    })
            )));
        });

        ServerSidePacketRegistry.INSTANCE.register(PacketQuery.ID, (context, buffer) -> {
            PacketQuery packet = new PacketQuery();
            try {
                packet.read(buffer);
                context.getTaskQueue().execute(() -> {
                    PlayerEntity player = context.getPlayer();
                    GameProfile profile = player.world.getPlayerByUuid(packet.getPlayerUuid()).getGameProfile();

                    String nameEntry = profile.getName().toLowerCase();
                    UUID uuid = PlayerEntity.getUuidFromProfile(profile);
                    String uuidEntry = uuid.toString().toLowerCase();
                    List<String> files = ImmutableList.of(nameEntry + ".zip", uuidEntry + ".zip");

                    for (String entry : files) {
                        File modelFile = new File(CustomModel.MODEL_DIR + "/" + entry);

                        try {
                            if (modelFile.isFile()) {
                                sendPacket(player, PacketModel.ID, new PacketModel(modelFile));
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
