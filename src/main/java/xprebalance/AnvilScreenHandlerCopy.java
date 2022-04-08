package xprebalance;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.text.LiteralText;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

// a copy of part of the vanilla AnvilScreenHandler code for reference
abstract class AnvilScreenHandlerCopy extends ForgingScreenHandler {
	private int repairItemUsage;
	private String newItemName;
	private final Property levelCost = Property.create();
	
	AnvilScreenHandlerCopy(int i, PlayerInventory playerInventory, ScreenHandlerContext screenHandlerContext) {
		super(ScreenHandlerType.ANVIL, i, playerInventory, screenHandlerContext);
	}
	
	public void updateResult() {
		ItemStack primaryInput = this.input.getStack(0);
		this.levelCost.set(1);
		int totalCost = 0;
		int repeatedRepairPenalty = 0;
		int renameCostPart = 0;
		if (primaryInput.isEmpty()) {
			this.output.setStack(0, ItemStack.EMPTY);
			this.levelCost.set(0);
		} else {
			ItemStack output = primaryInput.copy();
			ItemStack secondaryInput = this.input.getStack(1);
			Map<Enchantment, Integer> outputEnchants = EnchantmentHelper.get(output);
			repeatedRepairPenalty += primaryInput.getRepairCost() + (secondaryInput.isEmpty() ? 0 : secondaryInput.getRepairCost());
			this.repairItemUsage = 0;
			if (!secondaryInput.isEmpty()) {
				boolean isApplyingBookEnchants = secondaryInput.isOf(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantmentNbt(secondaryInput).isEmpty();
				if (output.isDamageable() && output.getItem().canRepair(primaryInput, secondaryInput)) {
					// repair with material
					
					if (output.getDamage() <= 0) {
						this.output.setStack(0, ItemStack.EMPTY);
						this.levelCost.set(0);
						return;
					}
					
					int repairItemsUsed = 0;
					int damageToRepair;
					do {
						damageToRepair = Math.min(output.getDamage(), output.getMaxDamage() / 4);
						output.setDamage(output.getDamage() - damageToRepair);
						totalCost++;
						repairItemsUsed++;
					} while (damageToRepair > 0 && repairItemsUsed < secondaryInput.getCount());
					
					this.repairItemUsage = repairItemsUsed;
				} else {
					// combine with other item
					
					if (!isApplyingBookEnchants && (!output.isOf(secondaryInput.getItem()) || !output.isDamageable())) {
						this.output.setStack(0, ItemStack.EMPTY);
						this.levelCost.set(0);
						return;
					}
					
					// repair
					if (output.isDamageable() && !isApplyingBookEnchants) {
						int primaryDurability = primaryInput.getMaxDamage() - primaryInput.getDamage();
						int secondaryDurability = secondaryInput.getMaxDamage() - secondaryInput.getDamage();
						int durabilityBonus = output.getMaxDamage() * 12 / 100;
						int outputDurability = primaryDurability + secondaryDurability + durabilityBonus;
						int outputDamage = output.getMaxDamage() - outputDurability;
						if (outputDamage < 0) {
							outputDamage = 0;
						}
						
						if (outputDamage < output.getDamage()) {
							output.setDamage(outputDamage);
							totalCost += 2;
						}
					}
					
					Map<Enchantment, Integer> enchantsToAdd = EnchantmentHelper.get(secondaryInput);
					boolean hadCompatibleEnchant = false;
					boolean hadIncompatibleEnchant = false;
					
					for (Enchantment enchantToCombine : enchantsToAdd.keySet()) {
						if (enchantToCombine == null) continue;
						
						int existingLevel = outputEnchants.getOrDefault(enchantToCombine, 0);
						int levelToCombine = enchantsToAdd.get(enchantToCombine);
						int newLevel = existingLevel == levelToCombine ? levelToCombine + 1 : Math.max(levelToCombine, existingLevel);
						boolean canApply = enchantToCombine.isAcceptableItem(primaryInput)
							|| this.player.getAbilities().creativeMode
							|| primaryInput.isOf(Items.ENCHANTED_BOOK);
						
						for (Enchantment enchantment : outputEnchants.keySet()) {
							if (enchantment != enchantToCombine && !enchantToCombine.canCombine(enchantment)) {
								canApply = false;
								totalCost++; // for real??
							}
						}
						
						if (!canApply) {
							hadIncompatibleEnchant = true;
						} else {
							hadCompatibleEnchant = true;
							if (newLevel > enchantToCombine.getMaxLevel()) {
								newLevel = enchantToCombine.getMaxLevel();
							}
							
							outputEnchants.put(enchantToCombine, newLevel);
							int rarityFactor = switch (enchantToCombine.getRarity()) {
								case COMMON -> 1;
								case UNCOMMON -> 2;
								case RARE -> 4;
								case VERY_RARE -> 8;
							};
							
							if (isApplyingBookEnchants) {
								rarityFactor = Math.max(1, rarityFactor / 2);
							}
							
							totalCost += rarityFactor * newLevel;
							if (primaryInput.getCount() > 1) {
								totalCost = 40;
							}
						}
					}
					
					if (hadIncompatibleEnchant && !hadCompatibleEnchant) {
						this.output.setStack(0, ItemStack.EMPTY);
						this.levelCost.set(0);
						return;
					}
				}
			}
			
			if (StringUtils.isBlank(this.newItemName)) {
				if (primaryInput.hasCustomName()) {
					renameCostPart = 1;
					totalCost += renameCostPart;
					output.removeCustomName();
				}
			} else if (!this.newItemName.equals(primaryInput.getName().getString())) {
				renameCostPart = 1;
				totalCost += renameCostPart;
				output.setCustomName(new LiteralText(this.newItemName));
			}
			
			this.levelCost.set(repeatedRepairPenalty + totalCost);
			if (totalCost <= 0) {
				output = ItemStack.EMPTY;
			}
			
			if (renameCostPart == totalCost && renameCostPart > 0 && this.levelCost.get() >= 40) {
				this.levelCost.set(39);
			}
			
			if (this.levelCost.get() >= 40 && !this.player.getAbilities().creativeMode) {
				output = ItemStack.EMPTY;
			}
			
			// penalize repeated repairs
			if (!output.isEmpty()) {
				int repairCost = output.getRepairCost();
				if (!secondaryInput.isEmpty() && repairCost < secondaryInput.getRepairCost()) {
					repairCost = secondaryInput.getRepairCost();
				}
				
				if (renameCostPart != totalCost || renameCostPart == 0) {
					repairCost = getNextCost(repairCost);
				}
				
				output.setRepairCost(repairCost);
				EnchantmentHelper.set(outputEnchants, output);
			}
			
			this.output.setStack(0, output);
			this.sendContentUpdates();
		}
	}
	
	private static int getNextCost(int cost) {
		return cost * 2 + 1;
	}
}
