/*
 * Copyright (C) 2018-2022 Zerthick
 *
 * This file is part of NoPortal.
 *
 * NoPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NoPortal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NoSleep.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.zerthick.noportal;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Plugin("noportal")
public class NoPortal {

    private final PluginContainer container;
    private final Logger logger;

    @Inject
    NoPortal(final PluginContainer container, final Logger logger) {
        this.container = container;
        this.logger = logger;
    }

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path configPath;

    private Component permissionText;

    @Listener
    public void onConstructPlugin(final ConstructPluginEvent event) {
        // Log Start Up to Console
        logger.info(
                container.metadata().name().orElse("Unknown Plugin") + " version " + container.metadata().version()
                        + " enabled!");

        // Load permission text from config
        loadConfig().ifPresent(c -> permissionText = LegacyComponentSerializer.legacyAmpersand().deserialize(c.node("NoPortalCreationPermissionError").getString("")));

        logger.error(permissionText);
    }

    //    @Listener
//    public void onPortalCreate(ChangeBlockEvent.Place event) {
//
//        boolean containsPortalBlocks = false;
//
//        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
//            if (transaction.getFinal().getState().getType().equals(BlockTypes.PORTAL)) {
//                containsPortalBlocks = true;
//                break;
//            }
//        }
//
//        if (containsPortalBlocks) {
//            Optional<Player> playerOptional = event.getCause().first(Player.class);
//
//            if (playerOptional.isPresent()) {
//                Player player = playerOptional.get();
//
//                if (!player.hasPermission("noportal.create")) {
//                    player.sendMessage(permissionText);
//                    event.setCancelled(true);
//                }
//            } else {
//                event.setCancelled(true);
//            }
//        }
//    }
//
//    @Listener
//    public void onPortalEnter(CollideBlockEvent event, @Root Player player) {
//
//        if (event.getTargetBlock().getType().equals(BlockTypes.PORTAL)) {
//
//            if (!player.hasPermission("noportal.enter")) {
//                event.setCancelled(true);
//            }
//        }
//    }
//
    private Optional<ConfigurationNode> loadConfig() {
        if (!configPath.toFile().exists()) {
            // Create config if not exists
            container.openResource(URI.create("default.conf")).ifPresent(r -> {
                try {
                    Files.copy(r, configPath);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            });
        }
        try {
            ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().path(configPath).build();
            return Optional.of(loader.load());
        } catch (ConfigurateException e) {
            logger.error(e.getMessage());
        }
        return Optional.empty();
    }
}
