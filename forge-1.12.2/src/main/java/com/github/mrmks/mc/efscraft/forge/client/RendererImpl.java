package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static net.minecraft.client.renderer.OpenGlHelper.*;
import static org.lwjgl.opengl.GL11.*;

class RendererImpl extends Renderer {

    private Framebuffer working = null, backup = null;
    private int lastFramebuffer = -1;
    private final int vao;
    private final boolean translucent;

    RendererImpl(RenderingQueue queue, boolean translucent) {
        super(queue);
        this.translucent = translucent;

        float[] data = {
                -1f, -1f,  0,      0, 0,        // left-bottom
                -1f,  1f,  0,      0, 1,        // left-top
                +1f,  1f,  0,      1, 1,        // right-top
                +1f, -1f,  0,      1, 0,        // right-bottom
        };

        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length * 4);
        buffer.asFloatBuffer().put(data);
        buffer.position(0);

        vao = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vao);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        StringBuilder bd = new StringBuilder()
                .append("#version 120\n")
                .append("uniform sampler2D backupColor;\n")
                .append("uniform sampler2D backupDepth;\n")
                .append("uniform sampler2D workingDepth;\n")
                .append("uniform sampler2D mainDepth;\n")
                ;
    }

    @Override
    protected double[] getRenderViewEntityPos() {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity == null)
            return null;
        else
            return new double[] {entity.posX, entity.posY, entity.posZ};
    }

    @Override
    protected double[] getRenderViewEntityPrevPos() {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity == null)
            return null;
        else
            return new double[] {entity.prevPosX, entity.prevPosY, entity.prevPosZ};
    }

    @Override
    protected void getModelviewMatrix(FloatBuffer buffer) {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
    }

    @Override
    protected void getProjectionMatrix(FloatBuffer buffer) {
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buffer);
    }

    @SubscribeEvent
    public void resizeFramebuffer(ResizeFramebufferEvent event) {
        if (OpenGlHelper.isFramebufferEnabled() && FramebufferHelper.apiSupported && working != null) {

            int current = FramebufferHelper.getCurrentFramebuffer();

            Minecraft mc = Minecraft.getMinecraft();
            working.createFramebuffer(mc.displayWidth, mc.displayHeight);
            working.bindFramebuffer(true);

            backup.createFramebuffer(mc.displayWidth, mc.displayHeight);
            backup.bindFramebuffer(true);

            glBindFramebuffer(GL_FRAMEBUFFER, current);
        }
    }

    @SubscribeEvent
    public void renderWorld(RenderParticleEvent event) {

        Minecraft minecraft = Minecraft.getMinecraft();

        if (OpenGlHelper.isFramebufferEnabled() && FramebufferHelper.apiSupported && translucent)
        {

            int current = FramebufferHelper.getCurrentFramebuffer();

            if (working == null) {
                working = new Framebuffer(minecraft.displayWidth, minecraft.displayHeight, true);
                working.setFramebufferColor(0, 0, 0, 0);
                working.bindFramebuffer(true);

                backup = new Framebuffer(minecraft.displayWidth, minecraft.displayHeight, true);
                backup.bindFramebuffer(true);
                backup.enableStencil();

                glBindFramebuffer(GL_FRAMEBUFFER, current);
            }

            int width = working.framebufferWidth, height = working.framebufferHeight;

            if (event.prev)
            {
                update(event.partial, event.finishNano, 1000_000_000L, Minecraft.getMinecraft().isGamePaused());

                working.framebufferClear();
                backup.framebufferClear();

                // mask sure that current framebuffer has a stencil buffer;
                Framebuffer fbo = Minecraft.getMinecraft().getFramebuffer();
                if (fbo.framebufferObject == current) {
                    if (!fbo.isStencilEnabled()) fbo.enableStencil();
                }

                FramebufferHelper.copyDepthFrom(current, working.framebufferObject, width, height);
                FramebufferHelper.copyFrom(current, backup.framebufferObject, GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT, width, height);

                glBindFramebuffer(GL_FRAMEBUFFER, working.framebufferObject);
                GlStateManager.depthMask(true);
                lastFramebuffer = current;
            }
            else
            {
                GlStateManager.depthMask(false);

                current = lastFramebuffer;
                lastFramebuffer = -1;
                glBindFramebuffer(GL_FRAMEBUFFER, current);
                FramebufferHelper.copyDepthFrom(working.framebufferObject, current, width, height);

                // generate stencil buffer
                glEnable(GL_STENCIL_TEST);
                glStencilMask(0xff);
                glStencilFunc(GL_ALWAYS, 1, 0xff);
                glStencilOp(GL_KEEP, GL_REPLACE, GL_KEEP);

                GlStateManager.colorMask(false, false, false, false);
                draw();
                GlStateManager.colorMask(true, true, true, true);

                // restore depth buffer
                FramebufferHelper.copyDepthFrom(backup.framebufferObject, current, width, height);

                // render effects with greater depth;
                glStencilMask(0x00);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                glStencilFunc(GL_EQUAL, 1, 0xff);

                draw();

                // render working texture to main framebuffer
                glDisable(GL_STENCIL_TEST);

                GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                drawTexture(working.framebufferTexture);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

                // render effects with less depth
                GlStateManager.enableDepth();
                GlStateManager.depthMask(false);
                glBindFramebuffer(GL_FRAMEBUFFER, current);

                glEnable(GL_STENCIL_TEST);
                glStencilMask(0x00);
                glStencilFunc(GL_NOTEQUAL, 1, 0xff);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

                draw();

                glDisable(GL_STENCIL_TEST);

                // restore stencil buffer
                FramebufferHelper.copyFrom(backup.framebufferObject, current, GL_STENCIL_BUFFER_BIT, width, height);
            }
        }
        else
        {
            if (!event.prev) {
                updateAndRender(event.partial, event.finishNano, 1000_000_000L, Minecraft.getMinecraft().isGamePaused());
            }
        }
    }

    private void drawTexture(int textureNext) {
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.disableFog();

        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        int textureId = GlStateManager.glGetInteger(GL_TEXTURE_BINDING_2D);
        if (textureNext >= 0)
            GlStateManager.bindTexture(textureNext);

        OpenGlHelper.glBindBuffer(GL_ARRAY_BUFFER, vao);
        GlStateManager.glVertexPointer(3, GL_FLOAT, 20, 0);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glTexCoordPointer(2, GL_FLOAT, 20, 12);

        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        GlStateManager.glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        GlStateManager.glDisableClientState(GL_VERTEX_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glDisableClientState(GL_TEXTURE_COORD_ARRAY);

        if (textureNext >= 0)
            GlStateManager.bindTexture(textureId);

        OpenGlHelper.glBindBuffer(GL_ARRAY_BUFFER, 0);

        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.popMatrix();

        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableFog();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (world != null && world.isRemote) unloadRender();
    }

    public void deleteFramebuffer() {
        if (backup != null)
            backup.deleteFramebuffer();
        if (working != null)
            working.deleteFramebuffer();
        if (vao >= 0)
            glDeleteBuffers(vao);
    }

    public static class RenderParticleEvent extends Event {
        private final int pass;
        private final float partial;
        private final long finishNano;
        private final boolean prev;

        RenderParticleEvent(int pass, float partial, long finishNano, boolean prev) {
            this.pass = pass;
            this.partial = partial;
            this.finishNano = finishNano;
            this.prev = prev;
        }
    }

    public static class ResizeFramebufferEvent extends Event {}

    private static class FramebufferHelper {

        static final boolean apiSupported;
        static final boolean apiGL30;

        static final int GL_FRAMEBUFFER_BINDING;
        static final int GL_READ_FRAMEBUFFER;
        static final int GL_DRAW_FRAMEBUFFER;

        static final int GL_STENCIL_ATTACHMENT;
        static final int GL_DEPTH_STENCIL_ATTACHMENT;
        static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME;

        static final int GL_STENCIL_INDEX8;

        static {
            ContextCapabilities cap = GLContext.getCapabilities();

            apiGL30 = cap.OpenGL30;
            apiSupported = OpenGlHelper.framebufferSupported && (cap.OpenGL30 || cap.GL_ARB_framebuffer_object);

            GL_FRAMEBUFFER_BINDING = apiSupported ? (apiGL30 ? GL30.GL_FRAMEBUFFER_BINDING : ARBFramebufferObject.GL_FRAMEBUFFER_BINDING) : -1;
            GL_READ_FRAMEBUFFER = apiSupported ? (apiGL30 ? GL30.GL_READ_FRAMEBUFFER : ARBFramebufferObject.GL_READ_FRAMEBUFFER) : -1;
            GL_DRAW_FRAMEBUFFER = apiSupported ? (apiGL30 ? GL30.GL_DRAW_FRAMEBUFFER : ARBFramebufferObject.GL_DRAW_FRAMEBUFFER) : -1;

            GL_STENCIL_ATTACHMENT = apiSupported ? (apiGL30 ? GL30.GL_STENCIL_ATTACHMENT : ARBFramebufferObject.GL_STENCIL_ATTACHMENT) : -1;
            GL_DEPTH_STENCIL_ATTACHMENT = apiSupported ? (apiGL30 ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : ARBFramebufferObject.GL_DEPTH_STENCIL_ATTACHMENT) : -1;
            GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME =
                    apiSupported ? (apiGL30 ? GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME : ARBFramebufferObject.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME) : -1;

            GL_STENCIL_INDEX8 = apiSupported ? (apiGL30 ? GL30.GL_STENCIL_INDEX8 : ARBFramebufferObject.GL_STENCIL_INDEX8) : -1;
        }

        static void copyDepthFrom(int src, int tar, int width, int height) {
            copyFrom(src, tar, GL_DEPTH_BUFFER_BIT, width, height);
        }

        static void copyColorDepthFrom(int src, int tar, int width, int height) {
            copyFrom(src, tar, GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT, width, height);
        }

        static void copyFrom(int src, int tar, int masks, int width, int height) {

            int id;
            if (apiGL30) {
                id = GlStateManager.glGetInteger(GL_FRAMEBUFFER_BINDING);
                GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, src);
                GL30.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, tar);

                GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, masks, GL_NEAREST);
            } else {
                id = GlStateManager.glGetInteger(GL_FRAMEBUFFER_BINDING);
                ARBFramebufferObject.glBindFramebuffer(GL_READ_FRAMEBUFFER, src);
                ARBFramebufferObject.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, tar);

                ARBFramebufferObject.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, masks, GL_NEAREST);
            }
            OpenGlHelper.glBindFramebuffer(GL_FRAMEBUFFER, id);
        }

        static int getCurrentFramebuffer() {
            return apiSupported ? GlStateManager.glGetInteger(GL_FRAMEBUFFER_BINDING) : -1;
        }

        static int glGetFramebufferAttachmentParameteri(int target, int attachment, int pname) {
            if (apiSupported) {
                return apiGL30 ? GL30.glGetFramebufferAttachmentParameteri(target, attachment, pname) : ARBFramebufferObject.glGetFramebufferAttachmentParameteri(target, attachment, pname);
            } else {
                return -1;
            }
        }
    }

}
