# Paraglider — Unofficial Fabric 26.2 Port

Unofficial Fabric port of Tictim's [Paraglider](https://github.com/Tictim/Paraglider) **26.2.1**.

| | |
|---|---|
| Minecraft | 26.2 |
| Loader | Fabric 0.19.5+ |
| Fabric API | 0.161.0+26.2 |
| Forge Config API Port | 26.2.1 |
| Java | 25 |
| Version | 26.2.1+fabric |
| License | GPL-3.0-only |

This is an unofficial port and is not endorsed by the upstream author. Port-specific
issues belong here, not in the upstream repository.

## Download

Ready-to-install JAR: [GitHub Releases](https://github.com/Dorukium/Paragliders-fabric-unofficial/releases/latest)
(`Paraglider-fabric-26.2-26.2.1+fabric.jar`). Sources JAR is also attached there.

## Dependencies

- [Fabric API](https://modrinth.com/mod/fabric-api) 0.161.0+26.2
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) 26.2.1

Trinkets and Cardinal Components are **not** required: upstream 26.2.1 no longer uses
Curios, and entity attachments use Fabric's Data Attachment API.

## Building

Requires JDK 25.

```sh
./gradlew build
```

The JAR is written to `build/libs/`.

> Minecraft 26.2 is unobfuscated and ships no Mojang mappings, so the build uses Loom's
> no-remap mode (`fabric.loom.disableObfuscation=true`) and plain `implementation`
> dependencies instead of `modImplementation`.

## Documentation

- `PORT_PLAN.md` — porting plan and phases
- `PORT_REPORT.md` — conversions, dependency list, test results, known differences

## Credits

Paraglider is created by **Tictim** and licensed GPL-3.0. `TEMPLATE_LICENSE.txt` keeps the
MIT notice covering parts originating in the NeoForged MDK template.
