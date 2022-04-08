package xprebalance.mixin;

import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xprebalance.*;

import java.util.List;
import java.util.stream.IntStream;

@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerMixin extends ScreenHandler implements EnchantmentCountAccessor {
	@Shadow
	@Final
	public int[] enchantmentPower;
	private final int[] xprebalance_enchantmentCount = new int[3];
	
	protected EnchantmentScreenHandlerMixin(ScreenHandlerType<?> screenHandlerType, int i) {
		super(screenHandlerType, i);
	}
	
	@Inject(at = @At("RETURN"), method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V")
	private void addProperties(CallbackInfo callbackInfo) {
		addProperty(Property.create(xprebalance_enchantmentCount, 0));
		addProperty(Property.create(xprebalance_enchantmentCount, 1));
		addProperty(Property.create(xprebalance_enchantmentCount, 2));
	}
	
	@Inject(at = @At("RETURN"), method = "generateEnchantments")
	private void onGenerateEnchantments(
		ItemStack stack, int slot, int level,
		CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir
	) {
		xprebalance_enchantmentCount[slot] = cir.getReturnValue().size();
	}
	
	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/player/PlayerEntity;applyEnchantmentCosts(Lnet/minecraft/item/ItemStack;I)V"
		),
		method = "m_ddudbryk" // lambda in onButtonClick
	)
	private void applyEnchantmentCosts(PlayerEntity player, ItemStack itemStack, int levels) {
		var slot = levels - 1;
		var minLevel = enchantmentPower[slot];
		var applier = (PlayerEnchantmentApplier) player;
		var xpCost = IntStream.range(minLevel - levels, minLevel)
			.map(level -> XPUtil.xpForLevel(player, level))
			.sum();
		applier.xprebalance_applyEnchantmentCosts(xpCost);
	}
	
	@Override
	public int xprebalance_getEnchantmentCount(int slot) {
		return xprebalance_enchantmentCount[slot];
	}
}
