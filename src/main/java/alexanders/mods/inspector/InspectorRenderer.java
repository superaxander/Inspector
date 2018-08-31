package alexanders.mods.inspector;

import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.IRenderer;
import de.ellpeck.rockbottom.api.Registries;
import de.ellpeck.rockbottom.api.assets.IAssetManager;
import de.ellpeck.rockbottom.api.construction.resource.ResInfo;
import de.ellpeck.rockbottom.api.data.settings.Settings;
import de.ellpeck.rockbottom.api.entity.AbstractEntityItem;
import de.ellpeck.rockbottom.api.entity.Entity;
import de.ellpeck.rockbottom.api.entity.EntityLiving;
import de.ellpeck.rockbottom.api.entity.player.AbstractEntityPlayer;
import de.ellpeck.rockbottom.api.event.EventResult;
import de.ellpeck.rockbottom.api.event.IEventListener;
import de.ellpeck.rockbottom.api.event.impl.OverlayRenderEvent;
import de.ellpeck.rockbottom.api.event.impl.TooltipEvent;
import de.ellpeck.rockbottom.api.inventory.IInventory;
import de.ellpeck.rockbottom.api.item.Item;
import de.ellpeck.rockbottom.api.item.ItemInstance;
import de.ellpeck.rockbottom.api.item.ItemTile;
import de.ellpeck.rockbottom.api.item.ToolProperty;
import de.ellpeck.rockbottom.api.tile.Tile;
import de.ellpeck.rockbottom.api.tile.TileLiquid;
import de.ellpeck.rockbottom.api.tile.entity.IFilteredInventory;
import de.ellpeck.rockbottom.api.tile.entity.TileEntity;
import de.ellpeck.rockbottom.api.tile.state.TileProp;
import de.ellpeck.rockbottom.api.tile.state.TileState;
import de.ellpeck.rockbottom.api.util.BoundBox;
import de.ellpeck.rockbottom.api.util.Direction;
import de.ellpeck.rockbottom.api.util.Util;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.gen.biome.Biome;
import de.ellpeck.rockbottom.api.world.layer.TileLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static de.ellpeck.rockbottom.api.RockBottomAPI.*;

public class InspectorRenderer implements IEventListener<OverlayRenderEvent> {
    private HashMap<Tile, List<ItemInstance>> dropLists = new HashMap<>();

    private int currentLayer = 0;
    private long lastPressed = 0;
    private List<TileLayer> layers = null;


    @Override
    public EventResult listen(EventResult result, OverlayRenderEvent event) {
        if (layers == null) layers = TileLayer.getAllLayers();
        ItemInstance selected;
        IGameInstance game = event.game;
        AbstractEntityPlayer player = event.player;


        if (event.gui == null && player != null && player.getInv() != null) {
            IWorld world = player.world;
            if ((selected = player.getInv().get(player.getSelectedSlot())) != null && selected.getItem() == Inspector.inspectorItem) {
                if (!Inspector.keybind.isDown()) {
                    if(player.facing.x < 0)
                        event.assetManager.getTexture(Inspector.inspectorName.addPrefix("items.")).draw(event.graphics.getMouseInGuiX(), event.graphics.getMouseInGuiY(), 12, 12);
                    else
                        event.assetManager.getTexture(Inspector.inspectorName.addPrefix("items.")).draw(event.graphics.getMouseInGuiX(), event.graphics.getMouseInGuiY(), -12, 12);
                } else {
                    double mouseX = event.graphics.getMousedTileX();
                    double mouseY = event.graphics.getMousedTileY();
                    int x = Util.floor(mouseX);
                    int y = Util.floor(mouseY);
                    List<Entity> entities = world.getEntities(new BoundBox(mouseX, mouseY, mouseX, mouseY));
                    if (entities.size() > 0) {
                        describeEntity(entities.get(0), event.assetManager, event.graphics, game);
                    } else {
                        TileState tile;
                        if (Settings.KEY_BACKGROUND.isPressed() && System.currentTimeMillis() > lastPressed + 200) {
                            lastPressed = System.currentTimeMillis();
                            currentLayer = (++currentLayer) % layers.size();
                        }
                        tile = world.getState(layers.get(currentLayer), x, y);
                        describeTile(tile, x, y, layers.get(currentLayer), world, event.assetManager, event.graphics, game, player);

                    }
                }
            }
        }
        return result;
    }

