package xprebalance.mixin;

import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xprebalance.EnchantmentCountAccessor;

import java.util.List;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends HandledScreen<EnchantmentScreenHandler> {
	public EnchantmentScreenMixin() {
		//noinspection ConstantConditions
		super(null, null, null);
	}
	
	@Inject(
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/EnchantmentScreen;renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/List;II)V"),
		method = "render",
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void preRenderTooltip(
		MatrixStack matrices, int mouseX, int mouseY, float delta,
		CallbackInfo callbackInfo,
		boolean bl, int i, int slot, int k, Enchantment enchantment, int l, int m, List<Text> tooltipLines
	) {
		var accessor = (EnchantmentCountAccessor) handler;
		var count = accessor.xprebalance_getEnchantmentCount(slot);
		// TODO: localize
		var descriptor = count == 1 ? "Enchantment" : "Enchantments";
		tooltipLines.add(new LiteralText(count + " " + descriptor));
	}
}
