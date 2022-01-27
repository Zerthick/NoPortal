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
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.transaction.BlockTransaction;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
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
import java.util.List;
import java.util.Optional;

@Plugin("noportal")
public class NoPortal {

    private final PluginContainer container;
    private final Logger logger;
    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path configPath;
    private Component permissionText;

    @Inject
    NoPortal(final PluginContainer container, final Logger logger) {
        this.container = container;
        this.logger = logger;
    }

    @Listener
    public void onConstructPlugin(final ConstructPluginEvent event) {
        // Log Start Up to Console
        logger.info(
                container.metadata().name().orElse("Unknown Plugin") + " version " + container.metadata().version()
                        + " enabled!");

        // Load permission text from config
        Optional<ConfigurationNode> configOptional = loadConfig();

        if (configOptional.isPresent()) {
            ConfigurationNode config = configOptional.get();
            permissionText = LegacyComponentSerializer.legacyAmpersand().deserialize(config.node("NoPortalNetherPortalCreationPermissionError").getString(""));
        } else {
            logger.error("Unable to load configuration file!");
        }
    }

    @Listener
    public void onPortalCreate(ChangeBlockEvent.All event, @Getter("transactions") List<BlockTransaction> transactions) {

        for (BlockTransaction transaction : transactions) {
            if (transaction.finalReplacement().state().type().equals(BlockTypes.NETHER_PORTAL.get())) {

                Optional<ServerPlayer> playerOptional = event.cause().first(ServerPlayer.class);

                if (playerOptional.isPresent()) {
                    ServerPlayer player = playerOptional.get();
                    if (!player.hasPermission("noportal.netherportal.create")) {
                        player.sendMessage(permissionText);
                        event.invalidateAll();
                    }
                } else {
                    event.invalidateAll();
                }
                break;
            }
        }
    }

    @Listener
    public void onPortalEnter(CollideBlockEvent event, @Root ServerPlayer player, @Getter("targetBlock") BlockState targetBlock) {

        if (targetBlock.type().equals(BlockTypes.NETHER_PORTAL.get())) {
            if (!player.hasPermission("noportal.netherportal.enter")) {
                event.setCancelled(true);
            }
        }

        if (targetBlock.type().equals(BlockTypes.END_PORTAL.get())) {
            if (!player.hasPermission("noportal.endportal.enter")) {
                event.setCancelled(true);
            }
        }
    }

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
