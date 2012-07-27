/*
 The MIT License

 Copyright (c) 2011 Zloteanu Nichita (ZNickq) and Andre Mohren (IceReaper)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
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
import net.morematerials.furnace.FurnaceRecipes;
import net.morematerials.materials.CustomShape;
import net.morematerials.materials.MMCustomBlock;
import net.morematerials.materials.MMCustomItem;
import net.morematerials.materials.MMCustomTool;

public class SmpManager {

	private MoreMaterials plugin;

	private ArrayList<MMCustomBlock> blocksList = new ArrayList<MMCustomBlock>();
	private ArrayList<MMCustomItem> itemsList = new ArrayList<MMCustomItem>();
	private ArrayList<MMCustomTool> toolsList = new ArrayList<MMCustomTool>();
	private ArrayList<CustomShape> shapesList = new ArrayList<CustomShape>();

	public SmpManager(MoreMaterials plugin) {
		this.plugin = plugin;
	}

	public void init() {
		// Load all .smp files.
		File dir = new File(this.plugin.getDataFolder().getPath(), "materials");
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".smp")) {
				try {
					this.loadPackage(file);
				} catch (Exception exception) {
					this.plugin.getUtilsManager().log("Cannot load " + file.getName(), Level.SEVERE);
				}
			}
		}
	}

	private void loadPackage(File file) throws Exception {
		HashMap<String, YamlConfiguration> materials = new HashMap<String, YamlConfiguration>();
		ZipFile smpFile = new ZipFile(file);

		// Get all material configurations
		Enumeration<? extends ZipEntry> entries = smpFile.entries();
		ZipEntry entry;
		YamlConfiguration yml;
		while (entries.hasMoreElements()) {
			entry = entries.nextElement();
			// Parse all .yml files in this .smp file.
			if (entry.getName().endsWith(".yml")) {
				yml = new YamlConfiguration();
				yml.load(smpFile.getInputStream(entry));
				materials.put(entry.getName().substring(0, entry.getName().lastIndexOf(".")), yml);
			} else if (entry.getName().endsWith(".shape")) {
				// Register .shape files.
				this.shapesList.add(new CustomShape(this.plugin, smpFile, entry));
			} else {
				// Add all other files as asset.
				this.plugin.getWebManager().addAsset(smpFile, entry);
			}
		}
		smpFile.close();

		// First loop - Create all materials.
		for (String matName : materials.keySet()) {
			this.createMaterial(this.plugin.getUtilsManager().getName(smpFile.getName()), matName, materials.get(matName), smpFile);
		}
		
		// Second loop - Now we can reference all drops
		for (Integer i = 0; i < this.blocksList.size(); i++) {
			this.blocksList.get(i).configureDrops();
		}
		
		// Third loop - Now we can reference all crafting recipes
		for (String matName : materials.keySet()) {
			this.registerRecipes(this.plugin.getUtilsManager().getName(smpFile.getName()), matName, materials.get(matName));
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
		} else {
			this.itemsList.add(MMCustomItem.create(this.plugin, yaml, smpName, matName));
		}
	}


	public CustomShape getShape(String smpName, String matName) {
		CustomShape shape;
		// Search for the correct shape
		for (Integer i = 0; i < this.shapesList.size(); i++) {
			shape = this.shapesList.get(i);
			if (shape.getSmpName().equals(smpName)) {
				if (shape.getMatName().equals(matName)) {
					return shape;
				}
			}
		}
		return null;
	}

	public Material getMaterial(String smpName, String matName) {
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
		if (matName.matches("^[0-9]+$")) {
			material = this.getMaterial(Integer.parseInt(matName));
		} else {
			material = this.getMaterial(smpName, matName);
		}

		for (Object orecipe : recipes) {
			@SuppressWarnings("unchecked")
			Map<String, Object> recipe = (Map<String, Object>) orecipe;
			// This is what we want to craft.
			Integer amount = recipe.containsKey("Amount") ? (Integer) recipe.get("Amount") : 1;
			SpoutItemStack stack = new SpoutItemStack(material, amount);
			String ingredients = (String) recipe.get("Ingredients");

			// Building recipe
			String type = (String) recipe.get("Type");
			if (type.equalsIgnoreCase("Furnace")) {
				// Get correct ingredient material
				Material ingredient;
				if (ingredients.matches("^[0-9]+$")) {
					ingredient = MaterialData.getMaterial(Integer.parseInt(ingredients));
				} else {
					ingredient = this.getMaterial(smpName, ingredients);
				}
				FurnaceRecipes.CustomFurnaceRecipe(new SpoutItemStack(material, amount), ingredient.getRawId(), ingredient.getRawData());
			} else {
				// Get recipe type.
				if (type.equalsIgnoreCase("Shapeless")) {
					SpoutShapelessRecipe sRecipe = new SpoutShapelessRecipe(stack);
					
					// Get correct ingredient material
					Material ingredient;
					if (ingredients.matches("^[0-9]+$")) {
						ingredient = MaterialData.getMaterial(Integer.parseInt(ingredients));
					} else {
						ingredient = this.getMaterial(smpName, ingredients);
					}
					((SpoutShapelessRecipe) sRecipe).addIngredient(ingredient);
					// Finaly register recipe.
					SpoutManager.getMaterialManager().registerSpoutRecipe(sRecipe);
				} else if (type.equalsIgnoreCase("Shaped")) {
					SpoutShapedRecipe sRecipe = new SpoutShapedRecipe(stack).shape("abc","def", "ghi");
					
					// Split ingredients.
					ingredients = ingredients.replaceAll("\\s{2,}", " ");
					
					// Parse all lines
					Integer currentLine = 0;
					for (String line : ingredients.split("\\r?\\n")) {
						Integer currentColumn = 0;
						
						for (String ingredientitem : line.split(" ")) {
							// Get correct ingredient material
							Material ingredient;
							if (ingredientitem.matches("^[0-9]+$")) {
								ingredient = MaterialData.getMaterial(Integer.parseInt(ingredientitem));
							} else {
								ingredient = this.getMaterial(smpName, ingredientitem);
							}

							// Skip "air"
							if (ingredient == null || ingredientitem.equals("0")) {
								currentColumn++;
								continue;
							}
							
							// Add the ingredient
							sRecipe.setIngredient((char) ('a' + currentColumn + currentLine * 3), ingredient);
							currentColumn++;
						}
						currentLine++;
					}
					// Finaly register recipe.
					SpoutManager.getMaterialManager().registerSpoutRecipe(sRecipe);
				}
			}
		}
	}

}
