package com.henclient.reborn.modules.exploration;

import com.henclient.reborn.module.Category;
import com.henclient.reborn.module.Module;
import com.henclient.reborn.module.setting.BooleanSetting;
import com.henclient.reborn.module.setting.NumberSetting;
import com.henclient.reborn.util.BlockEntityScanner;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Lists nearby chests, barrels and shulker boxes found in already-loaded chunks. */
public final class StorageFinderModule extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("range", "Range (chunks)", 6, 1, 16, 1));
    private final NumberSetting interval = addSetting(new NumberSetting("interval", "Scan interval (ticks)", 40, 10, 200, 10));
    private final NumberSetting maxResults = addSetting(new NumberSetting("max", "Max results", 30, 5, 100, 5));
    private final BooleanSetting chests = addSetting(new BooleanSetting("chests", "Chests", true));
    private final BooleanSetting barrels = addSetting(new BooleanSetting("barrels", "Barrels", true));
    private final BooleanSetting shulkers = addSetting(new BooleanSetting("shulkers", "Shulker boxes", true));

    private List<BlockEntityScanner.Hit> hits = new ArrayList<>();
    private int ticks;

    public StorageFinderModule() {
        super("Storage Finder", "Lists nearby chests, barrels and shulker boxes in loaded chunks.", Category.EXPLORATION);
    }

    @Override
    public String note() {
        return "Only searches chunks your client has already loaded; it never loads new chunks.";
    }

    @Override
    protected void onEnable() {
        ticks = 0;
    }

    @Override
    protected void onDisable() {
        hits = new ArrayList<>();
    }

    @Override
    public void onWorldUnload() {
        hits = new ArrayList<>();
    }

    @Override
    public void onTick(Minecraft mc) {
        if (ticks-- > 0) {
            return;
        }
        ticks = interval.getInt();
        boolean c = chests.isOn(), b = barrels.isOn(), s = shulkers.isOn();
        hits = BlockEntityScanner.scan(mc, range.getInt(), maxResults.getInt(),
                be -> BlockEntityScanner.storageKind(be, c, b, s));
    }

    @Override
    public List<String> overlayLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Storage: " + hits.size() + " found");
        int shown = Math.min(5, hits.size());
        for (int i = 0; i < shown; i++) {
            BlockEntityScanner.Hit h = hits.get(i);
            lines.add(" " + h.kind() + "  " + h.pos().getX() + " " + h.pos().getY() + " " + h.pos().getZ()
                    + "  (" + Math.round(Math.sqrt(h.distSq())) + "m)");
        }
        return lines;
    }
}
