package alexanders.mods.inspector;

import de.ellpeck.rockbottom.api.GameContent;
import de.ellpeck.rockbottom.api.IApiHandler;
import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.assets.IAssetManager;
import de.ellpeck.rockbottom.api.construction.BasicRecipe;
import de.ellpeck.rockbottom.api.data.settings.Keybind;
import de.ellpeck.rockbottom.api.event.IEventHandler;
import de.ellpeck.rockbottom.api.event.impl.OverlayRenderEvent;
import de.ellpeck.rockbottom.api.event.impl.WorldRenderEvent;
import de.ellpeck.rockbottom.api.item.ItemBasic;
import de.ellpeck.rockbottom.api.item.ItemInstance;
import de.ellpeck.rockbottom.api.mod.IMod;
import de.ellpeck.rockbottom.api.tile.TileBasic;
import de.ellpeck.rockbottom.api.util.reg.IResourceName;
import org.newdawn.slick.Input;
import org.newdawn.slick.util.Log;

import static de.ellpeck.rockbottom.api.RockBottomAPI.KEYBIND_REGISTRY;
import static de.ellpeck.rockbottom.api.RockBottomAPI.createRes;

public class Inspector implements IMod{
    
    public static Inspector instance;
    public static ItemBasic inspectorItem;
    public static IResourceName inspectorName;
    public static Keybind keybind;
    
    
    public Inspector() {
        instance = this;
        IResourceName keybindName = createRes(this, "desc.keybind");
        KEYBIND_REGISTRY.register(keybindName, keybind = new Keybind(keybindName, Input.KEY_LCONTROL, false));
    }

    @Override
    public String getDisplayName(){
        return "Inspector";
    }

    @Override
    public String getId(){
        return "inspector";
    }

    @Override
    public String getVersion(){
        return "@VERSION@";
    }

    @Override
    public String getResourceLocation(){
        return "/assets/"+getId();
    }

    @Override
    public String getDescription(){
        return RockBottomAPI.getGame().getAssetManager().localize(createRes(this, "desc.mod"));
    }

    @Override
    public void init(IGameInstance game, IApiHandler apiHandler, IEventHandler eventHandler){
        inspectorItem = new ItemBasic(inspectorName = createRes(this, "inspector"));
        inspectorItem.register();
        eventHandler.registerListener(OverlayRenderEvent.class, new InspectorRenderer());   
    }
}
