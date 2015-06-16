package fr.scarex.updater.client.gui;

import java.util.Map.Entry;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.client.GuiScrollingList;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import fr.scarex.updater.Updater;
import fr.scarex.updater.Updater.ModVersions;
import fr.scarex.updater.Updater.Version;
import fr.scarex.updater.utils.Grouper;

public class GuiSlotVersionSelected extends GuiScrollingList
{
	protected GuiSelectedMods parent;

	public GuiSlotVersionSelected(int width, GuiSelectedMods parent, int top) {
		super(parent.mc, width, parent.height, top, parent.height - 84, 10, 35);
		this.parent = parent;
	}

	@Override
	protected int getSize() {
		return Updater.filesToDownload.size();
	}

	@Override
	protected void elementClicked(int var1, boolean var2) {
		this.parent.selectVersion(var1);
	}

	@Override
	protected boolean isSelected(int var1) {
		return this.parent.isSelected(var1);
	}

	@Override
	protected void drawBackground() {}

	@Override
	protected int getContentHeight() {
		return (this.getSize()) * 35 + 1;
	}

	@Override
	protected void drawSlot(int listIndex, int var2, int var3, int var4, Tessellator var5) {
		Entry<String, Grouper<ModVersions, Version>> entry = getEntryForIndex(listIndex);
		this.parent.getFontRenderer().drawString(this.parent.getFontRenderer().trimStringToWidth(Loader.instance().getIndexedModList().get(entry.getKey()).getName(), listWidth - 10), this.left + 3, var3 + 2, 0xFFFFFF);
		this.parent.getFontRenderer().drawString(this.parent.getFontRenderer().trimStringToWidth(entry.getValue().getSecondValue().getName(), listWidth - 10), this.left + 3, var3 + 14, 0xCCCCCC);
	}
	
	public Entry<String, Grouper<ModVersions, Version>> getEntryForIndex(int i) {
		return Updater.filesToDownload.entrySet().toArray(new Entry[0])[i];
	}
}
