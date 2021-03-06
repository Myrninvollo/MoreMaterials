/*
 * This file is part of MoreMaterials, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 AlmuraDev <http://www.almuradev.com/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.morematerials.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.configuration.file.YamlConfiguration;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.inventory.SpoutItemStack;
import org.getspout.spoutapi.inventory.SpoutShapedRecipe;
import org.getspout.spoutapi.inventory.SpoutShapelessRecipe;
import org.getspout.spoutapi.material.Material;
import org.getspout.spoutapi.material.MaterialData;



import net.morematerials.MoreMaterials;
import net.morematerials.materials.CustomShapeTemplate;
import net.morematerials.materials.MMCustomArmor;
import net.morematerials.materials.MMCustomBlock;
import net.morematerials.materials.MMCustomFood;
import net.morematerials.materials.MMCustomItem;
import net.morematerials.materials.MMCustomTool;

public class SmpManager {

	private MoreMaterials plugin;
	private boolean crossRecipe = false;
	private ArrayList<MMCustomBlock> blocksList = new ArrayList<MMCustomBlock>();
	private ArrayList<MMCustomItem> itemsList = new ArrayList<MMCustomItem>();
	private ArrayList<MMCustomTool> toolsList = new ArrayList<MMCustomTool>();
	private ArrayList<MMCustomArmor> armorList = new ArrayList<MMCustomArmor>();
	private ArrayList<MMCustomFood> foodList = new ArrayList<MMCustomFood>();
	private HashMap<String, CustomShapeTemplate> shapesMap = new HashMap<String, CustomShapeTemplate>();
	
	HashMap<String, HashMap<String, YamlConfiguration>> storedConfigs = new HashMap<String, HashMap<String, YamlConfiguration>>();

	public SmpManager(MoreMaterials plugin) {
		this.plugin = plugin;
	}

	public void init() {
		// Load all .smp files.
		File dir = new File(this.plugin.getDataFolder().getPath(), "materials");
		
		// First we simply register all materials.
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".smp")) {
				try {
					this.loadPackage(file);
				} catch (Exception exception) {
					this.plugin.getUtilsManager().log("Cannot load " + file.getName(), Level.SEVERE);
					exception.printStackTrace();
				}
			}
		}
		
		// Now when they are known, we can simply configure them completely.
		this.configurePackages();
		
		// Free up the memory.
		this.storedConfigs.clear();
		this.shapesMap.clear();
		this.plugin.getAssetManager().freeImageCacheMemory();
	}

	private void loadPackage(File file) throws Exception {
		HashMap<String, YamlConfiguration> materials = new HashMap<String, YamlConfiguration>();
		ZipFile smpFile = new ZipFile(file);

		// Get all material configurations
		Enumeration<? extends ZipEntry> entries = smpFile.entries();
		ZipEntry entry;
		YamlConfiguration yml;
		String smpName = this.plugin.getUtilsManager().getName(file.getName());
		while (entries.hasMoreElements()) {
			entry = entries.nextElement();
			// Parse all .yml files in this .smp file.
			if (entry.getName().endsWith(".yml")) {
				yml = new YamlConfiguration();
				yml.load(smpFile.getInputStream(entry));
				materials.put(entry.getName().substring(0, entry.getName().lastIndexOf(".")), yml);
			} else if (entry.getName().endsWith(".shape") || entry.getName().endsWith(".obj") || entry.getName().endsWith(".ply")) {
				// Register .shape files.
				String matName = this.plugin.getUtilsManager().getName(entry.getName());
				this.shapesMap.put(smpName + "_" + matName, new CustomShapeTemplate(this.plugin, smpFile, entry));
			} else {
				// Add all other files as asset.
				this.plugin.getAssetManager().addAsset(smpFile, entry);
			}
		}
		smpFile.close();
		
		// First loop - Create all materials.
		for (String matName : materials.keySet()) {
			if (!matName.matches("^[0-9@]+$")) {
				this.createMaterial(smpName, matName, materials.get(matName), smpFile);
			}
		}
		
		// Needs to be stored till later.
		this.storedConfigs.put(smpName, materials);
	}

	private void configurePackages() {
		// Second loop - Now we can reference all drops
		for (Integer i = 0; i < this.blocksList.size(); i++) {
			this.blocksList.get(i).configureDrops();
		}
		
		// Third loops - Now we can reference all crafting recipes
		for (String smpName : this.storedConfigs.keySet()) {
			for (String matName : this.storedConfigs.get(smpName).keySet()) {
				this.registerRecipes(smpName, matName, this.storedConfigs.get(smpName).get(matName));
			}
		}
				
		// Fourth loops - Set all strength modifiers.
		for (Integer i = 0; i < this.toolsList.size(); i++) {
			this.toolsList.get(i).configureModifiers();
		}
	}

	private void createMaterial(String smpName, String matName, YamlConfiguration yaml, ZipFile smpFile) {
		// Create the actual materials.
		if (yaml.getString("Type", "").equals("Block")) {
			this.blocksList.add(MMCustomBlock.create(this.plugin, yaml, smpName, matName));
		} else if (yaml.getString("Type", "").equals("Tool")) {
			this.toolsList.add(MMCustomTool.create(this.plugin, yaml, smpName, matName));
		} else if (yaml.getString("Type", "").equals("Item")) {
			this.itemsList.add(MMCustomItem.create(this.plugin, yaml, smpName, matName));
		} else if (yaml.getString("Type", "").equals("Armor")) {
			this.armorList.add(MMCustomArmor.create(this.plugin, yaml, smpName, matName));
		} else if (yaml.getString("Type", "").equals("Food")) {
			this.foodList.add(MMCustomFood.create(this.plugin, yaml, smpName, matName));
		}
	}


	public CustomShapeTemplate getShape(String smpName, String shapeName) {
		// Allow to reference shapes from other .smp files.
		String[] fileNameParts = shapeName.split("/");
		if (fileNameParts.length == 2) {
			smpName = fileNameParts[0];
			shapeName = fileNameParts[1];
		}
		
		// Get the correct shape
		if (this.shapesMap.containsKey(smpName + "_" + shapeName)) {
			return this.shapesMap.get(smpName + "_" + shapeName);
		}
		return null;
	}

	public Material getMaterial(String smpName, String matName) {
		// Allow to reference materials from other .smp files.
		String[] matNameParts = matName.split("/");
		if (matNameParts.length == 2) {
			smpName = matNameParts[0];
			matName = matNameParts[1];
		}
		
		// First check for matching blocks.
		MMCustomBlock currentBlock;
		for (Integer i = 0; i < this.blocksList.size(); i++) {
			currentBlock = this.blocksList.get(i);
			if (currentBlock.getSmpName().equals(smpName) && currentBlock.getMaterialName().equals(matName)) {
				return currentBlock;
			}
		}
		
		// Then also check for matching items.
		MMCustomItem currentItem;
		for (Integer i = 0; i < this.itemsList.size(); i++) {
			currentItem = this.itemsList.get(i);
			if (currentItem.getSmpName().equals(smpName) && currentItem.getMaterialName().equals(matName)) {
				return currentItem;
			}
		}
		
		// Then also check for matching tools.
		MMCustomTool currentTool;
		for (Integer i = 0; i < this.toolsList.size(); i++) {
			currentTool = this.toolsList.get(i);
			if (currentTool.getSmpName().equals(smpName) && currentTool.getMaterialName().equals(matName)) {
				return currentTool;
			}
		}
		
		// Then also check for matching armor.
		MMCustomArmor currentArmor;
		for (Integer i = 0; i < this.armorList.size(); i++) {
			currentArmor = this.armorList.get(i);
			if (currentArmor.getSmpName().equals(smpName) && currentArmor.getMaterialName().equals(matName)) {
				return currentArmor;
			}
		}

		// Then also check for matching armor.
		MMCustomFood currentFood;
		for (Integer i = 0; i < this.foodList.size(); i++) {
			currentFood = this.foodList.get(i);
			if (currentFood.getSmpName().equals(smpName) && currentFood.getMaterialName().equals(matName)) {
				return currentFood;
			}
		}

		if (smpName == null) {			
			MMCustomBlock currentBlock_a;
			for (Integer i = 0; i < this.blocksList.size(); i++) {
				currentBlock_a = this.blocksList.get(i);
				if (currentBlock_a.getMaterialName().equals(matName)) {					
					return currentBlock_a;
				}
			}
			
			MMCustomItem currentItem_a;
			for (Integer i = 0; i < this.itemsList.size(); i++) {
				currentItem_a = this.itemsList.get(i);
				if (currentItem_a.getMaterialName().equals(matName)) {					
					return currentItem_a;
				}
			}
		}
		return null;
	}

	public Material getMaterial(String matName) {
			
		// First check for matching blocks.
		MMCustomBlock currentBlock;
		for (Integer i = 0; i < this.blocksList.size(); i++) {
			currentBlock = this.blocksList.get(i);
			if (currentBlock.getMaterialName().equals(matName)) {
				return currentBlock;
			}
		}
		
		// Then also check for matching items.
		MMCustomItem currentItem;
		for (Integer i = 0; i < this.itemsList.size(); i++) {
			currentItem = this.itemsList.get(i);
			if (currentItem.getMaterialName().equals(matName)) {
				return currentItem;
			}
		}
		
		// Then also check for matching tools.
		MMCustomTool currentTool;
		for (Integer i = 0; i < this.toolsList.size(); i++) {
			currentTool = this.toolsList.get(i);
			if (currentTool.getMaterialName().equals(matName)) {
				return currentTool;
			}
		}
		
		// Then also check for matching armor.
		MMCustomArmor currentArmor;
		for (Integer i = 0; i < this.armorList.size(); i++) {
			currentArmor = this.armorList.get(i);
			if (currentArmor.getMaterialName().equals(matName)) {
				return currentArmor;
			}
		}

		// Then also check for matching armor.
		MMCustomFood currentFood;
		for (Integer i = 0; i < this.foodList.size(); i++) {
			currentFood = this.foodList.get(i);
			if (currentFood.getMaterialName().equals(matName)) {
				return currentFood;
			}
		}
	
		return null;
	}
	
	public Material getMaterial(Integer materialId) {
		// First check for matching blocks.
		MMCustomBlock currentBlock;
		for (Integer i = 0; i < this.blocksList.size(); i++) {
			currentBlock = this.blocksList.get(i);
			if (currentBlock.getCustomId() == materialId) {
				return currentBlock;
			}
		}
		
		// Then also check for matching items.
		MMCustomItem currentItem;
		for (Integer i = 0; i < this.itemsList.size(); i++) {
			currentItem = this.itemsList.get(i);
			if (currentItem.getCustomId() == materialId) {
				return currentItem;
			}
		}
		
		// Then also check for matching tools.
		MMCustomTool currentTool;
		for (Integer i = 0; i < this.toolsList.size(); i++) {
			currentTool = this.toolsList.get(i);
			if (currentTool.getCustomId() == materialId) {
				return currentTool;
			}
		}
		
		// Then also check for matching armor.
		MMCustomArmor currentArmor;
		for (Integer i = 0; i < this.armorList.size(); i++) {
			currentArmor = this.armorList.get(i);
			if (currentArmor.getCustomId() == materialId) {
				return currentArmor;
			}
		}

		// Then also check for matching armor.
		MMCustomFood currentFood;
		for (Integer i = 0; i < this.foodList.size(); i++) {
			currentFood = this.foodList.get(i);
			if (currentFood.getCustomId() == materialId) {
				return currentFood;
			}
		}

		return null;
	}

	private void registerRecipes(String smpName, String matName, YamlConfiguration config) {
		List<?> recipes = config.getList("Recipes");
		// Make sure we have a valid list.
		if (recipes == null) {
			return;
		}
		
		// Get the material object which we want to craft.
		Material material;
		if (matName.matches("^[0-9@]+$")) {
			String[] matInfo = matName.split("@");
			if (matInfo.length == 1) {
				material = MaterialData.getMaterial(Integer.parseInt(matInfo[0]));
			} else {
				material = MaterialData.getMaterial(Integer.parseInt(matInfo[0]), (short) Integer.parseInt(matInfo[1]));
			}
		} else {
			material = this.getMaterial(smpName, matName);
		}
		
		if (material == null) {
			this.plugin.getUtilsManager().log("Recipe within: " + smpName + " for item: " + matName + " could not be found!", Level.WARNING);
			return;
		}

		for (Object orecipe : recipes) {
			@SuppressWarnings("unchecked")
			Map<String, Object> recipe = (Map<String, Object>) orecipe;
			// This is what we want to craft.
			Integer amount = recipe.containsKey("Amount") ? (Integer) recipe.get("Amount") : 1;
			SpoutItemStack stack = new SpoutItemStack(material, amount);
			String ingredients = "" + recipe.get("Ingredients");

			// Building recipe
			String type = recipe.containsKey("Type") ? (String) recipe.get("Type") : "";
			if (type.equalsIgnoreCase("Furnace")) {
				// Get correct ingredient material
				Material ingredient;
				if (ingredients.matches("^[0-9@]+$")) {
					String[] matInfo = ingredients.split("@");
					if (matInfo.length == 1) {
						ingredient = MaterialData.getMaterial(Integer.parseInt(matInfo[0]));
					} else {
						ingredient = MaterialData.getMaterial(Integer.parseInt(matInfo[0]), (short) Integer.parseInt(matInfo[1]));
					}
				} else {
					ingredient = this.getMaterial(smpName, ingredients);
				}
				
				if (ingredient == null) {
					// Look for previously registered ingredients from other SMP packages.
					ingredient = this.getMaterial(null, ingredients);
					if (ingredient != null) {
						crossRecipe = true;
					}
				}
				
				if (ingredient == null) {	
					this.plugin.getUtilsManager().log("Recipe within: " + smpName + " for item: " + matName + " using the ingredient of: " + ingredients + " could not be found!", Level.WARNING);
					continue;
				}			
				
				if (crossRecipe) {
					this.plugin.getUtilsManager().log("Multi-SMP based shaped recipe within [" + smpName + "] for item [" + matName + "] created.", Level.INFO);
					crossRecipe = false;
				}
				this.plugin.getFurnaceRecipeManager().registerRecipe(new SpoutItemStack(material, amount), ingredient);
                //System.out.println("Material: " + material.getName());
                //System.out.println("Amount: " + amount);
                //System.out.println("Ingredient: " + ingredient.getName());
			} else if (type.equalsIgnoreCase("Shapeless")) {
				SpoutShapelessRecipe sRecipe = new SpoutShapelessRecipe(stack);
				
				// Get correct ingredient materials
				Material ingredient;
				ingredients = ingredients.trim().replaceAll("[\\s\\r\\n]+", " ");
				for (String ingredientitem : ingredients.split(" ")) {
					if (ingredientitem.matches("^[0-9@]+$")) {
						String[] matInfo = ingredientitem.split("@");
						if (matInfo.length == 1) {
							ingredient = MaterialData.getMaterial(Integer.parseInt(matInfo[0]));
						} else {
							ingredient = MaterialData.getMaterial(Integer.parseInt(matInfo[0]), (short) Integer.parseInt(matInfo[1]));						
						}
					} else {
						ingredient = this.getMaterial(smpName, ingredientitem);
					}
					
					if (ingredient == null) {
						// Look for previously registered ingredients from other SMP packages.
						ingredient = this.getMaterial(null, ingredientitem);
						if (ingredient != null) {
							crossRecipe = true;
						}
					}
					
					if (ingredient == null) {
						this.plugin.getUtilsManager().log("Recipe within: " + smpName + " for item: " + matName + " using the ingredient of: " + ingredientitem + " could not be found!", Level.WARNING);
						continue;
					}
					
					(sRecipe).addIngredient(ingredient);
				}
				// Finaly register recipe.
				if (crossRecipe) {
					this.plugin.getUtilsManager().log("Multi-SMP based shaped recipe within [" + smpName + "] for item [" + matName + "] created.", Level.INFO);
					crossRecipe = false;
				}
				SpoutManager.getMaterialManager().registerSpoutRecipe(sRecipe);
			} else if (type.equalsIgnoreCase("Shaped")) {
				SpoutShapedRecipe sRecipe = new SpoutShapedRecipe(stack).shape("abc","def", "ghi");
				
				// Split ingredients.
				ingredients = ingredients.trim().replaceAll("[\\s\\r\\n]+", " ");
				
				// Parse all lines
				Boolean invalidRecipe = false;
				Integer currentColumn = 0;
					
				for (String ingredientitem : ingredients.split(" ")) {
					// Get correct ingredient material
					Material ingredient;
					if (ingredientitem.matches("^[0-9@]+$")) {
						String[] matInfo = ingredientitem.split("@");
						if (matInfo.length == 1) {
							ingredient = MaterialData.getMaterial(Integer.parseInt(matInfo[0]));
						} else {
							ingredient = MaterialData.getMaterial(Integer.parseInt(matInfo[0]), (short) Integer.parseInt(matInfo[1]));
						}
					} else {
						ingredient = this.getMaterial(smpName, ingredientitem);
					}
					
					if (ingredient == null) {
						// Look for previously registered ingredients from other SMP packages.
						ingredient = this.getMaterial(null, ingredientitem);
						if (ingredient != null) {
							crossRecipe = true;
						}
					}
					
					if (ingredient == null) {
						this.plugin.getUtilsManager().log("Recipe within: " + smpName + " for item: " + matName + " using the ingredient of: " + ingredientitem + " could not be found!", Level.WARNING);						
						invalidRecipe = true;
					}

					// Skip "air"
					if (ingredient == null || ingredientitem.equals("0")) {
						currentColumn++;
						continue;
					}
					
					// Add the ingredient
					sRecipe.setIngredient((char) ('a' + currentColumn), ingredient);
					currentColumn++;
				}
				
				// Finaly register recipe.
				if (!invalidRecipe) {
					if (crossRecipe) {
						this.plugin.getUtilsManager().log("Multi-SMP based shaped recipe within [" + smpName + "] for item [" + matName + "] created.", Level.INFO);
						crossRecipe = false;
					}
					SpoutManager.getMaterialManager().registerSpoutRecipe(sRecipe);
				}
			}
		}
	}

	public int getTotalMaterials() {
		return this.blocksList.size() + this.itemsList.size() + this.toolsList.size();
	}

}
