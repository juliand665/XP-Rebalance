package xprebalance.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import xprebalance.XPUtil;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ForgingScreen<AnvilScreenHandler> {
	@Shadow
	@Final
	private PlayerEntity player;
	
	public AnvilScreenMixin(AnvilScreenHandler forgingScreenHandler, PlayerInventory playerInventory, Text text, Identifier identifier) {
		super(forgingScreenHandler, playerInventory, text, identifier);
	}
	
	/**
	 @author juliand665
	 @reason we're changing pretty much everything about how this works
	 */
	@Overwrite
	public void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
		RenderSystem.disableBlend();
		super.drawForeground(matrices, mouseX, mouseY);
		
		int levelCost = handler.getLevelCost();
		if (levelCost == 0) return;
		var outputSlot = handler.getSlot(2);
		if (!outputSlot.hasStack()) return; // no output
		
		int color = outputSlot.canTakeItems(player) ? 0x80FF20 : 0xFF6060;
		XPUtil.updateTotalXP(player);
		// TODO: localize
		var costDescription = new LiteralText(player.totalExperience + "/" + levelCost + " XP");
		int x = backgroundWidth - 8 - textRenderer.getWidth(costDescription);
		int y = 69;
		fill(matrices, x - 2, y - 2, backgroundWidth - 8, y + 10, 0x4F000000);
		textRenderer.drawWithShadow(matrices, costDescription, x, y, color);
	}
}
