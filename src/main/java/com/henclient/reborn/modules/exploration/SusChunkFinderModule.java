package com.henclient.reborn.modules.exploration;

import com.henclient.reborn.module.Category;
import com.henclient.reborn.module.Module;
import com.henclient.reborn.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Heuristic detector for unusual underground chunks. It scores each loaded chunk by counting blocks
 * that commonly indicate structures or player activity (chests, spawners, obsidian, cobblestone,
 * mossy cobblestone, torches, rails). A high score is only a hint and proves nothing.
 *
 * Scanning is incremental: a few chunks per tick, each cached and re-scanned only after a delay.
 */
public final class SusChunkFinderModule extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("range", "Range (chunks)", 8, 2, 16, 1));
    private final NumberSetting perTick = addSetting(new NumberSetting("perTick", "Chunks per tick", 1, 1, 6, 1));
    private final NumberSetting rescan = addSetting(new NumberSetting("rescan", "Re-scan after (sec)", 30, 5, 300, 5));
    private final NumberSetting maxY = addSetting(new NumberSetting("maxY", "Only below Y", 48, -60, 120, 4));
    private final NumberSetting threshold = addSetting(new NumberSetting("threshold", "Score threshold", 30, 5, 200, 5));
    private final NumberSetting showCount = addSetting(new NumberSetting("show", "Rows shown", 5, 1, 10, 1));

    private record Entry(int cx, int cz, double score, int chests, int spawners, long tick) {
    }

    private final Map<Long, Entry> cache = new HashMap<>();
    private long tickCounter;

    public SusChunkFinderModule() {
        super("Sus Chunk Finder", "Flags chunks with unusual underground block concentrations (heuristic).", Category.EXPLORATION);
    }

    @Override
    public String note() {
        return "HEURISTIC ONLY: scores chunks by counting chests, spawners, obsidian, cobblestone, mossy "
                + "cobblestone, torches and rails below the chosen Y. A high score is a hint, not proof of a base or structure.";
    }

    @Override
    protected void onDisable() {
        cache.clear();
    }

    @Override
    public void onWorldUnload() {
        cache.clear();
    }

    @Override
    public void onTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        tickCounter++;
        int pcx = player.getBlockX() >> 4;
        int pcz = player.getBlockZ() >> 4;
        int r = range.getInt();

        // Evict entries that are out of range or whose chunk has unloaded.
        if (tickCounter % 40 == 0) {
            Iterator<Entry> it = cache.values().iterator();
            while (it.hasNext()) {
                Entry e = it.next();
                boolean far = Math.abs(e.cx - pcx) > r + 1 || Math.abs(e.cz - pcz) > r + 1;
                if (far || !(mc.level.getChunk(e.cx, e.cz, ChunkStatus.FULL, false) instanceof LevelChunk)) {
                    it.remove();
                }
            }
        }

        long maxAge = rescan.getInt() * 20L;
        int budget = perTick.getInt();
        outer:
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                long key = key(cx, cz);
                Entry existing = cache.get(key);
                if (existing != null && tickCounter - existing.tick < maxAge) {
                    continue;
                }
                ChunkAccess access = mc.level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) {
                    continue;
                }
                cache.put(key, scan(chunk, cx, cz));
                if (--budget <= 0) {
                    break outer;
                }
            }
        }
    }

    private Entry scan(LevelChunk chunk, int cx, int cz) {
        int limitY = maxY.getInt();
        int chests = 0, spawners = 0, obsidian = 0, cobble = 0, mossy = 0, torches = 0, rails = 0;
        LevelChunkSection[] sections = chunk.getSections();
        int minSection = chunk.getMinSectionY();
        for (int i = 0; i < sections.length; i++) {
            int baseY = (minSection + i) << 4;
            if (baseY > limitY) {
                continue;
            }
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            for (int y = 0; y < 16 && baseY + y <= limitY; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Block b = section.getBlockState(x, y, z).getBlock();
                        if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST) {
                            chests++;
                        } else if (b == Blocks.SPAWNER) {
                            spawners++;
                        } else if (b == Blocks.OBSIDIAN) {
                            obsidian++;
                        } else if (b == Blocks.COBBLESTONE) {
                            cobble++;
                        } else if (b == Blocks.MOSSY_COBBLESTONE) {
                            mossy++;
                        } else if (b == Blocks.TORCH || b == Blocks.WALL_TORCH) {
                            torches++;
                        } else if (b == Blocks.RAIL || b == Blocks.POWERED_RAIL
                                || b == Blocks.DETECTOR_RAIL || b == Blocks.ACTIVATOR_RAIL) {
                            rails++;
                        }
                    }
                }
            }
        }
        double score = chests * 3.0 + spawners * 25.0 + obsidian * 0.3 + cobble * 0.08
                + mossy * 1.5 + torches * 2.0 + rails * 1.2;
        return new Entry(cx, cz, score, chests, spawners, tickCounter);
    }

    private static long key(int cx, int cz) {
        return ((long) cx & 0xFFFFFFFFL) | (((long) cz & 0xFFFFFFFFL) << 32);
    }

    @Override
    public List<String> overlayLines() {
        List<String> lines = new ArrayList<>();
        List<Entry> flagged = new ArrayList<>();
        for (Entry e : cache.values()) {
            if (e.score >= threshold.getDouble()) {
                flagged.add(e);
            }
        }
        flagged.sort(Comparator.comparingDouble((Entry e) -> e.score).reversed());
        lines.add("Sus chunks: " + flagged.size() + " (heuristic)");
        int shown = Math.min(showCount.getInt(), flagged.size());
        for (int i = 0; i < shown; i++) {
            Entry e = flagged.get(i);
            lines.add(" " + (e.cx << 4) + ", " + (e.cz << 4) + "  score " + Math.round(e.score)
                    + (e.spawners > 0 ? "  spawner" : "") + (e.chests > 0 ? "  chests " + e.chests : ""));
        }
        return lines;
    }
}
