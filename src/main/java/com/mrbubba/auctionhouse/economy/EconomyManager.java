package com.mrbubba.auctionhouse.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mrbubba.auctionhouse.AuctionHouseMod;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Long> BALANCES = new HashMap<>();
    private static final int SAVE_INTERVAL_TICKS = 20 * 60 * 5; // 5 minutes
    private static int tickCounter = 0;

    private static Path getFile(MinecraftServer server) {
        return server.getServerDirectory().resolve("auctionhouse_balances.json");
    }

    public static void load(MinecraftServer server) {
        BALANCES.clear();
        Path file = getFile(server);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                if (obj != null) {
                    for (String key : obj.keySet()) {
                        BALANCES.put(UUID.fromString(key), obj.get(key).getAsLong());
                    }
                }
            } catch (IOException e) {
                AuctionHouseMod.LOGGER.error("Failed to load balances", e);
            }
        }
    }

    public static void save(MinecraftServer server) {
        Path file = getFile(server);
        JsonObject obj = new JsonObject();
        BALANCES.forEach((uuid, bal) -> obj.addProperty(uuid.toString(), bal));
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(obj, writer);
            }
        } catch (IOException e) {
            AuctionHouseMod.LOGGER.error("Failed to save balances", e);
        }
    }

    public static long getBalance(UUID id) {
        return BALANCES.getOrDefault(id, 0L);
    }

    public static void add(UUID id, long amount) {
        if (amount < 0) return;
        long current = getBalance(id);
        if (Long.MAX_VALUE - current < amount) {
            BALANCES.put(id, Long.MAX_VALUE);
        } else {
            BALANCES.put(id, current + amount);
        }
    }

    public static void set(UUID id, long amount) {
        BALANCES.put(id, Math.max(0, amount));
    }

    public static void remove(UUID id, long amount) {
        if (amount < 0) return;
        long current = getBalance(id);
        BALANCES.put(id, Math.max(0, current - amount));
    }

    public static void delete(UUID id) {
        BALANCES.remove(id);
    }

    public static boolean transfer(UUID from, UUID to, long amount) {
        if (amount < 0) return false;
        long fromBal = getBalance(from);
        if (fromBal < amount) return false;
        BALANCES.put(from, fromBal - amount);
        add(to, amount);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (++tickCounter >= SAVE_INTERVAL_TICKS) {
            save(server);
            tickCounter = 0;
        }
    }
}
