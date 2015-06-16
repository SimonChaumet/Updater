package fr.scarex.updater.client.gui;

import java.util.Map.Entry;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.client.GuiScrollingList;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import fr.scarex.updater.Updater;
import fr.scarex.updater.Updater.ModVersions;

public class GuiSlotUpdater extends GuiScrollingList
{
	protected GuiUpdater parent;

	public GuiSlotUpdater(int width, GuiUpdater parent, int top) {
		super(parent.mc, width, parent.height, top, parent.height - 84, 10, 35);
		this.parent = parent;
	}

	@Override
	protected int getSize() {
		return Updater.modsList.size();
	}

	@Override
	protected void elementClicked(int var1, boolean var2) {
		this.parent.selectModIndex(var1);
	}

	@Override
	protected boolean isSelected(int var1) {
		return this.parent.modIndexSelected(var1);
	}

	@Override
	protected void drawBackground() {}

	@Override
	protected int getContentHeight() {
		return (this.getSize()) * 35 + 1;
	}

	@Override
	protected void drawSlot(int listIndex, int var2, int var3, int var4, Tessellator var5) {
		ModVersions modV = Updater.modsList.get(listIndex);
		this.parent.getFontRenderer().drawString(this.parent.getFontRenderer().trimStringToWidth(Loader.instance().getIndexedModList().get(modV.getModID()).getName(), listWidth - 10), this.left + 3, var3 + 2, 0xFFFFFF);
		this.parent.getFontRenderer().drawString(this.parent.getFontRenderer().trimStringToWidth(modV.getCurrentVersion().toName(), listWidth - 10), this.left + 3, var3 + 12, 0xCCCCCC);
	}

	public int getRightSide() {
		return this.right;
	}
}
