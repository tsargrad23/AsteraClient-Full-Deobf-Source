package me.lyrica.modules.impl.player;

import me.lyrica.Lyrica;
import me.lyrica.modules.Module;
import me.lyrica.modules.RegisterModule;
import me.lyrica.settings.impl.BooleanSetting;
import me.lyrica.settings.impl.ModeSetting;
import me.lyrica.utils.minecraft.InventoryUtils;
import me.lyrica.utils.minecraft.NetworkUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

@RegisterModule(name = "KeyPearl", description = "Automatically switches to pearls and throws them.", category = Module.Category.PLAYER)
public class ThrowPearlModule extends Module {
    public ModeSetting autoSwitch = new ModeSetting("Switch", "The mode that will be used for automatically switching to necessary items.", "Silent", InventoryUtils.SWITCH_MODES);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Sends a packet rotation right before throwing the pearl.", true);

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            setToggled(false);
            return;
        }

        if (autoSwitch.getValue().equalsIgnoreCase("None") && mc.player.getMainHandStack().getItem() != Items.ENDER_PEARL) {
            Lyrica.CHAT_MANAGER.tagged("You are currently not holding any pearls.", getName());
            setToggled(false);
            return;
        }

        if (mc.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))) {
            setToggled(false);
            return;
        }

        int slot = InventoryUtils.find(Items.ENDER_PEARL, 0, autoSwitch.getValue().equalsIgnoreCase("AltSwap") || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
        int previousSlot = mc.player.getInventory().selectedSlot;

        if (slot == -1) {
            Lyrica.CHAT_MANAGER.tagged("No pearls could be found in your hotbar.", getName());
            setToggled(false);
            return;
        }

        InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);

        Lyrica.ROTATION_MANAGER.packetRotate(mc.player.getYaw(), mc.player.getPitch());
        NetworkUtils.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);

        setToggled(false);
    }
}
