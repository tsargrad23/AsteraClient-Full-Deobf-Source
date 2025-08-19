package me.lyrica.modules.impl.player;

import me.lyrica.modules.Module;
import me.lyrica.modules.RegisterModule;
import me.lyrica.settings.impl.BooleanSetting;
import me.lyrica.settings.impl.ModeSetting;
import net.minecraft.item.Items;

@RegisterModule(name = "NoInteract", description = "Prevents you from interacting with right-clickable blocks.", category = Module.Category.PLAYER)
public class NoInteractModule extends Module {
    public ModeSetting mode = new ModeSetting("Mode", "The way that right-clickable blocks will be ignored.", "Sneak", new String[]{"Sneak", "Disable"});
    public BooleanSetting gapple = new BooleanSetting("Gapple", "Only disables interactions when holding a golden apple in your main hand.", false);

    @Override
    public String getMetaData() {
        return mode.getValue();
    }

    public boolean shouldNoInteract() {
        return !gapple.getValue() || mc.player.getMainHandStack().getItem().equals(Items.ENCHANTED_GOLDEN_APPLE);
    }
}
