package alexanders.mods.inspector;

import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.assets.IAssetManager;
import de.ellpeck.rockbottom.api.data.settings.Settings;
import de.ellpeck.rockbottom.api.entity.Entity;
import de.ellpeck.rockbottom.api.entity.EntityItem;
import de.ellpeck.rockbottom.api.entity.EntityLiving;
import de.ellpeck.rockbottom.api.entity.player.AbstractEntityPlayer;
import de.ellpeck.rockbottom.api.event.EventResult;
import de.ellpeck.rockbottom.api.event.IEventListener;
import de.ellpeck.rockbottom.api.event.impl.OverlayRenderEvent;
import de.ellpeck.rockbottom.api.event.impl.TooltipEvent;
import de.ellpeck.rockbottom.api.item.ItemInstance;
import de.ellpeck.rockbottom.api.item.ToolType;
import de.ellpeck.rockbottom.api.tile.Tile;
import de.ellpeck.rockbottom.api.tile.entity.TileEntity;
import de.ellpeck.rockbottom.api.tile.entity.TileEntityFueled;
import de.ellpeck.rockbottom.api.tile.state.TileState;
import de.ellpeck.rockbottom.api.util.BoundBox;
import de.ellpeck.rockbottom.api.util.Util;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.TileLayer;
import org.newdawn.slick.Graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.ellpeck.rockbottom.api.RockBottomAPI.*;

public class InspectorRenderer implements IEventListener<OverlayRenderEvent> {
    private HashMap<Tile, List<ItemInstance>> dropLists = new HashMap<>();


    @Override
    public EventResult listen(EventResult result, OverlayRenderEvent event) {
        ItemInstance selected;
        IGameInstance game = event.game;
        AbstractEntityPlayer player = event.player;


        if (event.gui == null && player != null && player.getInv() != null) {
            IWorld world = player.world;
            if ((selected = player.getInv().get(player.getSelectedSlot())) != null && selected.getItem() == Inspector.inspectorItem) {
                if (!Inspector.keybind.isDown()) {
                    event.assetManager.getTexture(Inspector.inspectorName.addPrefix("items.")).draw(game.getMouseInGuiX(), game.getMouseInGuiY(), 12, 12);
                } else {
                    double mouseX = game.getInteractionManager().getMousedTileX();
                    double mouseY = game.getInteractionManager().getMousedTileY();
                    int x = Util.floor(mouseX);
                    int y = Util.floor(mouseY);
                    List<Entity> entities = world.getEntities(new BoundBox(mouseX, mouseY, mouseX, mouseY));
                    if (entities.size() > 0) {
                        describeEntity(entities.get(0), event.assetManager, event.graphics, game);
                    } else {
                        TileState tile;
                        if (Settings.KEY_BACKGROUND.isDown()) {
                            tile = world.getState(TileLayer.BACKGROUND, x, y);
                            describeTile(tile, x, y, TileLayer.BACKGROUND, world, event.assetManager, event.graphics, game, player);
                        } else {
                            tile = world.getState(TileLayer.MAIN, x, y);
                            describeTile(tile, x, y, TileLayer.MAIN, world, event.assetManager, event.graphics, game, player);
                        }
                    }
                }
            }
        }
        return result;
    }

