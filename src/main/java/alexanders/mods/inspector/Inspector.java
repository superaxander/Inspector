package alexanders.mods.inspector;

import de.ellpeck.rockbottom.api.IApiHandler;
import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.data.settings.Keybind;
import de.ellpeck.rockbottom.api.event.IEventHandler;
import de.ellpeck.rockbottom.api.event.impl.OverlayRenderEvent;
import de.ellpeck.rockbottom.api.item.ItemBasic;
import de.ellpeck.rockbottom.api.mod.IMod;
import de.ellpeck.rockbottom.api.util.reg.ResourceName;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;

public class Inspector implements IMod {

    public static Inspector instance;
    public static ItemBasic inspectorItem;
    public static ResourceName inspectorName;
    public static Keybind keybind;


    public Inspector() {
        instance = this;
        keybind = new Keybind(new ResourceName(this, "desc.keybind"), GLFW_KEY_LEFT_CONTROL).register();
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
        return "assets/" + getId();
    }

    @Override
    public String getDescription() {
        return RockBottomAPI.getGame().getAssetManager().localize(new ResourceName(this, "desc.mod"));
    }

    @Override
    public void init(IGameInstance game, IApiHandler apiHandler, IEventHandler eventHandler) {
        inspectorItem = new ItemBasic(inspectorName = new ResourceName(this, "inspector"));
        inspectorItem.register();
        eventHandler.registerListener(OverlayRenderEvent.class, new InspectorRenderer());
    }
}
