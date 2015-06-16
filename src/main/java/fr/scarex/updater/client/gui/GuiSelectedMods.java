package fr.scarex.updater.client.gui;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map.Entry;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Loader;

import org.apache.commons.io.FileUtils;

import fr.scarex.updater.Updater;
import fr.scarex.updater.Updater.ModVersions;
import fr.scarex.updater.Updater.Version;
import fr.scarex.updater.utils.Grouper;

public class GuiSelectedMods extends GuiScreen
{
	protected GuiScreen lastScreen;
	protected String selectedModID;
	protected GuiSlotVersionSelected versionsList;
	protected GuiButton deselectButton;
	protected GuiButton downloadButton;
	protected GuiButton autoExitButton;
	protected GuiProgressBar progressBar;
	protected static boolean autoExit = false;

	public GuiSelectedMods(GuiScreen lastScreen) {
		this.lastScreen = lastScreen;
	}

	@Override
	public void initGui() {
		this.versionsList = new GuiSlotVersionSelected(150, this, 50);
		this.versionsList.registerScrollButtons(this.buttonList, 10, 11);

		this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height - 38, I18n.format("gui.done")));
		this.buttonList.add(new GuiButton(1, 170, 60, 200, 20, I18n.format("updater.gui.downloadAll")));
		this.buttonList.add(deselectButton = new GuiButton(2, 170, 90, 200, 20, I18n.format("updater.gui.deselect")));
		this.buttonList.add(downloadButton = new GuiButton(3, 170, 120, 200, 20, I18n.format("updater.gui.download")));
		this.buttonList.add(autoExitButton = new GuiButton(4, 170, 150, 300, 20, I18n.format("updater.gui.autoExit")));
		this.progressBar = new GuiProgressBar(180.0F, 200.0F, 300.0F, 24.0F, 1.5F, 0, 0xFFFF2222, 0xFF444444, I18n.format("updater.gui.downloading") + " ({{size}}/{{maxSize}}, {{percent}}%)", 0xFFCCCCCC);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.versionsList.drawScreen(mouseX, mouseY, partialTicks);
		if (this.selectedModID != null) {
			this.deselectButton.visible = true;
			this.downloadButton.visible = true;
		} else {
			this.deselectButton.visible = false;
			this.downloadButton.visible = false;
		}
		this.autoExitButton.displayString = (autoExit ? EnumChatFormatting.DARK_GREEN : EnumChatFormatting.RED) + I18n.format("updater.gui.autoExit") + EnumChatFormatting.RESET;
		super.drawScreen(mouseX, mouseY, partialTicks);
		this.progressBar.drawBar(this.mc);
	}

	public void setMaxSize(long l) {
		this.progressBar.setMaxSize(l);
	}

	public void addMaxSize(long l) {
		this.progressBar.addMaxSize(l);
	}

	public void setSize(long l) {
		this.progressBar.update(l);
		if (!this.progressBar.visible) this.progressBar.visible = true;
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
		case 0:
			back();
			break;
		case 1:
			downloadAllVersions();
			Updater.filesToDownload.clear();
			if (autoExit) this.mc.shutdown();
			break;
		case 2:
			Updater.filesToDownload.remove(this.selectedModID);
			break;
		case 3:
			downloadSelectedVersion();
			if (autoExit) this.mc.shutdown();
			break;
		case 4:
			this.autoExit = !this.autoExit;
			break;
		}
	}

	public void downloadSelectedVersion() {
		new Thread("Download thread") {
			@Override
			public void run() {
				Grouper<ModVersions, Version> g = Updater.filesToDownload.get(GuiSelectedMods.this.selectedModID);
				Updater.filesToDownload.remove(GuiSelectedMods.this.selectedModID);
				URL link = g.getFirstValue().getDownloadLinkForVersion(g.getSecondValue(), null);
				try {
					URLConnection con = link.openConnection(Proxy.NO_PROXY);
					long l = con.getContentLengthLong();
					GuiSelectedMods.this.setMaxSize(l);
					FileUtils.copyInputStreamToFile(con.getInputStream(), new File(Updater.modFolder, link.getFile().replaceFirst("^/((.+/)*)", "")));
					Updater.appendFile(Loader.instance().getIndexedModList().get(g.getFirstValue().getModID()).getSource().getName());
					GuiSelectedMods.this.progressBar.update(l);
				} catch (IOException e) {
					Updater.logger.error("Couldn't get file", e);
				}
			}
		}.start();
	}

	public void downloadAllVersions() {
		Iterator<Entry<String, Grouper<ModVersions, Version>>> ite = Updater.filesToDownload.entrySet().iterator();
		long size;
		while (ite.hasNext()) {
			final Entry<String, Grouper<ModVersions, Version>> entry = ite.next();
			new Thread("Download Thread-" + entry.getValue().getFirstValue().getModID()) {
				@Override
				public void run() {
					try {
						URL link = entry.getValue().getFirstValue().getDownloadLinkForVersion(entry.getValue().getSecondValue(), null);
						if (Updater.isRemoteFileAccessibleWithType(link, "*")) {
							URLConnection con = link.openConnection();
							long l = con.getContentLengthLong();
							GuiSelectedMods.this.addMaxSize(l);
							FileUtils.copyInputStreamToFile(con.getInputStream(), new File(Updater.modFolder, link.getFile().replaceFirst("^/((.+/)*)", "")));
							Updater.appendFile(Loader.instance().getIndexedModList().get(entry.getValue().getFirstValue().getModID()).getSource().getName());
							GuiSelectedMods.this.progressBar.update(l);
						}
					} catch (Exception e) {
						Updater.logger.error("Couldn't download file", e);
					}
				}
			}.start();
		}
	}

	public void selectVersion(int i) {
		if (i >= 0 && i < Updater.filesToDownload.entrySet().toArray().length) {
			this.selectedModID = (String) Updater.filesToDownload.keySet().toArray(new String[0])[i];
		} else {
			this.selectedModID = null;
		}
	}

	public boolean isSelected(int i) {
		if (i >= 0 && i < Updater.filesToDownload.keySet().toArray().length)
			return ((String) Updater.filesToDownload.keySet().toArray(new String[0])[i]).equals(this.selectedModID);
		else
			return false;
	}

	protected void back() {
		this.mc.displayGuiScreen(lastScreen);
	}

	public FontRenderer getFontRenderer() {
		return this.fontRendererObj;
	}
}
