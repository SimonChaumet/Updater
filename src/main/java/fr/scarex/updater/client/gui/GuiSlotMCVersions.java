package fr.scarex.updater.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.client.GuiScrollingList;
import fr.scarex.updater.Updater;
import fr.scarex.updater.Updater.Version;

public class GuiSlotMCVersions extends GuiScrollingList
{
	protected GuiUpdater parent;

	public GuiSlotMCVersions(int width, GuiUpdater parent, int left, int top) {
		super(parent.mc, width, parent.height, top, parent.height - 84, left, 20);
		this.parent = parent;
	}

	@Override
	protected int getSize() {
		return Updater.modsList.get(parent.selectedModIndex).getVersions().keySet().size();
	}

	@Override
	protected void elementClicked(int var1, boolean var2) {
		this.parent.selectMCVersionIndex(var1);
	}

	@Override
	protected boolean isSelected(int var1) {
		return this.parent.mcversionIndexSelected(var1);
	}

	@Override
	protected void drawBackground() {}

	@Override
	protected int getContentHeight() {
		return (this.getSize()) * 35 + 1;
	}

	@Override
	protected void drawSlot(int listIndex, int var2, int var3, int var4, Tessellator var5) {
		String mcversion = Updater.modsList.get(this.parent.selectedModIndex).getVersions().keySet().toArray(new String[0])[listIndex];
		this.parent.getFontRenderer().drawString(this.parent.getFontRenderer().trimStringToWidth(mcversion, listWidth - 10), this.left + 3, var3 + 2, (mcversion.substring(0, 3).equalsIgnoreCase(Minecraft.getMinecraft().getVersion().substring(0, 3)) ? 0xFFFFFF : 0xEE1010));
	}

	public int getRightSide() {
		return this.right;
	}
}
