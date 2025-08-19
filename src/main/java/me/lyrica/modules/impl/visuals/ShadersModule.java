package me.lyrica.modules.impl.visuals;

import me.lyrica.Lyrica;
import me.lyrica.events.SubscribeEvent;
import me.lyrica.events.impl.RenderEntityEvent;
import me.lyrica.events.impl.RenderHandEvent;
import me.lyrica.events.impl.RenderShaderEvent;
import me.lyrica.modules.RegisterModule;
import me.lyrica.modules.Module;
import me.lyrica.settings.impl.*;
import me.lyrica.utils.color.ColorUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;

@RegisterModule(name = "Shaders", description = "Overlays specified entities with a customizable shader.", category = Module.Category.VISUALS)
public class ShadersModule extends Module {
    public CategorySetting targets = new CategorySetting("Targets", "The things that the shader rendering will be applied onto.");
    public BooleanSetting players = new BooleanSetting("Players", "Renders the shader effect on player entities.", new CategorySetting.Visibility(targets), true);
    public BooleanSetting hostiles = new BooleanSetting("Hostiles", "Renders the shader effect on hostile entities.", new CategorySetting.Visibility(targets), true);
    public BooleanSetting animals = new BooleanSetting("Animals", "Renders the shader effect on animal entities.", new CategorySetting.Visibility(targets), true);
    public BooleanSetting ambient = new BooleanSetting("Ambient", "Renders the shader effect on ambient entities.", new CategorySetting.Visibility(targets), false);
    public BooleanSetting invisibles = new BooleanSetting("Invisibles", "Renders the shader effect on invisible entities.", new CategorySetting.Visibility(targets), true);
    public BooleanSetting items = new BooleanSetting("Items", "Renders the shader effect on item entities.", new CategorySetting.Visibility(targets), true);
    public BooleanSetting crystals = new BooleanSetting("Crystals", "Renders the shader effect on crystal entities.", new CategorySetting.Visibility(targets), true);
    public BooleanSetting others = new BooleanSetting("Others", "Renders the shader effect on miscellaneous entities.", new CategorySetting.Visibility(targets), false);
    public BooleanSetting hands = new BooleanSetting("Hands", "Renders the shader effect on your hands.", new CategorySetting.Visibility(targets), true);

    public ModeSetting mode = new ModeSetting("Mode", "The rendering that will be applied to the target block.", "Both", new String[]{"Fill", "Outline", "Both"});
    public NumberSetting opacity = new NumberSetting("Opacity", "The opacity of the shader effect.", new ModeSetting.Visibility(mode, "Fill", "Both"), 80, 0, 255);
    public ColorSetting color = new ColorSetting("Color", "The color that will be used for the fill rendering.", ColorUtils.getDefaultColor());
    public ModeSetting friends = new ModeSetting("Friends",  "The color that will be applied to friended entities.", "Default", new String[]{"Default", "Custom", "Sync"});
    public ColorSetting friendColor = new ColorSetting("FriendColor", "The color that will be used for the shader effect on friends.", new ModeSetting.Visibility(friends, "Custom"), new ColorSetting.Color(new Color(85, 255, 255, ColorUtils.getDefaultFillColor().getColor().getAlpha()), false, false));
    public NumberSetting lineWidth = new NumberSetting("LineWidth", "Outline kalınlığı", new ModeSetting.Visibility(mode, "Outline", "Both"), 2, 1, 10);

    public CategorySetting glow = new CategorySetting("Glow", "glow iste");
    public NumberSetting glowRadius = new NumberSetting("GlowRadius", "Glow yarıçapı (0-20)", new CategorySetting.Visibility(glow), 10, 1, 20);
    public NumberSetting glowIntensity = new NumberSetting("GlowIntensity", "Glow parlaklık katsayısı (0.5-3.0)", new CategorySetting.Visibility(glow), 1.2, 0.5, 3.0);
    public NumberSetting glowExponent = new NumberSetting("GlowFalloff", "Glow düşüş sertliği (0.8-3.0)", new CategorySetting.Visibility(glow), 1.4, 0.8, 3.0);
    public NumberSetting outlineStrength = new NumberSetting("OutlineBrightness", "Kontur parlaklık katsayısı (1.0-3.0)", new CategorySetting.Visibility(glow), 1.8, 1.0, 3.0);

    @SubscribeEvent
    public void onRenderShader(RenderShaderEvent event) {
        if (mc.player == null || mc.world == null) return;

        Lyrica.SHADER_MANAGER.prepare();
    }

    @SubscribeEvent
    public void onRenderEntity(RenderEntityEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!isValidEntity(event.getEntity())) return;

        Color color = (event.getEntity() instanceof PlayerEntity player && Lyrica.FRIEND_MANAGER.contains(player.getName().getString()) && !friends.getValue().equals("Sync")) ? (friends.getValue().equals("Default") ? Lyrica.FRIEND_MANAGER.getDefaultFriendColor() : friendColor.getColor()) : this.color.getColor();
        event.setVertexConsumers(Lyrica.SHADER_MANAGER.create(event.getVertexConsumers(), color));
    }

    @SubscribeEvent
    public void onRenderEntity$POST(RenderEntityEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (players.getValue() || hostiles.getValue() || animals.getValue() || ambient.getValue() || invisibles.getValue() || items.getValue() || crystals.getValue() || others.getValue()) {
            Lyrica.SHADER_MANAGER.getVertexConsumerProvider().draw();
        }

        if (!hands.getValue()) Lyrica.SHADER_MANAGER.render(mode.getValue().equalsIgnoreCase("Fill") ? 0 : mode.getValue().equalsIgnoreCase("Outline") ? 1 : 2, opacity.getValue().intValue() / 255.0f);
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!hands.getValue()) return;

        event.setVertexConsumers(Lyrica.SHADER_MANAGER.create(event.getVertexConsumers(), color.getColor()));
    }

    @SubscribeEvent
    public void onRenderHand$POST(RenderHandEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (!hands.getValue()) return;

        Lyrica.SHADER_MANAGER.getVertexConsumerProvider().draw();
    }

    @SubscribeEvent
    public void onRenderShader$POST(RenderShaderEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (!hands.getValue()) return;

        Lyrica.SHADER_MANAGER.render(mode.getValue().equalsIgnoreCase("Fill") ? 0 : mode.getValue().equalsIgnoreCase("Outline") ? 1 : 2, opacity.getValue().intValue() / 255.0f);
    }

    public boolean isValidEntity(Entity entity) {
        if (players.getValue() && entity.getType() == EntityType.PLAYER) return true;
        if (hostiles.getValue() && entity.getType().getSpawnGroup() == SpawnGroup.MONSTER) return true;
        if (animals.getValue() && (entity.getType().getSpawnGroup() == SpawnGroup.CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.WATER_CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.WATER_AMBIENT || entity.getType().getSpawnGroup() == SpawnGroup.UNDERGROUND_WATER_CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.AXOLOTLS))
            return true;
        if (ambient.getValue() && entity.getType().getSpawnGroup() == SpawnGroup.AMBIENT) return true;
        if (invisibles.getValue() && entity.isInvisible()) return true;
        if (items.getValue() && (entity.getType() == EntityType.ITEM || entity.getType() == EntityType.EXPERIENCE_BOTTLE)) return true;
        if (crystals.getValue() && entity.getType() == EntityType.END_CRYSTAL) return true;
        return others.getValue();
    }
}