    private void describeEntity(Entity entity, IAssetManager manager, Graphics graphics, IGameInstance game) {
        List<String> desc = new ArrayList<>();
        desc.add("UUID: " + entity.getUniqueId());
        desc.add("Position: " + String.format("%.3f : %.3f", entity.x, entity.y));
        if (entity.getAdditionalData() != null)
            desc.add("Additional data: " + entity.getAdditionalData());
        desc.add("Seconds existed: " + entity.ticksExisted / 40);
        desc.add("Motion x axis: " + String.format("%.3f", entity.motionX));
        desc.add("Motion y axis: " + String.format("%.3f", entity.motionY));
        if (entity instanceof EntityItem) {
            ((EntityItem) entity).item.getItem().describeItem(manager, ((EntityItem) entity).item, desc, true);
            desc.add("Meta: " + ((EntityItem) entity).item.getItem());
            desc.add("Amount: " + ((EntityItem) entity).item.getAmount() + " / " + ((EntityItem) entity).item.getMaxAmount());
            if (((EntityItem) entity).item.getAdditionalData() != null)
                desc.add("Item additional data" + ((EntityItem) entity).item.getAdditionalData());
        } else if (entity instanceof EntityLiving) {
            desc.add("Health: " + ((EntityLiving) entity).getHealth() + " / " + ((EntityLiving) entity).getMaxHealth());
            desc.add("Regen rate: 1 HP every " + ((EntityLiving) entity).getRegenRate() + " ticks");
            if (entity instanceof AbstractEntityPlayer) {
                desc.add("");
                desc.add("Name: " + ((AbstractEntityPlayer) entity).getName());
                if (getNet().isActive())
                    desc.add("Command level: " + ((AbstractEntityPlayer) entity).getCommandLevel());
            }
        }
        getApiHandler().drawHoverInfoAtMouse(game, manager, graphics, true, 500, desc);
    }

    private void describeTile(TileState state, int x, int y, TileLayer layer, IWorld world, IAssetManager manager, Graphics graphics, IGameInstance game, AbstractEntityPlayer player) {
        Tile tile = state.getTile();
        ItemInstance itemInstance = null;
        if (tile.getItem() != null)
            itemInstance = new ItemInstance(tile);
        List<String> desc = new ArrayList<>();

        desc.add("Name: " + (itemInstance == null ? manager.localize(tile.getName()) : itemInstance.getDisplayName()));
        desc.add("State: " + state.toString());
        desc.add("Position: " + x + " : " + y);
        desc.add("Sky light: " + world.getSkyLight(x, y));
        desc.add("Artificial light: " + world.getArtificialLight(x, y));
        desc.add("Combined light: " + world.getCombinedLight(x, y));
        desc.add("Hardness: " + tile.getHardness(world, x, y, layer));
        desc.add("Light emitted: " + tile.getLight(world, x, y, layer));
        desc.add("Translucency: " + String.format("%.0f %%", tile.getTranslucentModifier(world, x, y, layer, true) * 100));
        String effective = getEffective(tile, world, x, y, layer);
        if (!effective.isEmpty())
            desc.add("Effective tool type: " + effective);
        if (tile.canProvideTileEntity()) {
            TileEntity te = world.getTileEntity(x, y);
            if (te != null) {
                if (te instanceof TileEntityFueled) {
                    desc.add("Active: " + ((TileEntityFueled) te).isActive());
                    desc.add("Fuel time: " + ((TileEntityFueled) te).getFuelPercentage());
                }
            }
        }
        desc.add("Biome: " + world.getBiome(x, y).getName());
        if (itemInstance != null)
            tile.describeItem(manager, itemInstance, desc, true);

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


        if (itemInstance == null || !getEventHandler().fireEvent(new TooltipEvent(itemInstance, game, manager, graphics, desc)).shouldCancel()) {
            getApiHandler().drawHoverInfoAtMouse(game, manager, graphics, true, 500, desc);
        }
    }

    private String getEffective(Tile tile, IWorld world, int x, int y, TileLayer layer) {
        StringBuilder builder = new StringBuilder();
        if (tile.isToolEffective(world, x, y, layer, ToolType.AXE, Integer.MAX_VALUE)) {
            builder.append("Axe");
        }
        if (tile.isToolEffective(world, x, y, layer, ToolType.PICKAXE, Integer.MAX_VALUE)) {
            if (builder.length() > 0)
                builder.append(", ");
            builder.append("Pickaxe");
        }
        if (tile.isToolEffective(world, x, y, layer, ToolType.SHOVEL, Integer.MAX_VALUE)) {
            if (builder.length() > 0)
                builder.append(", ");
            builder.append("Shovel");
        }
        if (tile.isToolEffective(world, x, y, layer, ToolType.SWORD, Integer.MAX_VALUE)) {
            if (builder.length() > 0)
                builder.append(", ");
            builder.append("Sword");
        }
        return builder.toString();
    }
}
