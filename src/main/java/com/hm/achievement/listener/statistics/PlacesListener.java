package com.hm.achievement.listener.statistics;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.command.executable.ReloadCommand;
import com.hm.achievement.db.CacheManager;
import com.hm.achievement.utils.RewardParser;
import com.hm.mcshared.file.CommentedYamlConfiguration;

/**
 * Listener class to deal with Places achievements.
 * 
 * @author Pyves
 *
 */
@Singleton
public class PlacesListener extends AbstractListener {

	@Inject
	public PlacesListener(@Named("main") CommentedYamlConfiguration mainConfig, int serverVersion,
			Map<String, List<Long>> sortedThresholds, CacheManager cacheManager, RewardParser rewardParser,
			ReloadCommand reloadCommand) {
		super(mainConfig, serverVersion, sortedThresholds, cacheManager, rewardParser, reloadCommand);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (!shouldIncreaseBeTakenIntoAccountNoPermissions(player)) {
			return;
		}

		Block block = event.getBlock();

		MultipleAchievements category = MultipleAchievements.PLACES;

		String blockName = block.getType().name().toLowerCase();
		if (!player.hasPermission(category.toPermName() + '.' + blockName)) {
			return;
		}

		Set<String> foundAchievements = findAchievementsByCategoryAndName(
				category, blockName + ':' + block.getState().getData().toItemStack(0).getDurability());
		foundAchievements.addAll(findAchievementsByCategoryAndName(category, blockName));
		foundAchievements.forEach(achievement -> updateStatisticAndAwardAchievementsIfAvailable(player, category,
				achievement, 1));
	}
}
