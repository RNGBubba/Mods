# Auction House Mod

A server-side-only NeoForge mod for Minecraft 1.21.1 that adds a persistent player economy and an in-game auction house. Clients do not need to install anything.

## Features
- JSON-backed economy balances saved in `auctionhouse_balances.json`
- Player-driven auction listings stored in `auctionhouse_auctions.json`
- Automatic save and purge every five minutes with item returns on expiration
- Safe transaction handling that prevents negative balances or overflow
- Fully server-side commands and chest-based menus

## Economy Commands
- `/balance` – view your own balance
- `/balance <player>` – view another player's balance (op)
- `/pay <player> <amount>` – send currency to another player
- `/eco add|set|remove|delete <player> <amount>` – administrator balance controls
- `/eco save` and `/eco reload` – manually persist or reload balances from disk

## Auction Commands
- `/auction list <price>` – list the item in your hand
- `/auction buy <id>` – purchase an auction listing
- `/auction cancel <id>` – cancel one of your listings
- `/auction my` – view your active listings
- `/auction browse [page]` – open the global auction browser
- `/auction save` and `/auction reload` – administrator persistence controls

## Installation
1. Build the mod with `./gradlew build`.
2. Place the generated jar from `build/libs` into the server's `mods` folder.
3. Start the server; balances and auctions will load automatically and save on shutdown.

No client installation is required.
