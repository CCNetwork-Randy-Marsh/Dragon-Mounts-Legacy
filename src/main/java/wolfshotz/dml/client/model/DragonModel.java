package wolfshotz.dml.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.EntityType;
import wolfshotz.dml.entity.DMLEntities;
import wolfshotz.dml.entity.dragons.TameableDragonEntity;
import wolfshotz.dml.util.MathX;

public class DragonModel extends EntityModel<TameableDragonEntity>
{
    // model constants
    public static final int NECK_SIZE = 10;
    public static final int TAIL_SIZE = 10;
    public static final int VERTS_NECK = 7;
    public static final int VERTS_TAIL = 12;
    public static final int HEAD_OFS = -16;

    // model parts
    public ModelPart head;
    public ModelPart neck;
    public ModelPart neckScale;
    public ModelPart tail;
    public ModelPart tailHornLeft;
    public ModelPart tailHornRight;
    public ModelPart tailScaleLeft;
    public ModelPart tailScaleMiddle;
    public ModelPart tailScaleRight;
    public ModelPart jaw;
    public ModelPart body;
    public ModelPart back;
    public ModelPart forethigh;
    public ModelPart forecrus;
    public ModelPart forefoot;
    public ModelPart foretoe;
    public ModelPart hindthigh;
    public ModelPart hindcrus;
    public ModelPart hindfoot;
    public ModelPart hindtoe;
    public ModelPart wingArm;
    public ModelPart wingForearm;
    public ModelPart[] wingFinger = new ModelPart[4];
    // model attributes
    public ModelPartProxy[] neckProxy = new ModelPartProxy[VERTS_NECK];
    public ModelPartProxy[] tailProxy = new ModelPartProxy[VERTS_TAIL];
    public ModelPartProxy[] thighProxy = new ModelPartProxy[4];
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float pitch;
    public float size;
    // delegates
    private TameableDragonEntity dragon;
    private final EntityType<TameableDragonEntity> type;

    public DragonModel(EntityType<TameableDragonEntity> type)
    {
        this.type = type;

        textureWidth = 256;
        textureHeight = 256;

        buildBody();
        buildNeck();
        buildHead();
        buildTail();
        buildWing();
        buildLegs();
    }

    private void buildHead()
    {
        head = new ModelPart(this);
        head.addBox(-6, -1, -8 + HEAD_OFS, 12, 5, 16, 56, 88);
        head.addBox(-8, -8, 6 + HEAD_OFS, 16, 16, 16, 0, 0);
        head.addBox(-5, -3, -6 + HEAD_OFS, 2, 2, 4, 48, 0);
        head.mirror = true;
        head.addBox(3, -3, -6 + HEAD_OFS, 2, 2, 4, 48, 0);

        buildHorn(false);
        buildHorn(true);

        jaw = head.addChildBox(-6, 0, -16, 12, 4, 16, 0, 88);
        jaw.setRotationPoint(0, 4, 8 + HEAD_OFS);
    }

    private void buildHorn(boolean mirror)
    {
        int hornThick = 3;
        int hornLength = 12;

        float hornOfs = -(hornThick / 2f);

        float hornPosX = -5;
        float hornPosY = -8;
        float hornPosZ = 0;

        float hornRotX = MathX.toRadians(30);
        float hornRotY = MathX.toRadians(-30);
        float hornRotZ = 0;

        if (mirror)
        {
            hornPosX *= -1;
            hornRotY *= -1;
        }

        head.mirror = mirror;
        ModelPart horn = head.addChildBox(hornOfs, hornOfs, hornOfs, hornThick, hornThick, hornLength, 28, 32);
        horn.setRotationPoint(hornPosX, hornPosY, hornPosZ);
        horn.setAngles(hornRotX, hornRotY, hornRotZ);
    }

    private void buildNeck()
    {
        neck = new ModelPart(this);
        neck.addBox(-5, -5, -5, NECK_SIZE, NECK_SIZE, NECK_SIZE, 112, 88);
        neckScale = neck.addChildBox(-1, -7, -3, 2, 4, 6, 0, 0);

        // initialize model proxies
        for (int i = 0; i < neckProxy.length; i++)
        {
            neckProxy[i] = new ModelPartProxy(neck);
        }
    }

    private void buildTail()
    {
        tail = new ModelPart(this);
        tail.addBox(-5, -5, -5, TAIL_SIZE, TAIL_SIZE, TAIL_SIZE, 152, 88);
        float scaleRotZ = MathX.toRadians(45);
        tailScaleLeft = tail.addChildBox(-1, -8, -3, 2, 4, 6, 0, 0).setAngles(0, 0, scaleRotZ);
        tailScaleMiddle = tail.addChildBox(-1, -8, -3, 2, 4, 6, 0, 0).setAngles(0, 0, 0);
        tailScaleRight = tail.addChildBox(-1, -8, -3, 2, 4, 6, 0, 0).setAngles(0, 0, -scaleRotZ);

        boolean show = type == DMLEntities.FIRE_DRAGON.get();

        tailScaleMiddle.showModel = !show;
        tailScaleLeft.showModel = show;
        tailScaleRight.showModel = show;

        buildTailHorn(false);
        buildTailHorn(true);

        // initialize model proxies
        for (int i = 0; i < tailProxy.length; i++)
        {
            tailProxy[i] = new ModelPartProxy(tail);
        }
    }

