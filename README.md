# Infinite Fusion

A NeoForge 1.21.1 add-on mod for Cobblemon 1.7.3 that adds item-driven, data-driven Pokemon fusion.

This mod provides the gameplay engine. Content lives in datapacks and resource packs.

The mod handles:
- Splicer and Desplicer items
- donor storage and return
- ordered fusion matching (`donor -> host`)
- unfusing
- validation and logging for fusion definitions
- Creative/Survival-safe item syncing

The content pack handles:
- fusion definitions
- Cobblemon forms and species additions
- models, textures, animations, posers, resolvers, lang, particles, and sounds

## Current scope

This mod is intentionally focused on fusion and unfusion.

It does **not** currently implement a generic mega-evolution system for fused forms. You can still define transformation JSON for future use or external integrations, but fusion megas are outside the main scope of this release.

## Version targets

- Minecraft: `1.21.1`
- NeoForge: `21.1.x`
- Cobblemon: `1.7.3`

## How fusion works in-game

The mod adds two items:

- `infinitefusion:splicer`
- `infinitefusion:desplicer`

### Splicer

1. Right-click one of your own Pokemon with an empty Splicer.
2. That Pokemon becomes the **donor** and is stored inside the item.
3. Right-click one of your own eligible Pokemon with the filled Splicer.
4. If a matching fusion exists for that exact `donor -> host` order, the host changes into the fused result.
5. If no matching fusion exists, the donor is returned to your party and you get the message:
   `Huh, guess that one doesn't work...`
6. If your party is full during donor return, the donor stays in the item and you get:
   `Please make room to return the donor Pokemon to your party`

### Desplicer

1. Right-click a fused Pokemon with an empty Desplicer.
2. The fused host is restored to its original state.
3. The donor is extracted and stored inside the Desplicer.
4. Right-click with a filled Desplicer to return the stored donor to your party.
5. If your party is full, the donor stays in the Desplicer and you get the same "make room" message.

## Ordered fusion rules

Fusions are **ordered**.

That means this:

- `Sylveon -> Gardevoir = Sylvevoir`

is not the same as this:

- `Gardevoir -> Sylveon`

If you want both directions to work, define two separate fusion entries.

## Data-driven architecture

This mod is built so that pack makers can add their own fusions without editing Java.

The split is:

### This mod owns
- fusion item mechanics
- donor storage
- registry loading
- validation
- fusion application and extraction

### Cobblemon owns
- species data
- forms
- abilities
- base stats
- movesets
- species additions
- species features

### Resource packs own
- models
- animations
- textures
- posers
- resolvers
- language entries
- particles
- sounds

## Pack structure

### Datapack layout

```text
<data pack root>/
├── pack.mcmeta
└── data/
    ├── infinitefusion/
    │   ├── infinitefusion/
    │   │   ├── fusions/
    │   │   │   └── your_fusion.json
    │   │   └── transformations/
    │   │       └── your_transformation.json
    │   └── recipe/
    │       ├── splicer.json
    │       └── desplicer.json
    └── cobblemon/
        ├── species_additions/
        ├── species_features/
        ├── species/
        └── dex_entries/
```

### Resource pack layout

```text
<resource pack root>/
├── pack.mcmeta
└── assets/
    ├── cobblemon/
    │   ├── bedrock/
    │   │   └── pokemon/
    │   │       ├── animations/
    │   │       ├── models/
    │   │       ├── posers/
    │   │       └── resolvers/
    │   ├── textures/
    │   │   ├── pokemon/
    │   │   └── particle/
    │   ├── particles/
    │   ├── sounds/
    │   ├── sounds.json
    │   └── lang/
    └── infinitefusion/
        ├── items/
        ├── models/item/
        ├── textures/item/
        └── lang/
```

## Fusion JSON format

Fusion definitions are loaded from:

```text
data/<namespace>/infinitefusion/fusions/*.json
```

The bundled content uses:

```text
data/infinitefusion/infinitefusion/fusions/*.json
```

### Minimal fusion example

```json
{
  "id": "sylveon_to_gardevoir",
  "donor": {
    "species": "cobblemon:sylveon"
  },
  "host": {
    "species": "cobblemon:gardevoir"
  },
  "result": {
    "species": "cobblemon:gardevoir",
    "form": "sylvevoir",
    "aspects": ["sylvevoir"]
  },
  "typing": { "mode": "form" },
  "stats": { "mode": "form" },
  "abilities": { "mode": "form" },
  "moves": { "mode": "form" }
}
```

## Fusion schema

### Required fields

```json
{
  "id": "unique_fusion_id",
  "donor": {
    "species": "namespace:species"
  },
  "host": {
    "species": "namespace:species"
  },
  "result": {
    "species": "namespace:species",
    "form": "form_name"
  }
}
```

