package com.magpie.infinitefusion;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class FusionRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceManager, LoadedRegistry> CACHE = new WeakHashMap<>();

    private FusionRegistry() {
    }

    public static Optional<FusionDefinition> findFusion(ServerPlayer player, Pokemon donor, Pokemon host) {
        return get(player.getServer()).findFusion(donor, host);
    }

    public static LoadedRegistry get(MinecraftServer server) {
        ResourceManager resourceManager = server.getResourceManager();
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(resourceManager, FusionRegistry::load);
        }
    }

    private static LoadedRegistry load(ResourceManager resourceManager) {
        Map<String, FusionDefinition> byId = new LinkedHashMap<>();
        Map<FusionKey, FusionDefinition> byPair = new LinkedHashMap<>();
        Map<String, TransformationDefinition> transformations = new LinkedHashMap<>();

        loadFusionFiles(resourceManager, byId, byPair);
        loadTransformationFiles(resourceManager, byId, transformations);

        LOGGER.info("[infinitefusion] Loaded {} fusion entries and {} transformation entries", byId.size(), transformations.size());
        return new LoadedRegistry(Map.copyOf(byId), Map.copyOf(byPair), Map.copyOf(transformations));
    }

    private static void loadFusionFiles(ResourceManager resourceManager, Map<String, FusionDefinition> byId, Map<FusionKey, FusionDefinition> byPair) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources("infinitefusion/fusions", location -> location.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                FusionDefinition definition = FusionDefinition.fromJson(json, entry.getKey().toString(), LOGGER);
                if (definition == null || !definition.enabled()) {
                    continue;
                }
                if (byId.containsKey(definition.id())) {
                    LOGGER.warn("[infinitefusion] Disabled fusion {}: duplicate id", definition.id());
                    continue;
                }
                FusionKey key = FusionKey.of(definition.donor().speciesId(), definition.host().speciesId());
                if (byPair.containsKey(key)) {
                    LOGGER.warn("[infinitefusion] Disabled fusion {}: duplicate donor/host pair {} -> {}", definition.id(), definition.donor().speciesId(), definition.host().speciesId());
                    continue;
                }
                byId.put(definition.id(), definition);
                byPair.put(key, definition);
                LOGGER.info("[infinitefusion] Loaded fusion {} -> {}/{}", definition.id(), definition.result().speciesId(), definition.result().form());
            } catch (Exception exception) {
                LOGGER.warn("[infinitefusion] Failed to load fusion resource {}", entry.getKey(), exception);
            }
        }
    }

    private static void loadTransformationFiles(ResourceManager resourceManager, Map<String, FusionDefinition> byId, Map<String, TransformationDefinition> transformations) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources("infinitefusion/transformations", location -> location.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                TransformationDefinition definition = TransformationDefinition.fromJson(json, entry.getKey().toString(), LOGGER);
                if (definition == null || !definition.enabled()) {
                    continue;
                }
                if (!byId.containsKey(definition.baseFusionId())) {
                    LOGGER.warn("[infinitefusion] Disabled transformation {}: missing base_fusion {}", definition.id(), definition.baseFusionId());
                    continue;
                }
                if (transformations.containsKey(definition.id())) {
                    LOGGER.warn("[infinitefusion] Disabled transformation {}: duplicate id", definition.id());
                    continue;
                }
                transformations.put(definition.id(), definition);
            } catch (Exception exception) {
                LOGGER.warn("[infinitefusion] Failed to load transformation resource {}", entry.getKey(), exception);
            }
        }
    }

    public record LoadedRegistry(Map<String, FusionDefinition> byId,
                                 Map<FusionKey, FusionDefinition> byPair,
                                 Map<String, TransformationDefinition> transformations) {
        public Optional<FusionDefinition> findFusion(Pokemon donor, Pokemon host) {
            FusionDefinition direct = byPair.get(FusionKey.of(
                    donor.getSpecies().getResourceIdentifier().toString(),
                    host.getSpecies().getResourceIdentifier().toString()
            ));
            if (direct != null && direct.matches(donor, host)) {
                return Optional.of(direct);
            }

            for (FusionDefinition definition : byId.values()) {
                if (definition.matches(donor, host)) {
                    return Optional.of(definition);
                }
            }
            return Optional.empty();
        }

        public List<TransformationDefinition> transformationsFor(FusionDefinition definition) {
            List<TransformationDefinition> matches = new ArrayList<>();
            for (String id : definition.transformations()) {
                TransformationDefinition transformation = transformations.get(id);
                if (transformation != null) {
                    matches.add(transformation);
                }
            }
            return matches;
        }
    }

    public record FusionKey(String donorSpecies, String hostSpecies) {
        static FusionKey of(String donorSpecies, String hostSpecies) {
            return new FusionKey(donorSpecies, hostSpecies);
        }
    }
}
