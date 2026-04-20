package com.magpie.infinitefusion;

import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record FusionDefinition(
        String id,
        boolean enabled,
        MatchSpec donor,
        MatchSpec host,
        ResultSpec result,
        ValueMode typingMode,
        List<String> typingValues,
        ValueMode statsMode,
        Map<String, Integer> statValues,
        ValueMode abilitiesMode,
        List<String> abilityValues,
        ValueMode movesMode,
        Map<Integer, List<String>> moveLevels,
        List<String> transformations,
        boolean allowUnfuse,
        boolean preserveDonorData,
        String displayNameKey
) {
    public boolean matches(Pokemon donorPokemon, Pokemon hostPokemon) {
        return donor.matches(donorPokemon) && host.matches(hostPokemon);
    }

    public String resultAspect() {
        if (!result.aspects().isEmpty()) {
            return result.aspects().getFirst();
        }
        return normalizeAspect(result.form());
    }

    public static FusionDefinition fromJson(JsonObject json, String resourceId, Logger logger) {
        String id = stringOrNull(json, "id");
        if (isBlank(id)) {
            logger.warn("[infinitefusion] Disabled fusion {}: missing id", resourceId);
            return null;
        }

        JsonObject donorObject = objectOrNull(json, "donor");
        JsonObject hostObject = objectOrNull(json, "host");
        JsonObject resultObject = objectOrNull(json, "result");
        if (donorObject == null || hostObject == null || resultObject == null) {
            logger.warn("[infinitefusion] Disabled fusion {}: donor, host, and result objects are required", id);
            return null;
        }

        MatchSpec donor = MatchSpec.fromJson(donorObject, id, "donor", logger);
        MatchSpec host = MatchSpec.fromJson(hostObject, id, "host", logger);
        ResultSpec result = ResultSpec.fromJson(resultObject, id, logger);
        if (donor == null || host == null || result == null) {
            return null;
        }

        if (!validateSpecies(donor.speciesId(), id, "donor", logger)) {
            return null;
        }
        if (!validateSpecies(host.speciesId(), id, "host", logger)) {
            return null;
        }
        Species resultSpecies = resolveSpecies(result.speciesId());
        if (resultSpecies == null) {
            logger.warn("[infinitefusion] Disabled fusion {}: unknown result species {}", id, result.speciesId());
            return null;
        }
        if (resultSpecies.getFormByName(result.form()) == null) {
            logger.warn("[infinitefusion] Disabled fusion {}: unknown result form '{}' for species {}", id, result.form(), result.speciesId());
            return null;
        }

        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();

        RuleValidation typing = parseTypeRule(id, objectOrNull(json, "typing"), logger);
        RuleValidation stats = parseStatsRule(id, objectOrNull(json, "stats"), logger);
        RuleValidation abilities = parseAbilityRule(id, objectOrNull(json, "abilities"), logger);
        MoveRuleValidation moves = parseMoveRule(id, objectOrNull(json, "moves"), logger);

        List<String> transformations = parseStringArray(json.get("transformations"));
        JsonObject flags = objectOrNull(json, "flags");
        boolean allowUnfuse = flags == null || !flags.has("allow_unfuse") || flags.get("allow_unfuse").getAsBoolean();
        boolean preserveDonorData = flags == null || !flags.has("preserve_donor_data") || flags.get("preserve_donor_data").getAsBoolean();

        JsonObject display = objectOrNull(json, "display");
        String displayNameKey = display == null ? null : stringOrNull(display, "name_key");

        return new FusionDefinition(
                id,
                enabled,
                donor,
                host,
                result,
                typing.mode(), typing.values(),
                stats.mode(), stats.statValues(),
                abilities.mode(), abilities.values(),
                moves.mode(), moves.levels(),
                transformations,
                allowUnfuse,
                preserveDonorData,
                displayNameKey
        );
    }

    private static RuleValidation parseTypeRule(String fusionId, JsonObject object, Logger logger) {
        if (object == null) {
            return RuleValidation.host();
        }
        ValueMode mode = ValueMode.parse(stringOrNull(object, "mode"), ValueMode.HOST, fusionId, "typing", logger);
        if (mode != ValueMode.OVERRIDE) {
            return new RuleValidation(mode, List.of(), Map.of());
        }

        List<String> values = new ArrayList<>();
        boolean valid = true;
        for (String raw : parseStringArray(object.get("values"))) {
            String normalized = normalizeRegistryName(raw);
            if (ElementalTypes.get(normalized) == null) {
                logger.warn("[infinitefusion] Fusion {}: unknown type {}, falling back typing to host", fusionId, raw);
                valid = false;
            } else {
                values.add(normalized);
            }
        }
        if (!valid || values.isEmpty()) {
            return RuleValidation.host();
        }
        return new RuleValidation(mode, values, Map.of());
    }

    private static RuleValidation parseStatsRule(String fusionId, JsonObject object, Logger logger) {
        if (object == null) {
            return RuleValidation.host();
        }
        ValueMode mode = ValueMode.parse(stringOrNull(object, "mode"), ValueMode.HOST, fusionId, "stats", logger);
        if (mode != ValueMode.OVERRIDE) {
            return new RuleValidation(mode, List.of(), Map.of());
        }

        JsonObject values = objectOrNull(object, "values");
        if (values == null) {
            logger.warn("[infinitefusion] Fusion {}: invalid stats override, falling back to host", fusionId);
            return RuleValidation.host();
        }

        Map<String, Integer> stats = new LinkedHashMap<>();
        List<String> required = List.of("hp", "attack", "defence", "special_attack", "special_defence", "speed");
        for (String key : required) {
            if (!values.has(key) || !values.get(key).isJsonPrimitive()) {
                logger.warn("[infinitefusion] Fusion {}: stats override missing {}, falling back to host", fusionId, key);
                return RuleValidation.host();
            }
            stats.put(key, values.get(key).getAsInt());
        }
        return new RuleValidation(mode, List.of(), stats);
    }

    private static RuleValidation parseAbilityRule(String fusionId, JsonObject object, Logger logger) {
        if (object == null) {
            return RuleValidation.host();
        }
        ValueMode mode = ValueMode.parse(stringOrNull(object, "mode"), ValueMode.HOST, fusionId, "abilities", logger);
        if (mode != ValueMode.OVERRIDE) {
            return new RuleValidation(mode, List.of(), Map.of());
        }

        List<String> values = new ArrayList<>();
        boolean valid = true;
        for (String raw : parseStringArray(object.get("values"))) {
            String normalized = normalizeRegistryName(raw);
            if (Abilities.get(normalized) == null) {
                logger.warn("[infinitefusion] Fusion {}: unknown ability {}, falling back abilities to host", fusionId, raw);
                valid = false;
            } else {
                values.add(normalized);
            }
        }
        if (!valid || values.isEmpty()) {
            return RuleValidation.host();
        }
        return new RuleValidation(mode, values, Map.of());
    }

    private static MoveRuleValidation parseMoveRule(String fusionId, JsonObject object, Logger logger) {
        if (object == null) {
            return MoveRuleValidation.host();
        }
        ValueMode mode = ValueMode.parse(stringOrNull(object, "mode"), ValueMode.HOST, fusionId, "moves", logger);
        if (mode == ValueMode.HOST || mode == ValueMode.FORM) {
            return new MoveRuleValidation(mode, Map.of());
        }
        JsonObject levelsObject = objectOrNull(object, "levels");
        if (levelsObject == null) {
            logger.warn("[infinitefusion] Fusion {}: malformed moves section, falling back to host", fusionId);
            return MoveRuleValidation.host();
        }

        Map<Integer, List<String>> levels = new LinkedHashMap<>();
        boolean sawValidMove = false;
        for (Map.Entry<String, JsonElement> entry : levelsObject.entrySet()) {
            int level;
            try {
                level = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException exception) {
                logger.warn("[infinitefusion] Fusion {}: invalid move level {}, ignoring entry", fusionId, entry.getKey());
                continue;
            }
            List<String> moveNames = new ArrayList<>();
            for (String rawMove : parseStringArray(entry.getValue())) {
                String normalized = normalizeRegistryName(rawMove);
                if (Moves.getByName(normalized) == null) {
                    logger.warn("[infinitefusion] Fusion {}: unknown move {} at level {}, ignoring move", fusionId, rawMove, level);
                    continue;
                }
                sawValidMove = true;
                moveNames.add(normalized);
            }
            if (!moveNames.isEmpty()) {
                levels.put(level, moveNames);
            }
        }

        if (!sawValidMove && (mode == ValueMode.OVERRIDE || mode == ValueMode.MERGE)) {
            logger.warn("[infinitefusion] Fusion {}: no valid moves remained after validation, falling back to host", fusionId);
            return MoveRuleValidation.host();
        }
        return new MoveRuleValidation(mode, levels);
    }

    private static boolean validateSpecies(String speciesId, String fusionId, String side, Logger logger) {
        if (resolveSpecies(speciesId) == null) {
            logger.warn("[infinitefusion] Disabled fusion {}: unknown {} species {}", fusionId, side, speciesId);
            return false;
        }
        return true;
    }

    static Species resolveSpecies(String speciesId) {
        try {
            return PokemonSpecies.getByIdentifier(ResourceLocation.parse(speciesId));
        } catch (Exception exception) {
            return null;
        }
    }

    static String normalizeAspect(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT);
    }

    static String normalizeRegistryName(String input) {
        if (input == null) {
            return "";
        }
        int colonIndex = input.indexOf(':');
        String normalized = colonIndex >= 0 ? input.substring(colonIndex + 1) : input;
        return normalized.toLowerCase(Locale.ROOT);
    }

    static JsonObject objectOrNull(JsonObject root, String member) {
        if (root == null || !root.has(member) || !root.get(member).isJsonObject()) {
            return null;
        }
        return root.getAsJsonObject(member);
    }

    static String stringOrNull(JsonObject root, String member) {
        if (root == null || !root.has(member) || !root.get(member).isJsonPrimitive()) {
            return null;
        }
        try {
            return root.get(member).getAsString();
        } catch (Exception exception) {
            return null;
        }
    }

    static List<String> parseStringArray(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (element.isJsonPrimitive()) {
            return List.of(element.getAsString());
        }
        if (!element.isJsonArray()) {
            return List.of();
        }
        JsonArray jsonArray = element.getAsJsonArray();
        List<String> values = new ArrayList<>(jsonArray.size());
        for (JsonElement child : jsonArray) {
            if (child != null && child.isJsonPrimitive()) {
                values.add(child.getAsString());
            }
        }
        return values;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record MatchSpec(String speciesId, String form, Set<String> aspects) {
        static MatchSpec fromJson(JsonObject json, String fusionId, String side, Logger logger) {
            String speciesId = stringOrNull(json, "species");
            if (isBlank(speciesId)) {
                logger.warn("[infinitefusion] Disabled fusion {}: {}.species is required", fusionId, side);
                return null;
            }

            String form = stringOrNull(json, "form");
            Set<String> aspects = new LinkedHashSet<>();
            for (String aspect : parseStringArray(json.get("aspects"))) {
                aspects.add(normalizeAspect(aspect));
            }
            return new MatchSpec(speciesId, form, Collections.unmodifiableSet(aspects));
        }

        public boolean matches(Pokemon pokemon) {
            if (pokemon == null || pokemon.getSpecies() == null) {
                return false;
            }
            if (!Objects.equals(pokemon.getSpecies().getResourceIdentifier().toString(), speciesId)) {
                return false;
            }
            if (!isBlank(form) && !form.equalsIgnoreCase(pokemon.getForm().getName())) {
                return false;
            }
            if (aspects.isEmpty()) {
                return true;
            }
            Set<String> currentAspects = new HashSet<>();
            for (String aspect : pokemon.getAspects()) {
                currentAspects.add(normalizeAspect(aspect));
            }
            return currentAspects.containsAll(aspects);
        }
    }

    public record ResultSpec(String speciesId, String form, List<String> aspects) {
        static ResultSpec fromJson(JsonObject json, String fusionId, Logger logger) {
            String speciesId = stringOrNull(json, "species");
            String form = stringOrNull(json, "form");
            if (isBlank(speciesId) || isBlank(form)) {
                logger.warn("[infinitefusion] Disabled fusion {}: result.species and result.form are required", fusionId);
                return null;
            }
            List<String> aspects = new ArrayList<>();
            for (String aspect : parseStringArray(json.get("aspects"))) {
                aspects.add(normalizeAspect(aspect));
            }
            return new ResultSpec(speciesId, form, Collections.unmodifiableList(aspects));
        }
    }

    public enum ValueMode {
        HOST,
        FORM,
        OVERRIDE,
        MERGE;

        static ValueMode parse(String raw, ValueMode fallback, String fusionId, String section, Logger logger) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return ValueMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                logger.warn("[infinitefusion] Fusion {}: invalid {} mode '{}', falling back to {}", fusionId, section, raw, fallback.name().toLowerCase(Locale.ROOT));
                return fallback;
            }
        }
    }

    private record RuleValidation(ValueMode mode, List<String> values, Map<String, Integer> statValues) {
        static RuleValidation host() {
            return new RuleValidation(ValueMode.HOST, List.of(), Map.of());
        }
    }

    private record MoveRuleValidation(ValueMode mode, Map<Integer, List<String>> levels) {
        static MoveRuleValidation host() {
            return new MoveRuleValidation(ValueMode.HOST, Map.of());
        }
    }
}