### Optional fields

```json
{
  "enabled": true,
  "donor": {
    "form": "specific_form",
    "aspects": ["aspect_a", "aspect_b"]
  },
  "host": {
    "form": "specific_form",
    "aspects": ["aspect_a", "aspect_b"]
  },
  "result": {
    "aspects": ["aspect_a", "aspect_b"]
  },
  "typing": {
    "mode": "host|form|override",
    "values": ["cobblemon:dragon", "cobblemon:fairy"]
  },
  "stats": {
    "mode": "host|form|override",
    "values": {
      "hp": 100,
      "attack": 100,
      "defence": 100,
      "special_attack": 100,
      "special_defence": 100,
      "speed": 100
    }
  },
  "abilities": {
    "mode": "host|form|override",
    "values": ["cobblemon:trace", "cobblemon:pixilate"]
  },
  "moves": {
    "mode": "host|form|override|merge",
    "levels": {
      "1": ["cobblemon:confusion", "cobblemon:disarmingvoice"],
      "42": ["cobblemon:moonblast"]
    }
  },
  "transformations": ["some_transformation_id"],
  "flags": {
    "allow_unfuse": true,
    "preserve_donor_data": true
  },
  "display": {
    "name_key": "fusion.example.name"
  }
}
```

## Mode behavior

### `host`
Use the host Pokemon's values.

### `form`
Use the values authored on the Cobblemon result form.

### `override`
Use the values explicitly listed in the fusion JSON.

### `merge`
Available for moves only.

This starts with the host learnset and adds the listed moves on top.

## Recommended content workflow

For most pack makers, the safest setup is:

- `typing.mode = form`
- `stats.mode = form`
- `abilities.mode = form`
- `moves.mode = form`

That means your fusion JSON only matches donor + host and points to the result form, while all battle data lives in Cobblemon's standard form definition.

This avoids duplication and keeps your pack easier to maintain.

## Match rules

A fusion entry can match by:

- donor species
- optional donor form
- optional donor aspects
- host species
- optional host form
- optional host aspects

If a required form or aspect does not match, that fusion does not apply.

## Result requirements

The `result.species` and `result.form` must exist in loaded Cobblemon data.

If the form does not exist, the fusion entry is disabled and the mod logs a warning.

In plain English:

The fusion JSON tells the mod what to do.
The Cobblemon species addition tells Cobblemon what the fused Pokemon actually is.

You need both.

## Cobblemon form setup

A fusion result should usually be implemented as a Cobblemon species addition on the host species.

For example, bundled Gardevoir fusions live in:

```text
data/cobblemon/species_additions/generation3/gardevoir_fusion.json
```

That file adds forms like:
- `sylvevoir`
- `umbrevoir`
- `espevoir`
- `flarevoir`
- `vaporevoir`
- `glacevoir`
- `leafevoir`
- `joltevoir`

A form typically defines:
- name
- types
- labels
- aspects
- abilities
- base stats
- moves
- pokedex entries
- hitbox or visuals if needed

## Species features

Species features are used to give forms their aspect hooks.

Bundled examples live in:

```text
data/cobblemon/species_features/
```

Examples:
- `sylvevoir.json`
- `aegievoir.json`
- `darkzor.json`
- `rayevoir.json`

If your form depends on a specific aspect, define that feature and make sure your form and resolver agree on the same aspect name.

## Resource authoring

To make a fusion visible in-game, you also need the normal Cobblemon rendering stack:

- model (`.geo.json`)
- animation (`.animation.json`)
- poser (`.json`)
- resolver (`.json`)
- texture(s)
- language entries

Bundled examples include:

```text
assets/cobblemon/bedrock/pokemon/models/0282_gardevoir/
assets/cobblemon/bedrock/pokemon/animations/0282_gardevoir/
assets/cobblemon/bedrock/pokemon/posers/0282_gardevoir/
assets/cobblemon/bedrock/pokemon/resolvers/0282_gardevoir/
assets/cobblemon/textures/pokemon/
```

Resolvers are especially important. They connect loaded species/forms/aspects to the correct model and texture assets.

## Transformations

Transformation definitions are loaded from:

```text
data/<namespace>/infinitefusion/transformations/*.json
```

Bundled example:

```json
{
  "id": "rayevoir_mega",
  "base_fusion": "gardevoir_to_rayquaza",
  "result": {
    "species": "cobblemon:rayquaza",
    "form": "Rayevoir-Mega",
    "aspects": ["rayevoir", "mega"]
  },
  "integration": {
    "type": "mega_showdown",
    "required": false
  },
  "trigger": {
    "mode": "external",
    "handler": "mega_showdown"
  },
  "typing": { "mode": "form" },
  "stats": { "mode": "form" },
  "abilities": { "mode": "form" },
  "moves": { "mode": "form" }
}
```

