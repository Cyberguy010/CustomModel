package com.github.gamepiaynmo.custommodel.mixin;

import com.github.gamepiaynmo.custommodel.client.CustomModelClient;
import com.github.gamepiaynmo.custommodel.client.ModelPack;
import com.github.gamepiaynmo.custommodel.render.EntityDimensions;
import com.github.gamepiaynmo.custommodel.render.EntityPose;
import com.github.gamepiaynmo.custommodel.server.CustomModel;
import com.github.gamepiaynmo.custommodel.server.ModConfig;
import com.github.gamepiaynmo.custommodel.server.ModelInfo;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PlayerStatureHandler {
    private static EntityPose getPose(EntityLivingBase entity) {
        if (entity.isElytraFlying()) return EntityPose.FALL_FLYING;
        if (entity.isPlayerSleeping()) return EntityPose.SLEEPING;
        if (entity.isSneaking()) return EntityPose.SNEAKING;
        return EntityPose.STANDING;
    }

    private static void setSize(EntityLivingBase entity, EntityDimensions dimensions) {
        AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox();
        axisalignedbb = new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ,
                axisalignedbb.minX + dimensions.width, axisalignedbb.minY + dimensions.height, axisalignedbb.minZ + dimensions.width);

        if (!entity.world.collidesWithAnyBlock(axisalignedbb)) {
            if (dimensions.width != entity.width || dimensions.height != entity.height) {
                float f = entity.width;
                entity.width = dimensions.width;
                entity.height = dimensions.height;

                if (entity.width < f) {
                    double d0 = (double)dimensions.width / 2.0D;
                    entity.setEntityBoundingBox(new AxisAlignedBB(entity.posX - d0, entity.posY, entity.posZ - d0, entity.posX + d0, entity.posY + entity.height, entity.posZ + d0));
                    return;
                }

                axisalignedbb = entity.getEntityBoundingBox();
                entity.setEntityBoundingBox(new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ, axisalignedbb.minX + entity.width, axisalignedbb.minY + entity.height, axisalignedbb.minZ + entity.width));

                if (entity.width > f && !entity.world.isRemote) {
                    entity.move(MoverType.SELF, f - entity.width, 0.0D, f - entity.width);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        EntityPose pose = getPose(player);

        if (player instanceof AbstractClientPlayer) {
            AbstractClientPlayer clientPlayer = (AbstractClientPlayer) player;
            ModelPack pack = CustomModelClient.getModelForPlayer(clientPlayer);
            if (pack != null) {
                if (CustomModelClient.serverConfig.customEyeHeight && event.phase == TickEvent.Phase.START) {
                    Float eyeHeight = pack.getModel().getModelInfo().eyeHeightMap.get(pose);
                    if (eyeHeight != null)
                        player.eyeHeight = eyeHeight;
                }

                if (CustomModelClient.serverConfig.customBoundingBox && event.phase == TickEvent.Phase.END) {
                    EntityDimensions dimensions = pack.getModel().getModelInfo().dimensionsMap.get(pose);
                    if (dimensions != null)
                        setSize(player, dimensions);
                }
            }
        }

        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            ModelInfo pack = CustomModel.getBoundingBoxForPlayer(serverPlayer);
            if (pack != null) {
                if (ModConfig.isCustomEyeHeight() && event.phase == TickEvent.Phase.START) {
                    Float eyeHeight = pack.eyeHeightMap.get(pose);
                    if (eyeHeight != null)
                        player.eyeHeight = eyeHeight;
                }

                if (ModConfig.isCustomBoundingBox() && event.phase == TickEvent.Phase.END) {
                    EntityDimensions dimensions = pack.dimensionsMap.get(pose);
                    if (dimensions != null)
                        setSize(player, dimensions);
                }
            }
        }
    }
}
