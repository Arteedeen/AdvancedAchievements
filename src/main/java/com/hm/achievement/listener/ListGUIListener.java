package com.hm.achievement.listener;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.category.NormalAchievements;
import com.hm.achievement.command.executable.ReloadCommand;
import com.hm.achievement.gui.CategoryGUI;
import com.hm.achievement.gui.MainGUI;
import com.hm.achievement.lang.GuiLang;
import com.hm.achievement.lang.Lang;
import com.hm.achievement.lifecycle.Reloadable;
import com.hm.mcshared.file.CommentedYamlConfiguration;

/**
 * Listener class to deal with the GUIs from the /aach list command.
 *
 * @author Pyves
 */
@Singleton
public class ListGUIListener implements Listener, Reloadable {

	private static final int MAIN_GUI_PAGE = 0;

	private final CommentedYamlConfiguration langConfig;
	private final Set<String> disabledCategories;
	private final MainGUI mainGUI;
	private final CategoryGUI categoryGUI;
	private final Material lockedMaterial;

	private String langListGUITitle;

	@Inject
	public ListGUIListener(@Named("lang") CommentedYamlConfiguration langConfig, int serverVersion,
			Set<String> disabledCategories, MainGUI mainGUI, CategoryGUI categoryGUI, ReloadCommand reloadCommand) {
		this.langConfig = langConfig;
		this.disabledCategories = disabledCategories;
		this.mainGUI = mainGUI;
		this.categoryGUI = categoryGUI;
		lockedMaterial = serverVersion < 8 ? Material.OBSIDIAN : Material.BARRIER;
		reloadCommand.addObserver(this);
	}

	@Override
	public void extractConfigurationParameters() {
		langListGUITitle = ChatColor.translateAlternateColorCodes('&', Lang.get(GuiLang.GUI_TITLE, langConfig));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		String inventoryName = event.getInventory().getName();
		if (!inventoryName.startsWith(langListGUITitle) || event.getRawSlot() < 0) {
			return;
		}

		// Prevent players from taking items out of the GUI.
		event.setCancelled(true);

		int page = getCurrentCategoryPage(inventoryName);
		Player player = (Player) event.getWhoClicked();
		if (page == MAIN_GUI_PAGE) {
			// Main GUI, check whether player can interact with the selected item.
			if (event.getCurrentItem().getType() != lockedMaterial && event.getRawSlot() < getMainGUIItemCount()) {
				categoryGUI.displayCategoryGUI(event.getCurrentItem(), player, 1);
			}
			return;
		}

		ItemStack categoryItem = event.getInventory().getItem(0);
		// Check whether a navigation button was clicked in a category GUI.
		if (isButtonClicked(event, categoryGUI.getBackButton())) {
			mainGUI.displayMainGUI(player);
		} else if (isButtonClicked(event, categoryGUI.getPreviousButton())) {
			categoryGUI.displayCategoryGUI(categoryItem, player, page - 1);
		} else if (isButtonClicked(event, categoryGUI.getNextButton())) {
			categoryGUI.displayCategoryGUI(categoryItem, player, page + 1);
		}
	}

	/**
	 * Verifies whether the user has clicked on the given navigation button.
	 *
	 * @param event
	 * @param button
	 * @return true if the button is clicked, false otherwise
	 */
	private boolean isButtonClicked(InventoryClickEvent event, ItemStack button) {
		if (event.getCurrentItem().getDurability() == button.getDurability()
				&& event.getCurrentItem().getType() == button.getType()) {
			// Clicked item seems to be the button. But player could have clicked on item in his personal inventory that
			// matches the properties of the button used by Advanced Achievements. The first item matching the
			// properties of the button is the real one, check that this is indeed the clicked one.
			Map<Integer, ItemStack> backButtonCandidates = new TreeMap<>(
					event.getInventory().all(event.getCurrentItem().getType()));
			for (Entry<Integer, ItemStack> entry : backButtonCandidates.entrySet()) {
				if (entry.getValue().getDurability() == event.getCurrentItem().getDurability()) {
					// Found real button. Did the player click on it?
					if (entry.getKey() == event.getRawSlot()) {
						return true;
					}
					break;
				}
			}
		}
		return false;
	}

	/**
	 * Gets the current page index, by parsing the inventory title.
	 *
	 * @param name
	 * @return the current page number (start index is 1)
	 */
	private int getCurrentCategoryPage(String name) {
		String pageNumber = StringUtils.replaceOnce(name, langListGUITitle + " ", "");
		if (StringUtils.isNumeric(pageNumber)) {
			return Integer.parseInt(pageNumber);
		}
		return MAIN_GUI_PAGE;
	}

	/**
	 * Returns the number of items in the main GUI.
	 *
	 * @return the count of non disabled categories
	 */
	private int getMainGUIItemCount() {
		return NormalAchievements.values().length + MultipleAchievements.values().length - disabledCategories.size() + 1;
	}
}
