package xyz.przemyk.simpleplanes.client.render.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import xyz.przemyk.simpleplanes.entities.PlaneEntity;

import static xyz.przemyk.simpleplanes.client.render.PlaneRenderer.getPropellerRotation;

public class HelicopterPropellerModel extends EntityModel<PlaneEntity> {
    private final ModelPart p;
    private final ModelPart bone_propeller;
    private final ModelPart bone_propeller2;

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        PartDefinition p = partDefinition.addOrReplaceChild("p", CubeListBuilder.create()
                .texOffs(0, 0).addBox(2.0F, 6.0F, 42.0F, 3.0F, 2.0F, 2.0F)
                .texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0, -16, 12)
        );

        p.addOrReplaceChild("bone_propeller", CubeListBuilder.create().texOffs(0, 0).addBox(-31.0F, -1.0F, -3.0F, 62.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(0, 0, 0, 0, 3.1416F, 0));
        p.addOrReplaceChild("bone_propeller2", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -7.0F, -1.0F, 1.0F, 14.0F, 2.0F), PartPose.offsetAndRotation(5.5F, 7.0F, 43.0F, 0, -0.0436F, 0));
        return LayerDefinition.create(meshDefinition, 16, 16);
    }

    public HelicopterPropellerModel(ModelPart part) {
        p = part.getChild("p");
        bone_propeller = p.getChild("bone_propeller");
        bone_propeller2 = p.getChild("bone_propeller2");
    }

    @Override
    public void setupAnim(PlaneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isPowered() && !entity.getParked()) {
            bone_propeller.yRot =
                getPropellerRotation(entity, limbSwing);
            bone_propeller2.xRot =
                getPropellerRotation(entity, limbSwing);
        } else {
            bone_propeller.yRot = 0;
            bone_propeller2.xRot = 0;
        }
    }

    @Override
    public void renderToBuffer(PoseStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        p.render(matrixStack, buffer, packedLight, packedOverlay);
    }

}