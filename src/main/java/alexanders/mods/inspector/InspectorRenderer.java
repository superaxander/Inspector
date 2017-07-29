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
import de.ellpeck.rockbottom.api.tile.Tile;
import de.ellpeck.rockbottom.api.tile.state.TileState;
import de.ellpeck.rockbottom.api.util.BoundBox;
import de.ellpeck.rockbottom.api.util.Util;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.TileLayer;
import org.newdawn.slick.Graphics;

import java.util.ArrayList;
import java.util.List;

import static de.ellpeck.rockbottom.api.RockBottomAPI.getApiHandler;
import static de.ellpeck.rockbottom.api.RockBottomAPI.getEventHandler;

public class InspectorRenderer implements IEventListener<OverlayRenderEvent> {
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
                    if(entities.size() > 0) {
                        describeEntity(entities.get(0), mouseX, mouseY, event.assetManager, event.graphics, game);
                    }else {
                        TileState tile;
                        if (Settings.KEY_BACKGROUND.isDown())
                            tile = world.getState(TileLayer.BACKGROUND, x, y);
                        else
                            tile = world.getState(TileLayer.MAIN, x, y);
                        describeTile(tile, x, y, world, event.assetManager, event.graphics, game);
                    }
                }
            }
        }
        return result;
    }

    private void describeEntity(Entity entity, double x, double y, IAssetManager manager, Graphics graphics, IGameInstance game) {
        List<String> desc = new ArrayList<>();
        desc.add("UUID: "+entity.getUniqueId());
        desc.add("Position: "+ String.format("%.3f : %.3f", entity.x, entity.y));
        if(entity instanceof EntityItem) {
            ((EntityItem) entity).item.getItem().describeItem(manager, ((EntityItem) entity).item, desc, true);
        }else if(entity instanceof EntityLiving) {
            desc.add("Health: "+ ((EntityLiving) entity).getHealth() + " / "+ ((EntityLiving) entity).getMaxHealth());
            desc.add("Regen rate: 1 HP every "+ ((EntityLiving) entity).getRegenRate()+ " ticks");
            if(entity instanceof AbstractEntityPlayer) {
                desc.add("Name: "+((AbstractEntityPlayer) entity).getName());
            }
        }
        getApiHandler().drawHoverInfoAtMouse(game, manager, graphics, true, 500, desc);
    }

    private void describeTile(TileState state, int x, int y, IWorld world, IAssetManager manager, Graphics graphics, IGameInstance game) {
        Tile tile = state.getTile();
        ItemInstance itemInstance = null;
        if (tile.getItem() != null)
            itemInstance = new ItemInstance(tile);
        List<String> desc = new ArrayList<>();

        desc.add("Name: " + (itemInstance == null ? manager.localize(tile.getName()) : itemInstance.getDisplayName()));
        desc.add("State: " + state.toString());
        desc.add("Position: " + x + " : " + y);
        desc.add("Light: " + world.getCombinedLight(x, y));
        if (itemInstance != null)
            tile.describeItem(manager, itemInstance, desc, true);

        if (itemInstance == null || !getEventHandler().fireEvent(new TooltipEvent(itemInstance, game, manager, graphics, desc)).shouldCancel()) {
            getApiHandler().drawHoverInfoAtMouse(game, manager, graphics, true, 500, desc);
        }
    }
}
