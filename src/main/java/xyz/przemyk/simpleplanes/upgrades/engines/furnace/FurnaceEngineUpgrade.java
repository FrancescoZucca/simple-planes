package xyz.przemyk.simpleplanes.upgrades.engines.furnace;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.inventory.ContainerData;
import com.mojang.math.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import xyz.przemyk.simpleplanes.SimplePlanesMod;
import xyz.przemyk.simpleplanes.client.ClientEventHandler;
import xyz.przemyk.simpleplanes.client.ClientUtil;
import xyz.przemyk.simpleplanes.container.FurnaceEngineContainer;
import xyz.przemyk.simpleplanes.entities.PlaneEntity;
import xyz.przemyk.simpleplanes.setup.SimplePlanesEntities;
import xyz.przemyk.simpleplanes.setup.SimplePlanesItems;
import xyz.przemyk.simpleplanes.setup.SimplePlanesUpgrades;
import xyz.przemyk.simpleplanes.upgrades.engines.EngineUpgrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FurnaceEngineUpgrade extends EngineUpgrade implements MenuProvider {

    public final ItemStackHandler itemStackHandler = new ItemStackHandler();
    public final LazyOptional<ItemStackHandler> itemHandlerLazyOptional = LazyOptional.of(() -> itemStackHandler);
    public int burnTime;
    public int burnTimeTotal;

    public FurnaceEngineUpgrade(PlaneEntity planeEntity) {
        super(SimplePlanesUpgrades.FURNACE_ENGINE.get(), planeEntity);
    }

    @Override
    public void tick() {
        if (burnTime > 0) {
            burnTime -= planeEntity.getFuelCost();
            updateClient();
        } else {
            ItemStack itemStack = itemStackHandler.getStackInSlot(0);
            int itemBurnTime = ForgeHooks.getBurnTime(itemStack, null);
            if (itemBurnTime > 0) {
                burnTimeTotal = itemBurnTime;
                burnTime = itemBurnTime;
                if (itemStack.hasContainerItem()) {
                    itemStackHandler.setStackInSlot(0, itemStack.getContainerItem());
                } else {
                    itemStackHandler.extractItem(0, 1, false);
                }
                updateClient();
            }
        }
    }

    @Override
    public boolean isPowered() {
        return burnTime > 0;
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
        ClientUtil.renderItemModelAsBlock(matrixStack, Minecraft.getInstance(), buffer, packedLight, SimplePlanesItems.FURNACE_ENGINE.get());

        matrixStack.popPose();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerLazyOptional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compound = new CompoundTag();
        compound.put("item", itemStackHandler.serializeNBT());
        compound.putInt("burnTime", burnTime);
        compound.putInt("burnTimeTotal", burnTimeTotal);
        return compound;
    }

    @Override
    public void deserializeNBT(CompoundTag compound) {
        itemStackHandler.deserializeNBT(compound.getCompound("item"));
        burnTime = compound.getInt("burnTime");
        burnTimeTotal = compound.getInt("burnTimeTotal");
    }

    @Override
    public void writePacket(FriendlyByteBuf buffer) {
        buffer.writeItem(itemStackHandler.getStackInSlot(0));
        buffer.writeVarInt(burnTime);
        buffer.writeVarInt(burnTimeTotal);
    }

    @Override
    public void readPacket(FriendlyByteBuf buffer) {
        itemStackHandler.setStackInSlot(0, buffer.readItem());
        burnTime = buffer.readVarInt();
        burnTimeTotal = buffer.readVarInt();
    }

    @Override
    public boolean canOpenGui() {
        return true;
    }

    @Override
    public void openGui(ServerPlayer playerEntity) {
        NetworkHooks.openGui(playerEntity, this);
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent(SimplePlanesMod.MODID + ".furnace_engine_container");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
        return new FurnaceEngineContainer(id, playerInventory, itemStackHandler, new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 0) {
                    return burnTime;
                }
                return burnTimeTotal;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 2;
            }
        });
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return itemHandlerLazyOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onRemoved() {
        planeEntity.spawnAtLocation(SimplePlanesItems.FURNACE_ENGINE.get());
        planeEntity.spawnAtLocation(itemStackHandler.getStackInSlot(0));
    }

    @Override
    public void renderPowerHUD(PoseStack matrixStack, HumanoidArm side, int scaledWidth, int scaledHeight, float partialTicks) {
        int i = scaledWidth / 2;
        Minecraft mc = Minecraft.getInstance();
        if (side == HumanoidArm.LEFT) {
            ClientEventHandler.blit(matrixStack, -90, i - 91 - 29, scaledHeight - 40, 0, 44, 22, 40);
        } else {
            ClientEventHandler.blit(matrixStack, -90, i + 91, scaledHeight - 40, 0, 44, 22, 40);
        }

        if (burnTime > 0) {
            int burnTimeTotal2 = burnTimeTotal == 0 ? 200 : burnTimeTotal;
            int burnLeftScaled = burnTime * 13 / burnTimeTotal2;
            if (side == HumanoidArm.LEFT) {
                // render on left side
                ClientEventHandler.blit(matrixStack, -90, i - 91 - 29 + 4, scaledHeight - 40 + 16 - burnLeftScaled, 22, 56 - burnLeftScaled, 14, burnLeftScaled + 1);
            } else {
                // render on right side
                ClientEventHandler.blit(matrixStack, -90, i + 91 + 4, scaledHeight - 40 + 16 - burnLeftScaled, 22, 56 - burnLeftScaled, 14, burnLeftScaled + 1);
            }
        }

        ItemStack fuelStack = itemStackHandler.getStackInSlot(0);
        if (!fuelStack.isEmpty()) {
            int i2 = scaledHeight - 16 - 3;
            if (side == HumanoidArm.LEFT) {
                ClientEventHandler.renderHotbarItem(matrixStack, i - 91 - 26, i2, partialTicks, fuelStack, mc);
            } else {
                ClientEventHandler.renderHotbarItem(matrixStack, i + 91 + 3, i2, partialTicks, fuelStack, mc);
            }
        }
    }
}
