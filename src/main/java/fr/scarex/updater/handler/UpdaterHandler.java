package fr.scarex.updater.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fr.scarex.updater.Updater;
import fr.scarex.updater.client.gui.GuiUpdater;

public class UpdaterHandler
{
	@SubscribeEvent
	public void onInitGuiEvent(final InitGuiEvent event) {
		if (event.gui instanceof GuiMainMenu) event.buttonList.add(new GuiButton(22, event.gui.width / 2 + 108, event.gui.height / 4 + 132, 20, 20, "") {
			protected final ResourceLocation buttonTexture = new ResourceLocation(Updater.MODID, "textures/gui/button_update_big.png");

			@Override
			public void mouseReleased(int mouseX, int mouseY) {
				event.gui.mc.displayGuiScreen(new GuiUpdater(event.gui));
			}

			@Override
			public void drawButton(Minecraft mc, int mouseX, int mouseY) {
				if (this.visible) {
					mc.getTextureManager().bindTexture(buttonTexture);
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					boolean flag = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
					int k = 0;
					if (flag) k += this.height;

					this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, k, 20, 20);
				}
			}
		});
	}
}
