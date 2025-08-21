package com.mrbubba.auctionhouse;

import com.mojang.logging.LogUtils;
import com.mrbubba.auctionhouse.auction.AuctionManager;
import com.mrbubba.auctionhouse.command.ModCommands;
import com.mrbubba.auctionhouse.economy.EconomyManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(value = AuctionHouseMod.MODID, dist = Dist.DEDICATED_SERVER)
public class AuctionHouseMod {
    public static final String MODID = "auctionhouse";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AuctionHouseMod(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("AuctionHouseMod setup complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        EconomyManager.load(event.getServer());
        AuctionManager.load(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        EconomyManager.save(event.getServer());
        AuctionManager.save(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer() != null) {
            EconomyManager.tick(event.getServer());
            AuctionManager.tick(event.getServer());
        }
    }
}
