package com.magpie.infinitefusion;

import com.google.gson.JsonObject;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public record TransformationDefinition(
        String id,
        boolean enabled,
        String baseFusionId,
        FusionDefinition.ResultSpec result,
        IntegrationType integrationType,
        boolean integrationRequired,
        String triggerMode,
        String triggerHandler,
        FusionDefinition.ValueMode typingMode,
        List<String> typingValues,
        FusionDefinition.ValueMode statsMode,
        FusionDefinition.ValueMode abilitiesMode,
        FusionDefinition.ValueMode movesMode
) {
    public static TransformationDefinition fromJson(JsonObject json, String resourceId, Logger logger) {
        String id = FusionDefinition.stringOrNull(json, "id");
        if (id == null || id.isBlank()) {
            logger.warn("[infinitefusion] Disabled transformation {}: missing id", resourceId);
            return null;
        }

        String baseFusionId = FusionDefinition.stringOrNull(json, "base_fusion");
        JsonObject resultObject = FusionDefinition.objectOrNull(json, "result");
        JsonObject integrationObject = FusionDefinition.objectOrNull(json, "integration");
        if (baseFusionId == null || baseFusionId.isBlank() || resultObject == null || integrationObject == null) {
            logger.warn("[infinitefusion] Disabled transformation {}: base_fusion, result, and integration are required", id);
            return null;
        }

        FusionDefinition.ResultSpec result = FusionDefinition.ResultSpec.fromJson(resultObject, id, logger);
        if (result == null) {
            return null;
        }

        String rawIntegration = FusionDefinition.stringOrNull(integrationObject, "type");
        IntegrationType integrationType = IntegrationType.parse(rawIntegration);
        if (integrationType == null) {
            logger.warn("[infinitefusion] Disabled transformation {}: unknown integration.type {}", id, rawIntegration);
            return null;
        }

        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
        boolean integrationRequired = integrationObject.has("required") && integrationObject.get("required").getAsBoolean();

        JsonObject triggerObject = FusionDefinition.objectOrNull(json, "trigger");
        String triggerMode = triggerObject == null ? null : FusionDefinition.stringOrNull(triggerObject, "mode");
        String triggerHandler = triggerObject == null ? null : FusionDefinition.stringOrNull(triggerObject, "handler");

        JsonObject typingObject = FusionDefinition.objectOrNull(json, "typing");
        FusionDefinition.ValueMode typingMode = FusionDefinition.ValueMode.parse(typingObject == null ? null : FusionDefinition.stringOrNull(typingObject, "mode"), FusionDefinition.ValueMode.HOST, id, "typing", logger);
        List<String> typingValues = typingObject == null ? List.of() : Collections.unmodifiableList(FusionDefinition.parseStringArray(typingObject.get("values")));

        JsonObject statsObject = FusionDefinition.objectOrNull(json, "stats");
        FusionDefinition.ValueMode statsMode = FusionDefinition.ValueMode.parse(statsObject == null ? null : FusionDefinition.stringOrNull(statsObject, "mode"), FusionDefinition.ValueMode.HOST, id, "stats", logger);
        JsonObject abilitiesObject = FusionDefinition.objectOrNull(json, "abilities");
        FusionDefinition.ValueMode abilitiesMode = FusionDefinition.ValueMode.parse(abilitiesObject == null ? null : FusionDefinition.stringOrNull(abilitiesObject, "mode"), FusionDefinition.ValueMode.HOST, id, "abilities", logger);
        JsonObject movesObject = FusionDefinition.objectOrNull(json, "moves");
        FusionDefinition.ValueMode movesMode = FusionDefinition.ValueMode.parse(movesObject == null ? null : FusionDefinition.stringOrNull(movesObject, "mode"), FusionDefinition.ValueMode.HOST, id, "moves", logger);

        if (integrationType == IntegrationType.MEGA_SHOWDOWN && !ModList.get().isLoaded("mega_showdown")) {
            logger.warn("[infinitefusion] Transformation {}: Mega Showdown not present, skipping optional integration", id);
            if (integrationRequired) {
                return null;
            }
        }

        return new TransformationDefinition(
                id,
                enabled,
                baseFusionId,
                result,
                integrationType,
                integrationRequired,
                triggerMode,
                triggerHandler,
                typingMode,
                typingValues,
                statsMode,
                abilitiesMode,
                movesMode
        );
    }

    public enum IntegrationType {
        MEGA_SHOWDOWN,
        INTERNAL;

        public static IntegrationType parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            try {
                return IntegrationType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
    }
}