### Important note

Transformation entries are data-only in this release.

This mod does not ship a generic mega trigger for fused forms. If you want transformation behavior, that should be handled by a separate integration or a future extension.

## Recipes

Recipes live in:

```text
data/<namespace>/recipe/
```

For this mod:

```text
data/infinitefusion/recipe/
```

### Important 1.21.1 recipe note

On this project, shaped recipe keys should use JSON ingredient objects, not raw strings.

Example:

```json
{
  "key": {
    "I": { "item": "minecraft:iron_ingot" },
    "D": { "item": "minecraft:diamond" }
  }
}
```

### Example Splicer recipe

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": [
    "IDI",
    "RER",
    "IAN"
  ],
  "key": {
    "I": { "item": "minecraft:iron_ingot" },
    "D": { "item": "minecraft:diamond" },
    "R": { "item": "minecraft:redstone" },
    "E": { "item": "minecraft:end_rod" },
    "A": { "item": "minecraft:amethyst_shard" },
    "N": { "item": "minecraft:netherite_ingot" }
  },
  "result": {
    "id": "infinitefusion:splicer",
    "count": 1
  }
}
```

### Example Desplicer recipe

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": [
    "GNG",
    "RSR",
    "GAD"
  ],
  "key": {
    "G": { "item": "minecraft:gold_ingot" },
    "N": { "item": "minecraft:netherite_ingot" },
    "R": { "item": "minecraft:redstone" },
    "S": { "item": "minecraft:shears" },
    "A": { "item": "minecraft:amethyst_shard" },
    "D": { "item": "minecraft:diamond" }
  },
  "result": {
    "id": "infinitefusion:desplicer",
    "count": 1
  }
}
```

## Validation behavior

The loader distinguishes between hard failures and soft failures.

### Hard failures disable the fusion
- missing `id`
- missing donor species
- missing host species
- missing result species
- missing result form
- unknown species
- unknown result form
- malformed required JSON structure
- duplicate fusion IDs

### Soft failures fall back safely
- invalid type override
- invalid ability override
- malformed move override block
- invalid move IDs
- missing optional transformation integration

Soft-failure defaults:
- typing -> `host`
- stats -> `host`
- abilities -> `host`
- moves -> `host`

## Logging

The mod logs fusion loading and validation results.

Examples of expected log messages:

```text
[infinitefusion] Loaded fusion sylveon_to_gardevoir -> cobblemon:gardevoir/sylvevoir
[infinitefusion] Disabled fusion foo: unknown result form 'bar' for species cobblemon:gardevoir
[infinitefusion] Fusion test_fusion: unknown move cobblemon:not_a_real_move at level 42, ignoring move
```

If your pack does not work, the log should always be your first stop.

## Bundled reference content

This project currently ships the following built-in fusions:

- Sylveon -> Gardevoir = Sylvevoir
- Umbreon -> Gardevoir = Umbrevoir
- Espeon -> Gardevoir = Espevoir
- Flareon -> Gardevoir = Flarevoir
- Vaporeon -> Gardevoir = Vaporevoir
- Glaceon -> Gardevoir = Glacevoir
- Leafeon -> Gardevoir = Leafevoir
- Jolteon -> Gardevoir = Joltevoir
- Gardevoir -> Aegislash = Aegievoir
- Gardevoir -> Arceus = Arcevoir
- Gardevoir -> Rayquaza = Rayevoir
- Darkrai -> Scizor = Darkzor

Additional non-fusion content:
- Female Kirlia + King's Rock -> Queen

## Adding your own fusion: quick checklist

1. Create a fusion JSON in `data/<namespace>/infinitefusion/fusions/`
2. Create or extend the target Cobblemon species addition with the result form
3. Add any needed species feature JSON
4. Add model, animation, poser, resolver, and texture assets in the resource pack
5. Add lang entries for the new form
6. Start the game and check the log for validation messages
7. Test the exact donor -> host order in-game

## Known limitations

- No built-in generic mega handling for fused forms
- Transformations are data-defined, but activation is left to integrations or future work
- Fusion is item-based and intentionally one-way by default

## Tips for pack makers

- Keep the fusion JSON small and let Cobblemon forms do the heavy lifting
- Use `mode: form` wherever possible
- Treat resolvers as mandatory, not optional
- Keep donor and host ordering explicit
- Check logs before assuming the mod ignored your files
- If a recipe fails to load, double-check the folder name and ingredient JSON format first

## License / usage

Use, modify, and extend your own content packs however you like, but make sure you have permission to redistribute any models, textures, or animations you include.

If you fork this project for your own fusion pack, please keep the data-driven structure intact. It makes everybody's life a little less awful.
