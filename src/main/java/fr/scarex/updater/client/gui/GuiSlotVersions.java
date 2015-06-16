package fr.scarex.updater.client.gui;

import java.util.Map.Entry;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.client.GuiScrollingList;
import scala.actors.threadpool.Arrays;
import fr.scarex.updater.Updater;
import fr.scarex.updater.Updater.Version;

public class GuiSlotVersions extends GuiScrollingList
{
	protected GuiUpdater parent;

	public GuiSlotVersions(int width, GuiUpdater parent, int left, int top) {
		super(parent.mc, width, parent.height, top, parent.height - 84, left, 35);
		this.parent = parent;
	}

	@Override
	protected int getSize() {
		return ((Version[]) Updater.modsList.get(this.parent.selectedModIndex).getVersions().entrySet().toArray(new Entry[0])[this.parent.selectedMCVersionIndex].getValue()).length;
	}

	@Override
	protected void elementClicked(int index, boolean doubleClick) {
		this.parent.selectVersionIndex(index);
	}

	@Override
	protected boolean isSelected(int index) {
		return this.parent.versionIndexSelected(index);
	}

	@Override
	protected void drawBackground() {}

	@Override
	protected void drawSlot(int index, int var2, int var3, int var4, Tessellator var5) {
		Version version = ((Version[]) Updater.modsList.get(this.parent.selectedModIndex).getVersions().entrySet().toArray(new Entry[0])[this.parent.selectedMCVersionIndex].getValue())[index];
		this.parent.getFontRenderer().drawString((version.getImportance() > 2 ? EnumChatFormatting.UNDERLINE : "") + this.parent.getFontRenderer().trimStringToWidth(version.getName(), listWidth - 10), this.left + 3, var3 + 2, Updater.filesToDownload.containsKey(this.parent.selectedMod) && Updater.filesToDownload.get(this.parent.selectedMod).getSecondValue().equals(version) ? 0x33FF33 : ((version.isSnapshot() && !this.parent.allowSnapshots) || version.getImportance() < 0 ? 0xFF1111 : 0xFFFFFF));
		this.parent.getFontRenderer().drawString(this.parent.getFontRenderer().trimStringToWidth(version.toName(), listWidth - 10), this.left + 3, var3 + 12, (version.isSnapshot() && !this.parent.allowSnapshots) || version.getImportance() < 0 ? 0xFF4444 : 0xFFFFFF);
	}

	@Override
	protected int getContentHeight() {
		return (this.getSize()) * 35 + 1;
	}

	public int getLeftSide() {
		return this.left;
	}

	public int getWidth() {
		return this.listWidth;
	}
}
