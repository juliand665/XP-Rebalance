package xprebalance;

import net.minecraft.entity.player.PlayerEntity;

import java.util.stream.IntStream;

public abstract class XPUtil {
	public static int updateTotalXP(PlayerEntity player) {
		var total = IntStream.range(0, player.experienceLevel)
			.map(i -> xpForLevel(player, i))
			.sum();
		total += Math.round(player.experienceProgress * player.getNextLevelExperience());
		player.totalExperience = total;
		return total;
	}
	
	public static int xpForLevel(PlayerEntity player, int level) {
		var oldLevel = player.experienceLevel;
		player.experienceLevel = level;
		var xp = player.getNextLevelExperience();
		player.experienceLevel = oldLevel;
		return xp;
	}
	
	public static void subtractXP(PlayerEntity player, int xp) {
		if (player.totalExperience < xp) {
			XPRebalanceMod.LOGGER.error(
				"attempting to subtract more xp than player currently has! ({}/{})",
				xp, player.totalExperience
			);
			player.totalExperience = 0;
		}
		
		updateTotalXP(player);
		player.totalExperience -= xp;
		
		// just reset to 0 and re-progress through the levels lol, avoids floating-point inaccuracy too
		player.experienceLevel = 0;
		float xpToAdd = player.totalExperience;
		while (xpToAdd > player.getNextLevelExperience()) {
			xpToAdd -= player.getNextLevelExperience();
			player.experienceLevel++;
		}
		player.experienceProgress = xpToAdd / player.getNextLevelExperience();
	}
}
