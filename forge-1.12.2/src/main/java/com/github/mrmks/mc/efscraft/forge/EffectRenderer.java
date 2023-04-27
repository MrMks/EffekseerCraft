package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.efkseer4j.EfsEffectHandle;
import com.github.mrmks.efkseer4j.EfsProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.opengl.GL11.*;

public class EffectRenderer {

    public static boolean USE_GL_TRANSLATE = false;

    private final EfsProgramContainer container;
    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
    private final Queue<Node> playingNodes = new ConcurrentLinkedQueue<>();
    private final Map<String, Map<String, Node>> playingNodeLookup = new ConcurrentHashMap<>();
    private final EffectResourceManager effectRegistry;

    private static class Node {
        private final String effect, emitter;
        private final float[] rotLocal, pos, rotModel, scale;
        private final int skipFrame, lifespan;
        private float lifeLength;
        EfsEffectHandle effectHandle;

        boolean markStop, markStart;

        Node(String effect, String emitter, float[] rotLocal, float[] pos, float[] rotModel, float[] scale, int skip, int lifespan) {
            this.effect = effect;
            this.emitter = emitter;
            this.rotLocal = rotLocal;
            this.rotModel = rotModel;
            this.pos = pos;
            this.scale = scale;
            this.skipFrame = skip;
            this.lifespan = lifespan;

            this.lifeLength = 0;
            this.effectHandle = null;
            this.markStart = this.markStop = false;
        }

    }

    EffectRenderer(EfsProgramContainer container, EffectResourceManager effectRegistry) {
        this.container = container;
        this.effectRegistry = effectRegistry;
    }

    private EfsProgram getProgram(float partialTicks) {

        EfsProgram program = container.getOrInitialize(8000);

        float[] floats = new float[16], trans = new float[] { 0.5f, 0, 0.5f };

        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (view != null) {
            trans[0] += (view.prevPosX - view.posX) * partialTicks - view.prevPosX;
            trans[1] += (view.prevPosY - view.posY) * partialTicks - view.prevPosY + view.getEyeHeight();
            trans[2] += (view.prevPosZ - view.posZ) * partialTicks - view.prevPosZ;
        }
        if (USE_GL_TRANSLATE) {
            int mode = glGetInteger(GL_MATRIX_MODE);

            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glTranslated(trans[0], trans[1], trans[2]);

            glGetFloat(GL_MODELVIEW_MATRIX, buffer);
            glPopMatrix();
            glMatrixMode(mode);

            buffer.get(floats).clear();
        } else {
            glGetFloat(GL_MODELVIEW_MATRIX, buffer);
            buffer.get(floats).clear();
            for (int i = 0; i < 3; i++) {
                float t = 0;
                for (int j = 0; j < 3; j++) t += floats[i + 4 * j] * trans[j];
                floats[i + 12] += t;
            }
        }
        program.setCameraMatrix(floats);

        // update projection matrix
        glGetFloat(GL_PROJECTION_MATRIX, buffer);
        buffer.get(floats).clear();
        program.setProjectionMatrix(floats);

        return program;
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event) {

        float partial = event.getPartialTicks();

        EfsProgram program = getProgram(partial);
        if (!Minecraft.getMinecraft().isGamePaused()) {
            program.update(partial);

            // todo filter nodes;
        }

        program.draw();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // maybe we are not in render thread now, so we request commands to be proceeded;
        synchronized (playingNodes) {
            // todo proceed nodes;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        stopAll();
    }

    private void stopAll() {
        EfsProgram program = container.get();
        if (program != null) {
            program.stopEffects();
            playingNodes.clear();
            playingNodeLookup.clear();
        }
    }

}
