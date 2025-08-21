package com.mrbubba.auctionhouse.auction;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AuctionMenu extends ChestMenu {
    private final ServerPlayer player;
    private final boolean mine;
    private int page;
    private final Map<Integer, Integer> slotToId = new HashMap<>();
    private final net.minecraft.world.SimpleContainer internal;
    private boolean hasNext;

    AuctionMenu(int id, ServerPlayer player, boolean mine, int page) {
        this(id, player.getInventory(), player, mine, page, new net.minecraft.world.SimpleContainer(54));
    }

    private AuctionMenu(int id, Inventory inv, ServerPlayer player, boolean mine, int page, net.minecraft.world.SimpleContainer container) {
        super(MenuType.GENERIC_9x6, id, inv, container, 6);
        this.player = player;
        this.mine = mine;
        this.page = page;
        this.internal = container;
        refresh();
    }

    @Override
    public boolean stillValid(Player p) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player clicker) {
        if (slotId == 45 && page > 0) {
            page--;
            refresh();
            return;
        }
        if (slotId == 53 && hasNext) {
            page++;
            refresh();
            return;
        }
        if (slotId == 49) {
            return;
        }
        Integer auctionId = slotToId.get(slotId);
        if (auctionId != null) {
            if (mine) {
                AuctionManager.cancelAuction(player, auctionId);
            } else {
                AuctionManager.buyAuction(player, auctionId);
            }
            refresh();
        }
        clicker.containerMenu.setCarried(ItemStack.EMPTY);
    }

    private void refresh() {
        internal.clearContent();
        slotToId.clear();
        List<AuctionManager.AuctionEntry> list = AuctionManager.getAuctions(mine, player.getUUID());
        int start = page * 45;
        int total = list.size();
        int slot = 0;
        for (int idx = start; idx < total && slot < 45; idx++) {
            AuctionManager.AuctionEntry entry = list.get(idx);
            ItemStack display = entry.stack().copy();
            display.set(DataComponents.CUSTOM_NAME, Component.literal("ID: " + entry.id() + " Price: " + entry.price()));
            internal.setItem(slot, display);
            slotToId.put(slot, entry.id());
            slot++;
        }
        boolean hasPrev = page > 0;
        hasNext = start + 45 < total;
        ItemStack prev = new ItemStack(hasPrev ? Items.SLIME_BALL : Items.BARRIER);
        prev.set(DataComponents.CUSTOM_NAME, Component.literal("Previous Page"));
        internal.setItem(45, prev);
        ItemStack next = new ItemStack(hasNext ? Items.SLIME_BALL : Items.BARRIER);
        next.set(DataComponents.CUSTOM_NAME, Component.literal("Next Page"));
        internal.setItem(53, next);
        ItemStack pageNum = new ItemStack(Items.PAPER, page + 1);
        pageNum.set(DataComponents.CUSTOM_NAME, Component.literal("Page #" + (page + 1)));
        internal.setItem(49, pageNum);
    }
}
