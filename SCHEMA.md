# Infinite Fusion Data Schema v1

## Fusion files

Location:

`data/<namespace>/infinitefusion/fusions/*.json`

Required fields:

- `id`
- `donor.species`
- `host.species`
- `result.species`
- `result.form`

Optional fields:

- `enabled`
- `donor.form`
- `donor.aspects`
- `host.form`
- `host.aspects`
- `result.aspects`
- `typing`
- `stats`
- `abilities`
- `moves`
- `transformations`
- `flags`
- `display`

Modes:

- `typing.mode`: `host`, `form`, `override`
- `stats.mode`: `host`, `form`, `override`
- `abilities.mode`: `host`, `form`, `override`
- `moves.mode`: `host`, `form`, `override`, `merge`

Bad optional sections log a warning and fall back to `host`.
Bad identity fields disable the entry.

## Transformation files

Location:

`data/<namespace>/infinitefusion/transformations/*.json`

Required fields:

- `id`
- `base_fusion`
- `result.species`
- `result.form`
- `integration.type`

Supported `integration.type` values:

- `mega_showdown`
- `internal`

If Mega Showdown is absent and the transformation is optional, the base fusion still loads and the transformation is skipped.
