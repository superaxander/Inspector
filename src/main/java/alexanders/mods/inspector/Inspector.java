package alexanders.mods.inspector;

import de.ellpeck.rockbottom.api.IApiHandler;
import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.data.settings.Keybind;
import de.ellpeck.rockbottom.api.event.IEventHandler;
import de.ellpeck.rockbottom.api.event.impl.OverlayRenderEvent;
import de.ellpeck.rockbottom.api.item.ItemBasic;
import de.ellpeck.rockbottom.api.mod.IMod;
import de.ellpeck.rockbottom.api.util.reg.IResourceName;
import org.lwjgl.input.Keyboard;

import static de.ellpeck.rockbottom.api.RockBottomAPI.createRes;

public class Inspector implements IMod {

    public static Inspector instance;
    public static ItemBasic inspectorItem;
    public static IResourceName inspectorName;
    public static Keybind keybind;


    public Inspector() {
        instance = this;
        keybind = new Keybind(createRes(this, "desc.keybind"), Keyboard.KEY_LCONTROL, false).register();
    }

    @Override
    public String getDisplayName() {
        return "Inspector";
    }

    @Override
    public String getId() {
        return "inspector";
    }

    @Override
    public String getVersion() {
        return "@VERSION@";
    }

    @Override
    public String getResourceLocation() {
        return "/assets/" + getId();
    }

    @Override
    public String getDescription() {
        return RockBottomAPI.getGame().getAssetManager().localize(createRes(this, "desc.mod"));
    }

    @Override
    public void init(IGameInstance game, IApiHandler apiHandler, IEventHandler eventHandler) {
        inspectorItem = new ItemBasic(inspectorName = createRes(this, "inspector"));
        inspectorItem.register();
        eventHandler.registerListener(OverlayRenderEvent.class, new InspectorRenderer());
    }
}
