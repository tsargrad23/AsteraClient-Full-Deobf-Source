package me.lyrica.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lyrica.events.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

@Getter @AllArgsConstructor
public class RenderOverlayEvent extends Event {
    private final DrawContext context;
    private final float tickDelta;

    public MatrixStack getMatrices() {
        return context.getMatrices();
    }
}
