package me.lyrica.modules.impl.combat;

import me.lyrica.Lyrica;
import me.lyrica.events.SubscribeEvent;
import me.lyrica.events.impl.PlayerPopEvent;
import me.lyrica.events.impl.PlayerUpdateEvent;
import me.lyrica.events.impl.TickEvent;
import me.lyrica.modules.Module;
import me.lyrica.modules.RegisterModule;
import me.lyrica.modules.impl.player.SpeedMineModule;
import me.lyrica.settings.impl.BooleanSetting;
import me.lyrica.settings.impl.ModeSetting;
import me.lyrica.settings.impl.NumberSetting;
import me.lyrica.utils.minecraft.InventoryUtils;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.MathHelper;

@RegisterModule(name = "AutoTotem", description = "Automatically puts a specified item in your offhand slot.", category = Module.Category.COMBAT)
public class AutoTotemModule extends Module {
    public ModeSetting item = new ModeSetting("Item", "The item that will be placed in your offhand slot when safety conditions are met.", "Totem", new String[]{"Totem", "Crystal", "Gapple"});
    public NumberSetting health = new NumberSetting("Health", "The health at which a totem will be prioritized.", new ModeSetting.Visibility(item, "Crystal", "Gapple"), 16, 0, 36);
    public BooleanSetting elytraCheck = new BooleanSetting("ElytraCheck", "Prioritizes a totem whenever you're wearing an elytra.", true);
    public NumberSetting fallDistance = new NumberSetting("FallDistance", "The fall distance at which the module will prioritize a totem.", 20.0f, 0.0f, 80.0f);
    public BooleanSetting useGapple = new BooleanSetting("UseGapple", "Switches to a golden apple in your offhand when holding right click and holding a sword.", true);
    public BooleanSetting lethalOverride = new BooleanSetting("LethalOverride", "Overrides any necessity for a totem when right-click gappling.", new BooleanSetting.Visibility(useGapple, true), false);
    public BooleanSetting tickAbort = new BooleanSetting("TickAbort", "Enable the interval between switching item which is determine by player ping", true);
    public BooleanSetting smartMine = new BooleanSetting("SmartMine", "Switches to a crystal whenever you start mining and a totem when you aren't mining.", new ModeSetting.Visibility(item, "Crystal"), false);
    public BooleanSetting antiMace = new BooleanSetting("AntiMace", "Switches to a totem if a player near you is trying to smash attack you with a mace.", false);
    public NumberSetting maceRange = new NumberSetting("MaceRange", "The distance at which an enemy has to be in with a mace in order to swap to a totem.", new BooleanSetting.Visibility(antiMace, true), 12.0f, 0.0f, 24.0f);

    private int totemCount = 0;
    private int ticks = 0;

    @SubscribeEvent
    public void onPlayerPop(PlayerPopEvent event) {
        if (event.getPlayer() == mc.player && !Lyrica.MODULE_MANAGER.getModule(SuicideModule.class).isToggled()) {
            ticks = 0;
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (ticks > 0 && tickAbort.getValue()) {
            ticks--;
            return;
        }

        if (!(mc.currentScreen instanceof InventoryScreen) && mc.currentScreen instanceof HandledScreen<?>)
            return;

        Item item = getItem();
        if (item == null) return;

        int slot;

        if (item == Items.TOTEM_OF_UNDYING && Lyrica.MODULE_MANAGER.getModule(SuicideModule.class).isToggled() && Lyrica.MODULE_MANAGER.getModule(SuicideModule.class).offhandOverride.getValue()) {
            if (mc.player.getOffHandStack().isEmpty()) return;

            slot = InventoryUtils.findEmptySlot(InventoryUtils.HOTBAR_START, InventoryUtils.INVENTORY_END);
        } else {
            if (mc.player.getOffHandStack().getItem() == item) return;

            slot = InventoryUtils.findInventory(item);
            if (slot == -1) slot = InventoryUtils.find(item);

            if (slot == -1) {
                if (item == Items.TOTEM_OF_UNDYING) slot = InventoryUtils.findEmptySlot(InventoryUtils.HOTBAR_START, InventoryUtils.INVENTORY_END);
                else return;
            }
        }

        if (slot == -1) return;

        InventoryUtils.swap("Pickup", slot, 45);
        ticks = 2 + Lyrica.SERVER_MANAGER.getPingDelay();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        totemCount = mc.player.getInventory().count(Items.TOTEM_OF_UNDYING);
    }

    private Item getItem() {
        if (useGapple.getValue() && mc.options.useKey.isPressed() && (lethalOverride.getValue() || !needsTotem()) && (mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem) && hasItem(Items.ENCHANTED_GOLDEN_APPLE))
            return Items.ENCHANTED_GOLDEN_APPLE;

        if (hasItem(Items.TOTEM_OF_UNDYING)) {
            if (needsTotem()) return Items.TOTEM_OF_UNDYING;

            if (item.getValue().equalsIgnoreCase("Crystal") && smartMine.getValue()) {
                SpeedMineModule module = Lyrica.MODULE_MANAGER.getModule(SpeedMineModule.class);
                if ((module.getPrimary() == null || !module.getPrimary().isMining()) && (module.getSecondary() == null || !module.getSecondary().isMining())) {
                    return Items.TOTEM_OF_UNDYING;
                }
            }
        }

        switch (item.getValue()) {
            case "Crystal" -> {
                if (!hasItem(Items.END_CRYSTAL)) return Items.TOTEM_OF_UNDYING;
                return Items.END_CRYSTAL;
            }
            case "Gapple" -> {
                if (!hasItem(Items.ENCHANTED_GOLDEN_APPLE)) return Items.TOTEM_OF_UNDYING;
                return Items.ENCHANTED_GOLDEN_APPLE;
            }
            default -> {
                return Items.TOTEM_OF_UNDYING;
            }
        }
    }

    private boolean needsTotem() {
        if (Lyrica.MODULE_MANAGER.getModule(SuicideModule.class).isToggled() && Lyrica.MODULE_MANAGER.getModule(SuicideModule.class).offhandOverride.getValue()) return false;

        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() <= health.getValue().floatValue()) return true;
        if (mc.player.fallDistance > fallDistance.getValue().floatValue()) return true;
        if (elytraCheck.getValue() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) return true;

        return antiMace.getValue() && (mc.world.getPlayers().stream().anyMatch(entity -> entity != mc.player && !Lyrica.FRIEND_MANAGER.contains(entity.getName().getString()) && mc.player.squaredDistanceTo(entity) <= MathHelper.square(maceRange.getValue().floatValue()) && entity.fallDistance >= 1.5 && entity.getMainHandStack().getItem().equals(Items.MACE)));
    }

    private boolean hasItem(Item item) {
        return InventoryUtils.find(item) != -1 || mc.player.getOffHandStack().getItem() == item;
    }

    @Override
    public String getMetaData() {
        return String.valueOf(totemCount);
    }
}
