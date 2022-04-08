package xprebalance.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xprebalance.*;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerEnchantmentApplier {
	@Shadow
	protected int enchantmentTableSeed;
	
	protected PlayerMixin() {
		//noinspection ConstantConditions
		super(null, null);
	}
	
	@Override
	public void xprebalance_applyEnchantmentCosts(int xp) {
		XPUtil.subtractXP((PlayerEntity) (LivingEntity) this, xp);
		enchantmentTableSeed = random.nextInt();
	}
	
	@Inject(
		at = @At("RETURN"),
		method = "getXpToDrop",
		cancellable = true
	)
	private void uncapDroppedXP(PlayerEntity killer, CallbackInfoReturnable<Integer> cir) {
		if (cir.getReturnValueI() == 0) return; // keep inventory/spectator/no xp
		
		var player = (PlayerEntity) (LivingEntity) this;
		var totalXP = XPUtil.updateTotalXP(player);
		var droppedXP = 0.8f * totalXP;
		cir.setReturnValue((int) droppedXP);
	}
}
