package com.mrbubba.auctionhouse.command;

import com.mrbubba.auctionhouse.auction.AuctionManager;
import com.mrbubba.auctionhouse.economy.EconomyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eco")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            long amount = LongArgumentType.getLong(ctx, "amount");
                            EconomyManager.add(target.getUUID(), amount);
                            ctx.getSource().sendSuccess(() -> Component.literal("Added " + amount + " to " + target.getName().getString()), true);
                            return 1;
                        }))))
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            long amount = LongArgumentType.getLong(ctx, "amount");
                            EconomyManager.set(target.getUUID(), amount);
                            ctx.getSource().sendSuccess(() -> Component.literal("Set balance of " + target.getName().getString() + " to " + amount), true);
                            return 1;
                        }))))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            long amount = LongArgumentType.getLong(ctx, "amount");
                            EconomyManager.remove(target.getUUID(), amount);
                            ctx.getSource().sendSuccess(() -> Component.literal("Removed " + amount + " from " + target.getName().getString()), true);
                            return 1;
                        }))))
            .then(Commands.literal("delete")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        EconomyManager.delete(target.getUUID());
                        ctx.getSource().sendSuccess(() -> Component.literal("Deleted balance of " + target.getName().getString()), true);
                        return 1;
                    })))
            .then(Commands.literal("save")
                .executes(ctx -> {
                    EconomyManager.save(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Balances saved"), true);
                    return 1;
                }))
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    EconomyManager.load(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Balances reloaded"), true);
                    return 1;
                }))
        );

        dispatcher.register(Commands.literal("balance")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                long bal = EconomyManager.getBalance(player.getUUID());
                ctx.getSource().sendSuccess(() -> Component.literal("Balance: " + bal), false);
                return 1;
            })
            .then(Commands.argument("player", EntityArgument.player())
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    long bal = EconomyManager.getBalance(target.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " balance: " + bal), false);
                    return 1;
                })));

        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                    .executes(ctx -> {
                        ServerPlayer from = ctx.getSource().getPlayerOrException();
                        ServerPlayer to = EntityArgument.getPlayer(ctx, "player");
                        long amount = LongArgumentType.getLong(ctx, "amount");
                        if (EconomyManager.transfer(from.getUUID(), to.getUUID(), amount)) {
                            ctx.getSource().sendSuccess(() -> Component.literal("Paid " + amount + " to " + to.getName().getString()), true);
                            return 1;
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Insufficient funds"));
                            return 0;
                        }
                    }))));

        dispatcher.register(Commands.literal("auction")
            .then(Commands.literal("list")
                .then(Commands.argument("price", LongArgumentType.longArg(1))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        long price = LongArgumentType.getLong(ctx, "price");
                        var stack = player.getMainHandItem();
                        if (stack.isEmpty()) {
                            ctx.getSource().sendFailure(Component.literal("Hold an item to list it."));
                            return 0;
                        }
                        ItemStack toList = stack.copy();
                        toList.setCount(1);
                        if (AuctionManager.listAuction(player, toList, price)) {
                            stack.shrink(1);
                            ctx.getSource().sendSuccess(() -> Component.literal("Listed item for " + price), true);
                            return 1;
                        } else {
                            ctx.getSource().sendFailure(Component.literal("You have too many active auctions."));
                            return 0;
                        }
                    })))
            .then(Commands.literal("buy")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        int id = IntegerArgumentType.getInteger(ctx, "id");
                        if (AuctionManager.buyAuction(player, id)) {
                            ctx.getSource().sendSuccess(() -> Component.literal("Purchased auction " + id), true);
                            return 1;
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Unable to purchase auction."));
                            return 0;
                        }
                    })))
            .then(Commands.literal("cancel")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        int id = IntegerArgumentType.getInteger(ctx, "id");
                        if (AuctionManager.cancelAuction(player, id)) {
                            ctx.getSource().sendSuccess(() -> Component.literal("Cancelled auction " + id), true);
                            return 1;
                        } else {
                            ctx.getSource().sendFailure(Component.literal("Unable to cancel auction."));
                            return 0;
                        }
                    })))
            .then(Commands.literal("my")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    AuctionManager.openMyListings(player);
                    return 1;
                }))
            .then(Commands.literal("browse")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    AuctionManager.openBrowse(player);
                    return 1;
                })
                .then(Commands.argument("page", IntegerArgumentType.integer(0))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        int page = IntegerArgumentType.getInteger(ctx, "page");
                        AuctionManager.openBrowse(player, page);
                        return 1;
                    })))
            .then(Commands.literal("save")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    AuctionManager.save(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Auctions saved"), true);
                    return 1;
                }))
            .then(Commands.literal("reload")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    AuctionManager.load(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Auctions reloaded"), true);
                    return 1;
                }))
        );
    }
}