    private void buildTailHorn(boolean mirror)
    {
        int hornThick = 3;
        int hornLength = 32;

        float hornOfs = -(hornThick / 2f);

        float hornPosX = 0;
        float hornPosY = hornOfs;
        float hornPosZ = TAIL_SIZE / 2f;

        float hornRotX = MathX.toRadians(-15);
        float hornRotY = MathX.toRadians(-145);
        float hornRotZ = 0;

        if (mirror)
        {
            hornPosX *= -1;
            hornRotY *= -1;
        }

        tail.mirror = mirror;
        ModelPart horn = tail.addChildBox(hornOfs, hornOfs, hornOfs, hornThick, hornThick, hornLength, 0, 117);
        horn.setRotationPoint(hornPosX, hornPosY, hornPosZ);
        horn.setAngles(hornRotX, hornRotY, hornRotZ);
        horn.showModel = type == DMLEntities.WATER_DRAGON.get();

        if (mirror) tailHornLeft = horn;
        else tailHornRight = horn;
    }

    private void buildBody()
    {
        body = new ModelPart(this);
        body.setRotationPoint(0, 4, 8);
        body.addBox(-12, 0, -16, 24, 24, 64, 0, 0);
        body.addBox(-1, -6, 10, 2, 6, 12, 0, 32);
        body.addBox(-1, -6, 30, 2, 6, 12, 0, 32);

        back = body.addChildBox(-1, -6, -10, 2, 6, 12, 0, 32);
    }

    private void buildWing()
    {
        wingArm = new ModelPart(this);
        wingArm.setRotationPoint(-10, 5, 4);
        wingArm.setRenderScale(1.1f);
        wingArm.addBox(-28, -3, -3, 28, 6, 6, 0, 152);
        wingArm.addBox(-28, 0, 2, 28, 0, 24, 116, 232);

        wingForearm = new ModelPart(this);
        wingForearm.setRotationPoint(-28, 0, 0);
        wingForearm.addBox(-48, -2, -2, 48, 4, 4, 0, 164);
        wingArm.addChild(wingForearm);

        wingFinger[0] = buildWingFinger(false);
        wingFinger[1] = buildWingFinger(false);
        wingFinger[2] = buildWingFinger(false);
        wingFinger[3] = buildWingFinger(true);
    }

    private ModelPart buildWingFinger(boolean small)
    {
        ModelPart finger = new ModelPart(this);
        finger.setRotationPoint(-47, 0, 0);
        finger.addBox(-70, -1, -1, 70, 2, 2, 0, 172);
        if (small) finger.addBox(-70, 0, 1, 70, 0, 32, -32, 224);
        else finger.addBox(-70, 0, 1, 70, 0, 48, -49, 176);
        wingForearm.addChild(finger);

        return finger;
    }

    private void buildLegs()
    {
        buildLeg(false);
        buildLeg(true);

        // initialize model proxies
        for (int i = 0; i < 4; i++)
        {
            if (i % 2 == 0)
            {
                thighProxy[i] = new ModelPartProxy(forethigh);
            }
            else
            {
                thighProxy[i] = new ModelPartProxy(hindthigh);
            }
        }
    }

