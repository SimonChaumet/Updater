package fr.scarex.updater.client.gui;

import java.io.IOException;
import java.net.URL;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import fr.scarex.updater.Updater;
import fr.scarex.updater.Updater.ModVersions;
import fr.scarex.updater.Updater.Version;
import fr.scarex.updater.utils.Grouper;

public class GuiUpdater extends GuiScreen
{
	protected GuiScreen lastScreen;
	protected String selectedMod;
	protected int selectedModIndex = -1;
	protected GuiSlotUpdater modList;
	protected GuiSlotVersions versionList;
	protected int selectedVersionIndex = -1;
	protected Version selectedVersion;
	protected GuiSlotMCVersions mcversionList;
	protected int selectedMCVersionIndex = -1;
	protected String selectedMCVersion;
	protected GuiButton switchSnapshotsButton;
	protected GuiButton downloadLatestVButton;
	protected GuiButton downloadCurrentVButton;
	public boolean allowSnapshots = false;

	public GuiUpdater(GuiScreen g) {
		this.lastScreen = g;
	}

	@Override
	public void initGui() {
		this.modList = new GuiSlotUpdater(110, this, 90);
		this.modList.registerScrollButtons(this.buttonList, 10, 11);
		this.mcversionList = new GuiSlotMCVersions(40, this, 130, 90);
		this.mcversionList.registerScrollButtons(this.buttonList, 12, 13);
		this.versionList = new GuiSlotVersions(140, this, 180, 90);
		this.versionList.registerScrollButtons(this.buttonList, 14, 15);

		this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height - 38, I18n.format("gui.done")));
		this.buttonList.add(this.switchSnapshotsButton = new GuiButton(1, 20, 10, 180, 20, I18n.format("updater.gui.switchSnapshots")));
		this.buttonList.add(new GuiButton(2, 220, 10, 120, 20, I18n.format("selectServer.refresh")));
		this.buttonList.add(new GuiButton(3, 20, 40, 240, 20, I18n.format("updater.gui.showSelectedMods")));
		this.buttonList.add(new GuiButton(4, 360, 10, 250, 20, I18n.format("updater.gui.selectAll")));
		this.buttonList.add(new GuiButton(5, 360, 40, 250, 20, I18n.format("updater.gui.deselectAll")));
		int minWidth = this.versionList.getLeftSide() + this.versionList.getWidth() + 20;
		this.buttonList.add(this.downloadLatestVButton = new GuiButton(6, minWidth, 204, 200, 20, I18n.format("updater.gui.selectLatestV")));
		this.buttonList.add(this.downloadCurrentVButton = new GuiButton(7, minWidth, 280, 200, 20, I18n.format("updater.gui.selectCurrentV")));
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
		case 0:
			back();
			break;
		case 1:
			allowSnapshots = !allowSnapshots;
			break;
		case 2:
			this.selectedVersion = null;
			this.selectedVersionIndex = -1;
			this.selectedMCVersion = null;
			this.selectedMCVersionIndex = -1;
			this.selectedMod = null;
			this.selectedModIndex = -1;
			Updater.filesToDownload.clear();
			Updater.doUpdate();
			break;
		case 3:
			this.mc.displayGuiScreen(new GuiSelectedMods(this));
			break;
		case 4:
			Updater.filesToDownload.clear();
			for (ModVersions modV : Updater.modsList) {
				Version v = modV.getLatestVersionForUser((byte) 0, this.allowSnapshots);
				if (v != null) {
					Updater.filesToDownload.put(modV.getModID(), new Grouper(modV, v));
				}
			}
			break;
		case 5:
			Updater.filesToDownload.clear();
			break;
		case 6:
			URL latestVersionLink = Updater.modsList.get(this.selectedModIndex).getDownloadLinkForVersion(Updater.modsList.get(this.selectedModIndex).getLatestVersionForUser((byte) 0, this.allowSnapshots), Updater.modsList.get(this.selectedModIndex).getMCVersionForLatestVersion());
			if (Updater.isRemoteFileAccessibleWithType(latestVersionLink, "*")) {
				ModVersions m = Updater.modsList.get(this.selectedModIndex);
				Updater.filesToDownload.put(m.getModID(), new Grouper<ModVersions, Version>(m, m.getLatestVersionForUser((byte) 0, this.allowSnapshots)));
			} else {
				Updater.logger.warn("Remote file is not accessible, try with another version or report to the mod author");
				Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("game.player.hurt.fall.big")));
			}
			break;
		case 7:
			URL link = Updater.modsList.get(this.selectedModIndex).getDownloadLinkForVersion(this.selectedVersion, this.selectedMCVersion);
			if (Updater.isRemoteFileAccessibleWithType(link, "*")) {
				ModVersions m = Updater.modsList.get(this.selectedModIndex);
				Updater.filesToDownload.put(m.getModID(), new Grouper<ModVersions, Version>(m, this.selectedVersion));
			} else {
				Updater.logger.warn("Remote file is not accessible, try with another version or report to the mod author");
				Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("game.player.hurt.fall.big")));
			}
			break;
		}
	}

	protected float drawLine(String text, float x, float y, int color, boolean shadow, float scale) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0.0F);
		GlStateManager.scale(scale, scale, scale);
		this.fontRendererObj.drawString(text, 0.0F, 0.0F, color, shadow);
		GlStateManager.popMatrix();
		return y + (10.0F * scale) + 2.0F;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.modList.drawScreen(mouseX, mouseY, partialTicks);
		if (selectedMod != null) this.mcversionList.drawScreen(mouseX, mouseY, partialTicks);
		if (selectedMCVersion != null) this.versionList.drawScreen(mouseX, mouseY, partialTicks);
		float minWidth = this.versionList.getLeftSide() + this.versionList.getWidth() + 20;
		float offsetY = 100.0F;
		if (selectedMod != null) {
			offsetY = drawLine(I18n.format("updater.gui.name", Loader.instance().getIndexedModList().get(selectedMod).getName()), minWidth, offsetY, 0xEEEEEE, false, 2.0F);
			offsetY = drawLine(I18n.format("updater.gui.modid", selectedMod), minWidth, offsetY, 0xAAAAAA, false, 1.2F);
			offsetY += 20.0F;
			offsetY = drawLine(I18n.format("updater.gui.latestVersion"), minWidth, offsetY, 0xFFFFFF, false, 1.4F);
			Version latestV = Updater.modsList.get(this.selectedModIndex).getLatestVersionForUser((byte) 0, this.allowSnapshots);
			if (latestV != null) {
				this.downloadLatestVButton.visible = true;
				offsetY = drawLine(I18n.format("updater.gui.name", latestV.getName()), minWidth, offsetY, 0xDDDDDD, false, 0.8F);
				offsetY = drawLine(I18n.format("updater.gui.version", latestV.toName()), minWidth, offsetY, 0xDDDDDD, false, 0.8F);
				offsetY = drawLine(I18n.format("updater.gui.changes", latestV.getChanges()), minWidth, offsetY, 0xDDDDDD, false, 0.8F);
			} else {
				this.downloadLatestVButton.visible = false;
			}

			offsetY = 230.0F;
			if (selectedVersion != null) {
				this.downloadCurrentVButton.visible = true;
				offsetY = drawLine(I18n.format("updater.gui.selectedVersion"), minWidth, offsetY, selectedVersion.isSnapshot() && !this.allowSnapshots ? 0xFF0000 : 0xFFFFFF, false, 1.4F);
				int color = selectedVersion.isSnapshot() && !this.allowSnapshots ? 0xFF2222 : 0xDDDDDD;
				offsetY = drawLine(I18n.format("updater.gui.userType", I18n.format("updater.userType." + selectedVersion.getUserType())), minWidth, offsetY, color, false, 0.8F);
				offsetY = drawLine("Snapshot : " + I18n.format("general." + selectedVersion.isSnapshot()), minWidth, offsetY, color, false, 0.8F);
				offsetY = drawLine(I18n.format("updater.gui.changes", selectedVersion.getChanges()), minWidth, offsetY, color, false, 0.8F);
			} else {
				this.downloadCurrentVButton.visible = false;
			}
		} else {
			this.downloadLatestVButton.visible = false;
			this.downloadCurrentVButton.visible = false;
		}
		this.switchSnapshotsButton.displayString = (allowSnapshots ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_RED) + I18n.format("updater.gui.switchSnapshots");
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	protected void back() {
		this.mc.displayGuiScreen(lastScreen);
	}

	public void selectModIndex(int i) {
		this.selectedModIndex = i;
		this.selectedMCVersionIndex = -1;
		this.selectedMCVersion = null;
		this.selectedVersionIndex = -1;
		this.selectedVersion = null;
		if (i >= 0 && i <= Updater.modsList.size()) {
			this.selectedMod = Updater.modsList.get(selectedModIndex).getModID();
		} else {
			this.selectedMod = null;
		}
	}

	public boolean modIndexSelected(int var1) {
		return var1 == selectedModIndex;
	}

	public void selectMCVersionIndex(int i) {
		this.selectedMCVersionIndex = i;
		this.selectedVersionIndex = -1;
		this.selectedVersion = null;
		if (i >= 0 && i <= Updater.modsList.get(selectedModIndex).getVersions().size()) {
			this.selectedMCVersion = Updater.modsList.get(selectedModIndex).getVersions().keySet().toArray(new String[0])[i];
		} else {
			this.selectedMCVersion = null;
		}
	}

	public boolean mcversionIndexSelected(int i) {
		return i == selectedMCVersionIndex;
	}

	public void selectVersionIndex(int i) {
		this.selectedVersionIndex = i;
		if (i >= 0 && i <= ((Version[]) Updater.modsList.get(selectedModIndex).getVersions().entrySet().toArray(new Entry[0])[this.selectedMCVersionIndex].getValue()).length) {
			this.selectedVersion = ((Version[]) Updater.modsList.get(selectedModIndex).getVersions().entrySet().toArray(new Entry[0])[this.selectedMCVersionIndex].getValue())[i];
		} else {
			this.selectedVersion = null;
		}
	}

	public boolean versionIndexSelected(int i) {
		return i == selectedVersionIndex;
	}

	public FontRenderer getFontRenderer() {
		return this.fontRendererObj;
	}
}
