package com.mrbubba.auctionhouse.auction;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.mrbubba.auctionhouse.AuctionHouseMod;
import com.mrbubba.auctionhouse.economy.EconomyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionManager {
    public record AuctionEntry(int id, UUID seller, ItemStack stack, long price, long expiresAt){}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Integer, AuctionEntry> AUCTIONS = new LinkedHashMap<>();
    private static int NEXT_ID = 1;
    private static final long DEFAULT_DURATION_MS = 7L * 24 * 60 * 60 * 1000; // 7 days
    private static final int MAX_PER_PLAYER = 10;
    private static final int SAVE_INTERVAL_TICKS = 20 * 60 * 5; // 5 minutes
    private static int tickCounter = 0;

    private static Path getFile(MinecraftServer server) {
        return server.getServerDirectory().resolve("auctionhouse_auctions.json");
    }

    public static void load(MinecraftServer server) {
        AUCTIONS.clear();
        NEXT_ID = 1;
        Path file = getFile(server);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonArray arr = GSON.fromJson(reader, JsonArray.class);
                if (arr != null) {
                    for (JsonElement el : arr) {
                        JsonObject obj = el.getAsJsonObject();
                        int id = obj.get("id").getAsInt();
                        UUID seller = UUID.fromString(obj.get("seller").getAsString());
                        long price = obj.get("price").getAsLong();
                        long expires = obj.has("expires") ? obj.get("expires").getAsLong() : (System.currentTimeMillis() + DEFAULT_DURATION_MS);
                        ItemStack stack = ItemStack.CODEC.parse(JsonOps.INSTANCE, obj.get("item"))
                            .result().orElse(ItemStack.EMPTY);
                        if (!stack.isEmpty()) {
                            AUCTIONS.put(id, new AuctionEntry(id, seller, stack, price, expires));
                            if (id >= NEXT_ID) NEXT_ID = id + 1;
                        }
                    }
                }
            } catch (IOException e) {
                AuctionHouseMod.LOGGER.error("Failed to load auctions", e);
            }
        }
        purgeExpired(server);
    }

    public static void save(MinecraftServer server) {
        JsonArray arr = new JsonArray();
        for (AuctionEntry entry : AUCTIONS.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", entry.id());
            obj.addProperty("seller", entry.seller().toString());
            obj.addProperty("price", entry.price());
            obj.addProperty("expires", entry.expiresAt());
            ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, entry.stack())
                .result().ifPresent(json -> obj.add("item", json));
            arr.add(obj);
        }
        Path file = getFile(server);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(arr, writer);
            }
        } catch (IOException e) {
            AuctionHouseMod.LOGGER.error("Failed to save auctions", e);
        }
    }

    public static boolean listAuction(ServerPlayer player, ItemStack stack, long price) {
        if (price <= 0) return false;
        if (countListings(player.getUUID()) >= MAX_PER_PLAYER) {
            return false;
        }
        AUCTIONS.put(NEXT_ID, new AuctionEntry(NEXT_ID, player.getUUID(), stack.copy(), price, System.currentTimeMillis() + DEFAULT_DURATION_MS));
        NEXT_ID++;
        return true;
    }

    public static boolean buyAuction(ServerPlayer buyer, int id) {
        AuctionEntry entry = AUCTIONS.get(id);
        if (entry == null) return false;
        if (entry.seller().equals(buyer.getUUID())) return false;
        if (entry.expiresAt() <= System.currentTimeMillis()) return false;
        if (!EconomyManager.transfer(buyer.getUUID(), entry.seller(), entry.price())) return false;

        ItemStack stack = entry.stack().copy();
        if (!buyer.addItem(stack)) {
            buyer.drop(stack, false);
        }
        AUCTIONS.remove(id);
        return true;
    }

    public static boolean cancelAuction(ServerPlayer player, int id) {
        AuctionEntry entry = AUCTIONS.get(id);
        if (entry == null) return false;
        if (!entry.seller().equals(player.getUUID()) && !player.hasPermissions(2)) return false;
        ItemStack stack = entry.stack().copy();
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        AUCTIONS.remove(id);
        return true;
    }

    public static void openBrowse(ServerPlayer player) {
        openBrowse(player, 0);
    }

    public static void openBrowse(ServerPlayer player, int page) {
        purgeExpired(player.server);
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Auction House");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new AuctionMenu(id, inv, (ServerPlayer) p, false, page);
            }
        };
        player.openMenu(provider);
    }

    public static void openMyListings(ServerPlayer player) {
        openMyListings(player, 0);
    }

    public static void openMyListings(ServerPlayer player, int page) {
        purgeExpired(player.server);
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Your Auctions");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new AuctionMenu(id, inv, (ServerPlayer) p, true, page);
            }
        };
        player.openMenu(provider);
    }

    public static List<AuctionEntry> getAllAuctions() {
        return new ArrayList<>(AUCTIONS.values());
    }

    public static List<AuctionEntry> getAuctionsFor(UUID seller) {
        List<AuctionEntry> list = new ArrayList<>();
        for (AuctionEntry entry : AUCTIONS.values()) {
            if (entry.seller().equals(seller)) {
                list.add(entry);
            }
        }
        return list;
    }

    private static long countListings(UUID id) {
        return AUCTIONS.values().stream().filter(e -> e.seller().equals(id)).count();
    }

    private static void purgeExpired(MinecraftServer server) {
        long now = System.currentTimeMillis();
        AUCTIONS.entrySet().removeIf(entry -> {
            AuctionEntry value = entry.getValue();
            if (value.expiresAt() > now) return false;
            ItemStack stack = value.stack().copy();
            ServerPlayer seller = server.getPlayerList().getPlayer(value.seller());
            if (seller != null) {
                if (!seller.addItem(stack)) {
                    seller.drop(stack, false);
                }
            } else {
                Level level = server.overworld();
                BlockPos pos = level.getSharedSpawnPos();
                ItemEntity entity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                level.addFreshEntity(entity);
            }
            return true;
        });
    }

    public static void tick(MinecraftServer server) {
        if (++tickCounter >= SAVE_INTERVAL_TICKS) {
            purgeExpired(server);
            save(server);
            tickCounter = 0;
        }
    }
}
