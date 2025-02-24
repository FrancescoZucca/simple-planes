package xyz.przemyk.simpleplanes.upgrades.engines.electric;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import com.mojang.math.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.network.NetworkHooks;
import xyz.przemyk.simpleplanes.EnergyStorageWithSet;
import xyz.przemyk.simpleplanes.SimplePlanesMod;
import xyz.przemyk.simpleplanes.client.ClientEventHandler;
import xyz.przemyk.simpleplanes.client.ClientUtil;
import xyz.przemyk.simpleplanes.container.ElectricEngineContainer;
import xyz.przemyk.simpleplanes.entities.PlaneEntity;
import xyz.przemyk.simpleplanes.setup.SimplePlanesEntities;
import xyz.przemyk.simpleplanes.setup.SimplePlanesItems;
import xyz.przemyk.simpleplanes.setup.SimplePlanesUpgrades;
import xyz.przemyk.simpleplanes.upgrades.engines.EngineUpgrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ElectricEngineUpgrade extends EngineUpgrade implements MenuProvider {

    public static final int CAPACITY = 480_000;

    public final EnergyStorageWithSet energyStorage = new EnergyStorageWithSet(CAPACITY);
    public final LazyOptional<EnergyStorage> energyStorageLazyOptional = LazyOptional.of(() -> energyStorage);

    public ElectricEngineUpgrade(PlaneEntity planeEntity) {
        super(SimplePlanesUpgrades.ELECTRIC_ENGINE.get(), planeEntity);
    }

    @Override
    public void tick() {
        if (!planeEntity.getParked()) {
            if (energyStorage.extractEnergy(40 * planeEntity.getFuelCost(), false) > 0) {
                updateClient();
            }
        }
    }

    @Override
    public boolean isPowered() {
        return energyStorage.getEnergyStored() > 0;
    }

    @Override
    public void renderPowerHUD(PoseStack matrixStack, HumanoidArm side, int scaledWidth, int scaledHeight, float partialTicks) {
        int i = scaledWidth / 2;
        if (side == HumanoidArm.LEFT) {
            ClientEventHandler.blit(matrixStack, -90, i - 91 - 29, scaledHeight - 22, 38, 44, 22, 21);
        } else {
            ClientEventHandler.blit(matrixStack, -90, i + 91, scaledHeight - 22, 38, 44, 22, 21);
        }

        int energy = energyStorage.getEnergyStored();

        if (energy > 0) {
            int energyScaled = energy * 15 / CAPACITY;
            if (side == HumanoidArm.LEFT) {
                ClientEventHandler.blit(matrixStack, -90, i - 91 - 29 + 3, scaledHeight - 22 + 16 - energyScaled, 60, 57 - energyScaled, 16, energyScaled + 2);
            } else {
                ClientEventHandler.blit(matrixStack, -90, i + 91 + 3, scaledHeight - 22 + 16 - energyScaled, 60, 57 - energyScaled, 16, energyScaled + 2);
            }
        }
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource buffer, int packedLight, float partialTicks) {
        matrixStack.pushPose();
        EntityType<?> entityType = planeEntity.getType();

        if (entityType == SimplePlanesEntities.HELICOPTER.get()) {
            matrixStack.translate(0, -0.8, 0.65);
        } else if (entityType == SimplePlanesEntities.LARGE_PLANE.get()) {
            matrixStack.translate(0, 0, 1.1);
        }

        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(180));
        matrixStack.translate(-0.4, -1, 0.3);
        matrixStack.scale(0.82f, 0.82f, 0.82f);
        ClientUtil.renderItemModelAsBlock(matrixStack, Minecraft.getInstance(), buffer, packedLight, SimplePlanesItems.ELECTRIC_ENGINE.get());

        matrixStack.popPose();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyStorageLazyOptional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compoundNBT = new CompoundTag();
        compoundNBT.putInt("energy", energyStorage.getEnergyStored());
        return compoundNBT;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        int energy = nbt.getInt("energy");
        energyStorage.setEnergy(Math.min(energy, CAPACITY));
    }

    @Override
    public void writePacket(FriendlyByteBuf buffer) {
        buffer.writeVarInt(energyStorage.getEnergyStored());
    }

    @Override
    public void readPacket(FriendlyByteBuf buffer) {
        energyStorage.setEnergy(buffer.readVarInt());
    }

    @Override
    public boolean canOpenGui() {
        return true;
    }

    @Override
    public void openGui(ServerPlayer playerEntity) {
        NetworkHooks.openGui(playerEntity, this, buffer -> buffer.writeVarInt(planeEntity.getId()));
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent(SimplePlanesMod.MODID + ".electric_engine_container");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
        return new ElectricEngineContainer(id, playerInventory, planeEntity.getId());
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return energyStorageLazyOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onRemoved() {
        planeEntity.spawnAtLocation(SimplePlanesItems.ELECTRIC_ENGINE.get());
    }
}
