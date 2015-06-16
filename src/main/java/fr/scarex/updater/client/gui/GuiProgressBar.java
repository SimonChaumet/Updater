package fr.scarex.updater.client.gui;

import java.util.regex.Matcher;

import fr.scarex.updater.Updater;
import fr.scarex.updater.Updater.ModVersions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;

public class GuiProgressBar extends Gui
{
	protected float x;
	protected float y;
	protected float width;
	protected float height;
	protected float outlineWidth;
	protected int color;
	protected int outlineColor;
	protected int stringColor;
	protected String displayString;
	protected boolean visible = true;
	protected long maxSize;
	protected long currentSize;

	public GuiProgressBar(float x, float y, float width, float height, long size, int color) {
		this(x, y, width, height, 1, size, color, 0xCCCCCC, "", 0xFFFFFFFF);
	}

	public GuiProgressBar(float x, float y, float width, float height, float outlineWidth, long size, int color, int outlineColor, String displayString, int stringColor) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.outlineWidth = outlineWidth;
		this.maxSize = size;
		this.color = color;
		this.outlineColor = outlineColor;
		this.displayString = displayString;
		this.stringColor = stringColor;
	}

	public void drawBar(Minecraft mc) {
		if (this.visible) {
			drawRectd(this.x, this.y, this.x + this.width, this.y + this.outlineWidth, this.outlineColor);
			drawRectd(this.x, this.y + this.height, this.x + this.width, this.y + this.height - this.outlineWidth, this.outlineColor);
			drawRectd(this.x, this.y, this.x + this.outlineWidth, this.y + this.height, this.outlineColor);
			drawRectd(this.x + this.width, this.y, this.x + this.width - this.outlineWidth, this.y + this.height, this.outlineColor);

			drawRectd(this.x + this.outlineWidth, this.y + this.outlineWidth, Math.max((double) this.x + (this.currentSize * this.width) / (this.maxSize == 0 ? 1 : this.maxSize) - this.outlineWidth, this.x + this.outlineWidth), this.y + this.height - this.outlineWidth, color);
			
			Matcher m = ModVersions.PARAMETERS_FINDER.matcher(this.displayString);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				if ("size".equalsIgnoreCase(m.group(1)))
					m.appendReplacement(sb, "" + this.currentSize);
				else if ("maxSize".equalsIgnoreCase(m.group(1)))
					m.appendReplacement(sb, "" + this.maxSize);
				else if ("percent".equalsIgnoreCase(m.group(1)))
					m.appendReplacement(sb, "" + Math.round((this.currentSize * 100) / (this.maxSize == 0 ? 1 : this.maxSize)));
			}
			m.appendTail(sb);
			mc.fontRendererObj.drawStringWithShadow(sb.toString(), this.x + (this.width - mc.fontRendererObj.getStringWidth(sb.toString())) / 2, this.y + (this.height - 8) / 2, this.stringColor);
		}
	}

	public void drawRectd(double left, double top, double right, double bottom, int color) {
		double j1;

		if (left < right) {
			j1 = left;
			left = right;
			right = j1;
		}

		if (top < bottom) {
			j1 = top;
			top = bottom;
			bottom = j1;
		}

		float f3 = (float) (color >> 24 & 255) / 255.0F;
		float f = (float) (color >> 16 & 255) / 255.0F;
		float f1 = (float) (color >> 8 & 255) / 255.0F;
		float f2 = (float) (color & 255) / 255.0F;
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(f, f1, f2, f3);
		worldrenderer.startDrawingQuads();
		worldrenderer.addVertex(left, bottom, 0.0D);
		worldrenderer.addVertex(right, bottom, 0.0D);
		worldrenderer.addVertex(right, top, 0.0D);
		worldrenderer.addVertex(left, top, 0.0D);
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	public void update(long l) {
		this.currentSize += l >= this.maxSize ? this.maxSize : l;
	}
	
	public void clear() {
		this.maxSize = 1;
		this.currentSize = 1;
	}
	
	public void setMaxSize(long l) {
		this.maxSize = l;
	}
	
	public boolean isFinished() {
		return this.currentSize >= this.maxSize;
	}

	public void addMaxSize(long l) {
		this.maxSize += l;
	}
}
