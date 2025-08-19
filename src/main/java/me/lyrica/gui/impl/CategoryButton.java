package me.lyrica.gui.impl;

import me.lyrica.Lyrica;
import me.lyrica.gui.api.Button;
import me.lyrica.gui.api.Frame;
import me.lyrica.settings.impl.CategorySetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class CategoryButton extends Button {
    private final CategorySetting setting;

    public CategoryButton(CategorySetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Lyrica.FONT_MANAGER.drawTextWithShadow(context, setting.getTag(), getX() + getTextPadding() + 1, getY() + 2, Color.WHITE);
        Lyrica.FONT_MANAGER.drawTextWithShadow(context, setting.isOpen() ? "-" : "+", getX() + getWidth() - getTextPadding() - 1 - Lyrica.FONT_MANAGER.getWidth(setting.isOpen() ? "-" : "+"), getY() + 2, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY) && button == 1) {
            setting.setOpen(!setting.isOpen());
            playClickSound();
        }
    }
}
