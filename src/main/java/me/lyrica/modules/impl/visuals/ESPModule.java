package me.lyrica.modules.impl.visuals;

import me.lyrica.events.SubscribeEvent;
import me.lyrica.events.impl.RenderWorldEvent;
import me.lyrica.events.impl.TickEvent;
import me.lyrica.modules.Module;
import me.lyrica.modules.RegisterModule;
import me.lyrica.settings.impl.BooleanSetting;
import me.lyrica.settings.impl.ColorSetting;
import me.lyrica.settings.impl.ModeSetting;
import me.lyrica.utils.color.ColorUtils;
import me.lyrica.utils.graphics.Renderer3D;
import me.lyrica.utils.minecraft.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

@RegisterModule(name = "ESP", description = "Renders a box ESP around entities that you have selected.", category = Module.Category.VISUALS)
public class ESPModule extends Module {
    public BooleanSetting players = new BooleanSetting("Players", "Renders the box ESP on player entities.", true);
    public BooleanSetting hostiles = new BooleanSetting("Hostiles", "Renders the box ESP on hostile entities.", true);
    public BooleanSetting animals = new BooleanSetting("Animals", "Renders the box ESP on animal entities.", true);
    public BooleanSetting ambient = new BooleanSetting("Ambient", "Renders the box ESP on ambient entities.", false);
    public BooleanSetting invisibles = new BooleanSetting("Invisibles", "Renders the box ESP on invisible entities.", true);
    public BooleanSetting items = new BooleanSetting("Items", "Renders the box ESP on item entities.", true);
    public BooleanSetting others = new BooleanSetting("Others", "Renders the box ESP on miscellaneous entities.", false);

    public ModeSetting mode = new ModeSetting("Mode", "The rendering that will be applied to the target entities.", "Both", new String[]{"None", "Fill", "Outline", "Both"});
    public ColorSetting fillColor = new ColorSetting("FillColor", "The color that will be used for the fill rendering.", new ModeSetting.Visibility(mode, "Fill", "Both"), ColorUtils.getDefaultFillColor());
    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "The color that will be used for the outline rendering.", new ModeSetting.Visibility(mode, "Outline", "Both"), ColorUtils.getDefaultOutlineColor());

    private List<Entity> targetEntities = new ArrayList<>();

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.world == null) return;

        List<Entity> targetEntities = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!isValidEntity(entity)) continue;

            targetEntities.add(entity);
        }

        this.targetEntities = targetEntities;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.world == null) return;
        if (targetEntities.isEmpty()) return;

        for (Entity entity : targetEntities) {
            Vec3d pos = EntityUtils.getRenderPos(entity, event.getTickDelta());
            Box box = new Box(pos.x - entity.getBoundingBox().getLengthX()/2, pos.y, pos.z - entity.getBoundingBox().getLengthZ()/2, pos.x + entity.getBoundingBox().getLengthX()/2, pos.y + entity.getBoundingBox().getLengthY(), pos.z + entity.getBoundingBox().getLengthZ()/2);

            if (mode.getValue().equalsIgnoreCase("Fill") || mode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBox(event.getMatrices(), box, fillColor.getColor());
            if (mode.getValue().equalsIgnoreCase("Outline") || mode.getValue().equalsIgnoreCase("Both")) Renderer3D.renderBoxOutline(event.getMatrices(), box, outlineColor.getColor());
        }
    }

    private boolean isValidEntity(Entity entity) {
        if (players.getValue() && entity.getType() == EntityType.PLAYER) return true;
        if (hostiles.getValue() && entity.getType().getSpawnGroup() == SpawnGroup.MONSTER) return true;
        if (animals.getValue() && (entity.getType().getSpawnGroup() == SpawnGroup.CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.WATER_CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.WATER_AMBIENT || entity.getType().getSpawnGroup() == SpawnGroup.UNDERGROUND_WATER_CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.AXOLOTLS))
            return true;
        if (ambient.getValue() && entity.getType().getSpawnGroup() == SpawnGroup.AMBIENT) return true;
        if (invisibles.getValue() && entity.isInvisible()) return true;
        if (items.getValue() && (entity.getType() == EntityType.ITEM || entity.getType() == EntityType.EXPERIENCE_BOTTLE)) return true;
        return others.getValue();
    }
}
