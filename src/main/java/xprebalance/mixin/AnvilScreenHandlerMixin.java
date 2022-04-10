package xprebalance.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.text.LiteralText;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xprebalance.XPUtil;

import java.util.HashSet;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {
	@Shadow
	@Final
	private Property levelCost;
	@Shadow
	private int repairItemUsage;
	@Shadow
	private String newItemName;
	
	public AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> screenHandlerType, int i, PlayerInventory playerInventory, ScreenHandlerContext screenHandlerContext) {
		super(screenHandlerType, i, playerInventory, screenHandlerContext);
	}
	
	/// cost to customize one item's name
	private static final int renameCostMultiplier = 1;
	/// cost multiplier for combining two enchantments into a new one 1 level higher
	private static final int upgradeCostMultiplier = 5;
	/// cost multiplier for repairing durability (composed with enchantDeltaCostMultiplier when not repairing with raw materials)
	private static final int repairCostMultiplier = 1;
	/// cost multiplier to "catch up" an item to the enchants of another, when its durability is used to repair
	private static final int enchantDeltaCostMultiplier = 10;
	/// multiplier for the "level cost" int into xp cost
	private static final int costMultiplier = 5;
	
	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/player/PlayerEntity;addExperienceLevels(I)V"
		),
		method = "onTakeOutput"
	)
	private void subtractXP(PlayerEntity player, int negativeCost) {
		XPUtil.subtractXP(player, -negativeCost);
	}
	
	/**
	 @author juliand665
	 @reason we've changed the way levelCost works
	 */
	@Overwrite
	public boolean canTakeOutput(PlayerEntity player, boolean present) {
		if (levelCost.get() < 0) return false;
		if (player.getAbilities().creativeMode) return true;
		return XPUtil.updateTotalXP(player) >= levelCost.get();
	}
	
	/**
	 @author juliand665
	 @reason We're replacing this entire functionality because vanilla sucks lmao
	 */
	@Overwrite
	public void updateResult() {
		output.setStack(0, ItemStack.EMPTY);
		levelCost.set(0);
		repairItemUsage = 0;
		
		var input1 = input.getStack(0);
		if (input1.isEmpty()) return;
		
		var output = input1.copy();
		var outputEnchants = EnchantmentHelper.get(output);
		var maxDamage = output.getMaxDamage();
		
		var repairCost = 0;
		var upgradeCost = 0; // flat upgrade costs for combining enchants of the same level
		var renameCost = 0;
		
		// renaming
		if (StringUtils.isBlank(newItemName)) {
			if (input1.hasCustomName()) {
				output.removeCustomName(); // free
			}
		} else if (!newItemName.equals(input1.getName().getString())) {
			output.setCustomName(new LiteralText(newItemName));
			renameCost += input1.getCount();
		}
		
		var input2 = input.getStack(1);
		if (!input2.isEmpty()) {
			var isRepairingWithMaterial = output.isDamageable() && output.getItem().canRepair(input1, input2);
			
			if (isRepairingWithMaterial) {
				// repair with material
				var durabilityPerItem = maxDamage / 4;
				var maxItemsUsed = (output.getDamage() + durabilityPerItem - 1) / durabilityPerItem;
				repairItemUsage = Math.min(maxItemsUsed, input2.getCount());
				var prevDamage = output.getDamage();
				output.setDamage(Math.max(0, prevDamage - repairItemUsage * durabilityPerItem));
				var durabilityRepaired = prevDamage - output.getDamage();
				var enchantFactor = outputEnchants.entrySet().stream()
					.mapToInt(entry -> entry.getValue() * rarityFactor(entry.getKey()))
					.sum();
				repairCost += enchantFactor * durabilityRepaired;
			} else {
				// combine with other item
				var isApplyingBookEnchants = input2.isOf(Items.ENCHANTED_BOOK)
					&& !EnchantedBookItem.getEnchantmentNbt(input2).isEmpty();
				var canMerge = output.isDamageable() && output.isOf(input2.getItem());
				if (!isApplyingBookEnchants && !canMerge) return;
				
				// combine enchantments
				outputEnchants.clear();
				
				var enchantDelta1 = 0;
				var enchantDelta2 = 0;
				
				var enchants1 = EnchantmentHelper.get(input1);
				var enchants2 = EnchantmentHelper.get(input2);
				var collectiveEnchants = new HashSet<Enchantment>();
				collectiveEnchants.addAll(enchants1.keySet());
				collectiveEnchants.addAll(enchants2.keySet());
				
				for (var enchant : collectiveEnchants) {
					int level1 = enchants1.getOrDefault(enchant, 0);
					int level2 = enchants2.getOrDefault(enchant, 0);
					
					var isAcceptableItem = enchant.isAcceptableItem(output)
						|| output.isOf(Items.ENCHANTED_BOOK);
					var isCompatible = enchants1.keySet().stream()
						.allMatch(e -> e == enchant || enchant.canCombine(e));
					var canApply = isAcceptableItem && isCompatible
						|| player.getAbilities().creativeMode;
					
					if (!canApply) continue; // enchantment on second item can't be applied to first one
					
					var rarityFactor = rarityFactor(enchant);
					
					var combinedLevel = Math.max(level1, level2);
					if (level1 == level2 && combinedLevel < enchant.getMaxLevel()) {
						// upgrade
						combinedLevel++;
						upgradeCost += combinedLevel * combinedLevel * rarityFactor;
					}
					outputEnchants.put(enchant, combinedLevel);
					
					// TODO: is this correct for upgrades?
					enchantDelta1 += (combinedLevel - level1) * rarityFactor;
					enchantDelta2 += (combinedLevel - level2) * rarityFactor;
				}
				
				EnchantmentHelper.set(outputEnchants, output);
				
				if (canMerge) {
					// combine durability
					int durability1 = input1.getMaxDamage() - input1.getDamage();
					int durability2 = input2.getMaxDamage() - input2.getDamage();
					int combinedDurability = Math.min(durability1 + durability2, maxDamage);
					int combinedDamage = maxDamage - combinedDurability;
					output.setDamage(combinedDamage);
					
					// every bit of durability that can't be covered by one item needs to come from the other, which needs xp to come up for its missing enchants
					repairCost += (combinedDurability - durability1) * enchantDelta2;
					repairCost += (combinedDurability - durability2) * enchantDelta1;
					repairCost *= enchantDeltaCostMultiplier;
				}
			}
		}
		
		var totalCost = 0f;
		totalCost += renameCost * renameCostMultiplier;
		totalCost += upgradeCost * upgradeCostMultiplier;
		if (repairCost > 0) { // avoid division by zero when not repairing
			totalCost += repairCost * repairCostMultiplier / (float) maxDamage;
		}
		totalCost *= costMultiplier;
		
		levelCost.set(Math.round(totalCost));
		this.output.setStack(0, output);
		sendContentUpdates();
	}
	
	private static int rarityFactor(Enchantment enchantment) {
		return switch (enchantment.getRarity()) {
			case COMMON -> 1;
			case UNCOMMON -> 2;
			case RARE -> 4;
			case VERY_RARE -> 8;
		};
	}
}
