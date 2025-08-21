package com.mrbubba.auctionhouse.auction;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionMenu extends ChestMenu {
    private final ServerPlayer player;
    private final boolean myListings;
    private int page;
    private final SimpleContainer container;
    private final Map<Integer, Integer> slotToAuction = new HashMap<>();

    public AuctionMenu(int id, Inventory inv, ServerPlayer player, boolean myListings, int page) {
        this(id, inv, player, myListings, page, new SimpleContainer(54));
    }

    private AuctionMenu(int id, Inventory inv, ServerPlayer player, boolean myListings, int page, SimpleContainer container) {
        super(net.minecraft.world.inventory.MenuType.GENERIC_9x6, id, inv, container, 6);
        this.player = player;
        this.myListings = myListings;
        this.page = page;
        this.container = container;
        refresh();
    }

    private void refresh() {
        slotToAuction.clear();
        container.clearContent();
        List<AuctionManager.AuctionEntry> list = myListings ?
                AuctionManager.getAuctionsFor(player.getUUID()) :
                AuctionManager.getAllAuctions();
        int start = page * 45;
        int i = 0;
        for (int idx = start; idx < list.size() && i < 45; idx++) {
            AuctionManager.AuctionEntry entry = list.get(idx);
            ItemStack display = entry.stack().copy();
            display.set(DataComponents.CUSTOM_NAME, Component.literal("ID: " + entry.id() + " Price: " + entry.price()));
            container.setItem(i, display);
            slotToAuction.put(i, entry.id());
            i++;
        }
        boolean hasPrev = page > 0;
        ItemStack prev = new ItemStack(hasPrev ? Items.SLIME_BALL : Items.BARRIER);
        prev.set(DataComponents.CUSTOM_NAME, Component.literal("Previous Page"));
        container.setItem(45, prev);
        ItemStack pageItem = new ItemStack(Items.PAPER, page + 1);
        pageItem.set(DataComponents.CUSTOM_NAME, Component.literal("Page #" + (page + 1)));
        container.setItem(49, pageItem);
        boolean hasNext = list.size() > (start + 45);
        ItemStack next = new ItemStack(hasNext ? Items.SLIME_BALL : Items.BARRIER);
        next.set(DataComponents.CUSTOM_NAME, Component.literal("Next Page"));
        container.setItem(53, next);
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId < 0) return;
        if (slotId < 54) {
            if (slotId == 45 && page > 0) {
                if (myListings) {
                    AuctionManager.openMyListings(this.player, page - 1);
                } else {
                    AuctionManager.openBrowse(this.player, page - 1);
                }
                return;
            }
            if (slotId == 53) {
                List<AuctionManager.AuctionEntry> list = myListings ?
                        AuctionManager.getAuctionsFor(this.player.getUUID()) :
                        AuctionManager.getAllAuctions();
                if (list.size() > (page + 1) * 45) {
                    if (myListings) {
                        AuctionManager.openMyListings(this.player, page + 1);
                    } else {
                        AuctionManager.openBrowse(this.player, page + 1);
                    }
                }
                return;
            }
            if (slotId == 49) {
                return;
            }
            Integer id = slotToAuction.get(slotId);
            if (id != null) {
                if (myListings) {
                    AuctionManager.cancelAuction(this.player, id);
                    AuctionManager.openMyListings(this.player, page);
                } else {
                    AuctionManager.buyAuction(this.player, id);
                    AuctionManager.openBrowse(this.player, page);
                }
                return;
            }
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
