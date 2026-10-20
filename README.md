# RaindanceMC/economy

- **`economy-module`** — Rain's Flexible Modules — Economy: A server's money: accounts and a bank, paying and billing, coins and banknotes you can carry, a shop sorted like the creative inventory with prices worked out from the server's own recipes, and ways to earn — every part switchable on its own. Provides RainsCore's economy, so every other plugin (and every Vault plugin) charges through it.
- **`economy-standalone`** — Rain's Economy: Rain's Economy as a plugin of its own: the economy module and the standard wrapper.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Split out of `rains-flexible-modules` on 16.10.2026, history kept.

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `economy-standalone/target/RainsEconomy-0.52.1.jar` — also published as `de.raindancer.modules:economy-standalone:0.52.1`

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.