    private void describeEntity(Entity entity, IAssetManager manager, IRenderer graphics, IGameInstance game) {
        ArrayList<String> desc = new ArrayList<>();
        desc.add("UUID: " + entity.getUniqueId());
        desc.add("Position: " + String.format("%.3f : %.3f", entity.getX(), entity.getY()));
        desc.add("Facing: " + entity.facing);
        if (entity.getAdditionalData() != null) desc.add("Additional data: " + entity.getAdditionalData());
        desc.add("Seconds existed: " + entity.ticksExisted / 40);
        desc.add("Motion x axis: " + String.format("%.3f", entity.motionX));
        desc.add("Motion y axis: " + String.format("%.3f", entity.motionY));
        desc.add("Can climb: " + entity.canClimb);
        desc.add("Is climbing: " + entity.isClimbing);
        desc.add("Is falling: " + entity.isFalling);
        if (entity instanceof AbstractEntityItem) {
            desc.add("Item: ");
            ItemInstance itemInstance = ((AbstractEntityItem) entity).getItem();
            Item item = itemInstance.getItem();
            desc.add("Item name: " + item.getName());
            describeAndIndent(manager, itemInstance, desc);
            List<String> names = new ArrayList<>(getResourceRegistry().getNames(new ResInfo(itemInstance)));

            int highestPossibleMeta = itemInstance.getItem().getHighestPossibleMeta();
            if (highestPossibleMeta > 0) {
                for (int i = 0; i < highestPossibleMeta; i++) { // TODO: Check for negative meta values?
                    names.addAll(getResourceRegistry().getNames(new ResInfo(itemInstance.getItem(), i)));
                }
            }

            if (names.size() > 0) {
                if (names.size() > 1) {
                    desc.add("\tResource registry names:");
                    for (String name : names)
                        desc.add("\t\t" + name);
                } else {
                    desc.add("\tResource registry name: " + names.get(0));
                }
            }

            desc.add("\tMeta: " + item);
            desc.add("\tAmount: " + itemInstance.getAmount() + " / " + itemInstance.getMaxAmount());
            if (itemInstance.getAdditionalData() != null) desc.add("\tItem additional data" + itemInstance.getAdditionalData());
            if (item instanceof ItemTile) {
                desc.add("\tItemTile name: " + ((ItemTile) item).getTile().getName());
            }
            if (getEventHandler().fireEvent(new TooltipEvent(itemInstance, game, manager, graphics, desc)) == EventResult.CANCELLED) return;
        } else if (entity instanceof EntityLiving) {
            desc.add("Entity Living: ");
            EntityLiving living = (EntityLiving) entity;
            desc.add("\tHealth: " + living.getHealth() + " / " + living.getMaxHealth());
            desc.add("\tRegen rate: 1 HP every " + living.getRegenRate() + " ticks");
            if (living instanceof AbstractEntityPlayer) {
                AbstractEntityPlayer player = (AbstractEntityPlayer) living;
                desc.add("\tName: " + player.getName());
                if (getNet().isActive()) desc.add("\tCommand level: " + player.getCommandLevel());
            }
        }
        tabsToSpaces(desc);
        graphics.drawHoverInfo(game, manager, graphics.getMouseInGuiX() + 18.0F / graphics.getGuiScale(), graphics.getMouseInGuiY() + 18.0F / graphics.getGuiScale(), 0.2F, true,
                               false, 500, desc);
    }

    private void tabsToSpaces(ArrayList<String> desc) {
        desc.replaceAll((str) -> str.replaceAll("\t", "----"));
    }