    private void buildLeg(boolean hind)
    {
        // thinner legs for skeletons
        boolean thin = type == DMLEntities.GHOST_DRAGON.get();
        float baseLength = 26;
        int texY = hind ? 29 : 0;

        // thigh variables
        float thighPosX = -11;
        float thighPosY = 18;
        float thighPosZ = 4;

        int thighThick = 9 - (thin ? 2 : 0);
        int thighLength = (int) (baseLength * (hind ? 0.9f : 0.77f));

        if (hind)
        {
            thighThick++;
            thighPosY -= 5;
        }

        float thighOfs = -(thighThick / 2f);

        ModelPart thigh = new ModelPart(this);
        thigh.setRotationPoint(thighPosX, thighPosY, thighPosZ);
        thigh.addBox(thighOfs, thighOfs, thighOfs, thighThick, thighLength, thighThick, 112, texY);

        // crus variables
        float crusPosX = 0;
        float crusPosY = thighLength + thighOfs;
        float crusPosZ = 0;

        int crusThick = thighThick - 2;
        int crusLength = (int) (baseLength * (hind ? 0.7f : 0.8f));

        if (hind)
        {
            crusThick--;
            crusLength -= 2;
        }

        float crusOfs = -(crusThick / 2f);

        ModelPart crus = new ModelPart(this);
        crus.setRotationPoint(crusPosX, crusPosY, crusPosZ);
        crus.addBox(crusOfs, crusOfs, crusOfs, crusThick, crusLength, crusThick, hind ? 152 : 148, texY);
        thigh.addChild(crus);

        // foot variables
        float footPosX = 0;
        float footPosY = crusLength + (crusOfs / 2f);
        float footPosZ = 0;

        int footWidth = crusThick + 2 + (thin ? 2 : 0);
        int footHeight = 4;
        int footLength = (int) (baseLength * (hind ? 0.67f : 0.34f));

        float footOfsX = -(footWidth / 2f);
        float footOfsY = -(footHeight / 2f);
        float footOfsZ = footLength * -0.75f;

        ModelPart foot = new ModelPart(this);
        foot.setRotationPoint(footPosX, footPosY, footPosZ);
        foot.addBox(footOfsX, footOfsY, footOfsZ, footWidth, footHeight, footLength, hind ? 180 : 210, texY);
        crus.addChild(foot);

        // toe variables
        int toeWidth = footWidth;
        int toeHeight = footHeight;
        int toeLength = (int) (baseLength * (hind ? 0.27f : 0.33f));

        float toePosX = 0;
        float toePosY = 0;
        float toePosZ = footOfsZ - (footOfsY / 2f);

        float toeOfsX = -(toeWidth / 2f);
        float toeOfsY = -(toeHeight / 2f);
        float toeOfsZ = -toeLength;

        ModelPart toe = new ModelPart(this);
        toe.setRotationPoint(toePosX, toePosY, toePosZ);
        toe.addBox(toeOfsX, toeOfsY, toeOfsZ, toeWidth, toeHeight, toeLength, hind ? 215 : 176, texY);
        foot.addChild(toe);

        if (hind)
        {
            hindthigh = thigh;
            hindcrus = crus;
            hindfoot = foot;
            hindtoe = toe;
        }
        else
        {
            forethigh = thigh;
            forecrus = crus;
            forefoot = foot;
            foretoe = toe;
        }
    }

    // First
    @Override
    public void setLivingAnimations(TameableDragonEntity dragon, float limbSwing, float limbSwingAmount, float partialTick)
    {
        this.dragon = dragon;
        dragon.animator.setPartialTicks(partialTick);
        dragon.animator.setMovement(limbSwing, limbSwingAmount * dragon.getScale());
        size = dragon.getScale();
    }

    // Second
    @Override
    public void setRotationAngles(TameableDragonEntity dragon, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        dragon.animator.setLook(netHeadYaw, headPitch);
        dragon.animator.animate(this);
    }

    @Override
    public void render(MatrixStack ms, IVertexBuilder buffer, int packedLight, int packedOverlay, float r, float g, float b, float a)
    {
        ms.push();
        ms.translate(offsetX, offsetY, offsetZ);
        ms.rotate(Vector3f.XP.rotationDegrees(-pitch));

        renderHead(ms, buffer, packedLight, packedOverlay, r, g, b, a);
        renderNeck(ms, buffer, packedLight, packedOverlay, r, g, b, a);
        renderBody(ms, buffer, packedLight, packedOverlay, r, g, b, a);
        renderWings(ms, buffer, packedLight, packedOverlay, r, g, b, a);
        renderLegs(ms, buffer, packedLight, packedOverlay, r, g, b, a);
        renderTail(ms, buffer, packedLight, packedOverlay, r, g, b, a);

        ms.pop();
    }

    protected void renderBody(MatrixStack ms, IVertexBuilder buffer, int packedLight, int packedOverlay, float r, float g, float b, float a)
    {
        body.render(ms, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    protected void renderHead(MatrixStack ms, IVertexBuilder buffer, int packedLight, int packedOverlay, float r, float g, float b, float a)
    {
        float headScale = 1.4f / (size + 0.4f);

        head.setRenderScale(headScale);
        head.render(ms, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    protected void renderNeck(MatrixStack ms, IVertexBuilder buffer, int packedLight, int packedOverlay, float r, float g, float b, float a)
    {
        for (ModelPartProxy proxy : neckProxy) proxy.render(ms, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    protected void renderTail(MatrixStack ms, IVertexBuilder buffer, int packedLight, int packedOverlay, float r, float g, float b, float a)
    {
        for (ModelPartProxy proxy : tailProxy) proxy.render(ms, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    protected void renderWings(MatrixStack ms, IVertexBuilder buffer, int packedLight, int packedOverlay, float r, float g, float b, float a)
    {
        ms.push();

        for (int i = 0; i < 2; i++)
        {
            wingArm.render(ms, buffer, packedLight, packedOverlay, r, g, b, a);

            if (i == 0)
            {
                // mirror next wing
                ms.scale(-1, 1, 1);
            }
        }


        ms.pop();
    }

    protected void renderLegs(MatrixStack ms, IVertexBuilder buffer, int packedLight, int packedOverlay, float r, float g, float b, float a)
    {

        for (int i = 0; i < thighProxy.length; i++)
        {
            thighProxy[i].render(ms, buffer, packedLight, packedOverlay, r, g, b, a);

            if (i == 1)
            {
                // mirror next legs
                ms.scale(-1, 1, 1);
                // switch to front face culling
            }
        }
    }


}
