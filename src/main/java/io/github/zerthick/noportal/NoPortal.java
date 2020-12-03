/*
 * Copyright (C) 2018  Zerthick
 *
 * This file is part of NoPortal.
 *
 * NoPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * NoPortal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NoPortal.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.zerthick.noportal;

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(
        id = "noportal",
        name = "NoPortal",
        description = "A simple plugin to prevent creating Nether Portals.",
        authors = {
                "Zerthick"
        }
)
public class NoPortal {

    @Inject
    private Logger logger;

    @Inject
    private PluginContainer instance;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path config;
    private Text permissionText;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {

        // Log Start Up to Console
        logger.info(
                instance.getName() + " version " + instance.getVersion().orElse("unknown")
                        + " enabled!");

        loadConfig();
    }

    @Listener
    public void onPortalCreate(ChangeBlockEvent.Place event) {

        boolean containsPortalBlocks = false;

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            if (transaction.getFinal().getState().getType().equals(BlockTypes.PORTAL)) {
                containsPortalBlocks = true;
                break;
            }
        }

        if (containsPortalBlocks) {
            Optional<Player> playerOptional = event.getCause().first(Player.class);

            if (playerOptional.isPresent()) {
                Player player = playerOptional.get();

                if (!player.hasPermission("noportal.create")) {
                    player.sendMessage(permissionText);
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onPortalEnter(CollideBlockEvent event, @Root Player player) {

        if (event.getTargetBlock().getType().equals(BlockTypes.PORTAL)) {

            if (!player.hasPermission("noportal.enter")) {
                event.setCancelled(true);
            }
        }
    }

    private void loadConfig() {
        if (!config.toFile().exists()) {
            // Create config if not exists
            try {
                InputStream in = this.getClass().getResourceAsStream("/config.conf");
                OutputStream out = new FileOutputStream(config.toFile());
                byte[] buff = new byte[1024];
                int len;
                while ((len = in.read(buff)) > 0) {
                    out.write(buff, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(config).build();
            ConfigurationNode rootNode = loader.load().getNode();
            permissionText = TextSerializers.FORMATTING_CODE.deserialize(rootNode.getNode("NoPortalCreationPermissionError").getString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