    private void describeTile(TileState state, int x, int y, TileLayer layer, IWorld world, IAssetManager manager, IRenderer graphics, IGameInstance game, AbstractEntityPlayer player) {
        Tile tile = state.getTile();
        ItemInstance itemInstance = null;
        if (tile.getItem() != null) itemInstance = new ItemInstance(tile);
        ArrayList<String> desc = new ArrayList<>();

        desc.add("Name: " + (itemInstance == null ? manager.localize(tile.getName()) : itemInstance.getDisplayName()));
        if (itemInstance != null) {
            List<String> names = new ArrayList<>(getResourceRegistry().getNames(new ResInfo(tile)));
            int highestPossibleMeta = itemInstance.getItem().getHighestPossibleMeta();
            if (highestPossibleMeta > 0) {
                for (int i = 0; i < highestPossibleMeta; i++) { // TODO: Check for negative meta values?
                    names.addAll(getResourceRegistry().getNames(new ResInfo(itemInstance.getItem(), i)));
                }
            }
            if (names.size() > 0) {
                if (names.size() > 1) {
                    desc.add("Resource registry names:");
                    for (String name : names)
                        desc.add("\t" + name);
                } else {
                    desc.add("Resource registry name: " + names.get(0));
                }
            }
        }
        desc.add("Layer: " + layer);
        desc.add("State: " + state.toString());
        desc.add("Properties:" + tile.getProps().stream().map(TileProp::toString).collect(Collectors.joining(",", "[", "]")));
        desc.add("Position: " + x + " : " + y);
        desc.add("Sky light: " + world.getSkyLight(x, y));
        desc.add("Artificial light: " + world.getArtificialLight(x, y));
        desc.add("Combined light: " + world.getCombinedLight(x, y));
        desc.add("Hardness: " + tile.getHardness(world, x, y, layer));
        desc.add("Breakable: " + tile.canBreak(world, x, y, layer, player, true));
        desc.add("Replaceable: " + tile.canReplace(world, x, y, layer));
        desc.add("Can keep plants: " + tile.canKeepPlants(world, x, y, layer));
        desc.add("Grass spreadable: " + tile.canGrassSpreadTo(world, x, y, layer));
        desc.add("Sustains trees: " + tile.doesSustainLeaves(world, x, y, layer));
        desc.add("Light emitted: " + tile.getLight(world, x, y, layer));
        desc.add("Translucency: " + String.format("%.0f %%", tile.getTranslucentModifier(world, x, y, layer, true) * 100));
        desc.add("Full tile: " + tile.isFullTile());
        desc.add("Solid surface: " + tile.hasSolidSurface(world, x, y, layer));
        desc.add("Obscures background: " + tile.obscuresBackground(world, x, y, layer));
        desc.add("Liquid: " + tile.isLiquid());
        if (tile instanceof TileLiquid) {
            TileLiquid liquid = (TileLiquid) tile;
            desc.add("Liquid levels: " + liquid.getLevels());
            desc.add("Flows: " + liquid.doesFlow());
            desc.add("Liquid flow rate: " + liquid.getFlowSpeed());
        }
        String effective = getEffective(tile, world, x, y, layer, manager);
        if (!effective.isEmpty()) desc.add("Effective tool type: " + effective);
        if (tile.canProvideTileEntity()) {
            TileEntity te = world.getTileEntity(x, y);
            if (te != null) {
                IFilteredInventory inv;
                if ((inv = te.getTileInventory()) != null) {
                    desc.add("Inventory:");
                    int amount = inv.getSlotAmount();
                    for (int i = 0; i < amount / 5; i++) {
                        String row = getRow(inv, i);
                        if (row.equals("\t")) {
                            continue;
                        }
                        desc.add(row);
                    }
                }
            }
        }
        desc.add("");
        desc.add("Biome:");
        Biome biome = world.getBiome(x, y);
        desc.add("\tName: " + biome.getName());
        desc.add("\tGrassland decoration: " + biome.hasGrasslandDecoration());
        desc.add("\tFlower chance: " + biome.getFlowerChance());
        desc.add("\tPebble chance: " + biome.getPebbleChance());

        if (itemInstance != null) tile.describeItem(manager, itemInstance, desc, true);

        List<ItemInstance> drops = tile.getDrops(world, x, y, layer, player);
        if (dropLists.containsKey(tile)) { //This is for reducing flickering
            if (dropLists.get(tile) != null) {
                if (dropLists.get(tile).equals(drops)) {
                    if (!drops.isEmpty()) {
                        desc.add("");
                        desc.add("Drops:");
                        for (ItemInstance drop : drops) {
                            desc.add("\t" + drop.getDisplayName() + ": " + drop.getAmount());
                        }
                    }
                } else {
                    dropLists.put(tile, null);
                }
            }
        } else {
            if (drops != null && !drops.isEmpty()) {
                dropLists.put(tile, drops);
                desc.add("");
                desc.add("Drops:");
                for (ItemInstance drop : drops) {
                    desc.add("\t" + drop.getDisplayName() + ": " + drop.getAmount());
                }
            }
        }

        if (itemInstance == null || getEventHandler().fireEvent(new TooltipEvent(itemInstance, game, manager, graphics, desc)) != EventResult.CANCELLED) {
            tabsToSpaces(desc);
            graphics.drawHoverInfo(game, manager, graphics.getMouseInGuiX() + 18.0F / graphics.getGuiScale(), graphics.getMouseInGuiY() + 18.0F / graphics.getGuiScale(), 0.2F,
                                   true, false, 500, desc);
        }
    }

    private String getRow(IInventory inv, int i) {
        StringBuilder builder = new StringBuilder("\t");
        i *= 5;

        boolean first = true;
        for (int j = i; j < i + 5; j++) {
            if (inv.getSlotAmount() - 1 < j) break;
            ItemInstance instance = inv.get(j);
            if (instance != null) {
                if (first) first = false;
                else builder.append(":");
                builder.append(instance);
            }
        }

        return builder.toString();
    }

    private void describeAndIndent(IAssetManager manager, ItemInstance itemInstance, ArrayList<String> desc) {
        Item item = itemInstance.getItem();
        ArrayList<String> indentList = new ArrayList<>();
        item.describeItem(manager, itemInstance, indentList, true);
        indentList.replaceAll((str) -> '\t' + str);
        desc.addAll(indentList);
    }

    private String getEffective(Tile tile, IWorld world, int x, int y, TileLayer layer, IAssetManager manager) {
        StringBuilder builder = new StringBuilder();

        for (ToolProperty property : Registries.TOOL_PROPERTY_REGISTRY.values()) {
            if (tile.isToolEffective(world, x, y, layer, property, Integer.MAX_VALUE)) builder.append(manager.localize(property.getName()));
        }

        return builder.toString();
    }
}
