# Paraglider 26.2.1 → Fabric 26.2 — Port Raporu

## Teslim edilenler

- Gradle projesi: bu dizinin kökü (`build.gradle`, `gradle.properties`, `settings.gradle`, wrapper).
- Derlenen JAR: `build/libs/Paraglider-fabric-26.2-26.2.1+fabric.jar` (~657 KB)
- Kaynak JAR: `build/libs/Paraglider-fabric-26.2-26.2.1+fabric-sources.jar`
- `src/main/resources/fabric.mod.json`, `paraglider.accesswidener`, mixin config'leri.
- Tüm upstream asset/data dosyaları (item model'leri, blok modelleri, 17 yapı NBT'si, tarifler,
  advancements, tag'ler, worldgen JSON'ları, shaders, 6 dil dosyası).

## Sürümler (doğrulandı)

| Bileşen | Sürüm |
|---|---|
| Minecraft | 26.2 (obfuscate edilmemiş, mappings yok) |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.2 |
| Forge Config API Port | 26.2.1 |
| Loom | 1.17.21 |
| Gradle | 9.9.2 |
| Java | 25 |

> MC 26.2 artık Mojang mappings yayınlamadığı için Loom **no-remap** modunda
> (`fabric.loom.disableObfuscation=true`) çalışıyor; `mappings` satırı yok ve bağımlılıklar
> `modImplementation` yerine `implementation` olarak tanımlı. Access widener `official`
> namespace kullanıyor.

## Yapılan dönüşümler

| Sistem | NeoForge | Fabric 26.2 |
|---|---|---|
| Bootstrap | `@Mod(ModContainer, IEventBus)` | `ModInitializer` / `ClientModInitializer` |
| Registry | 17 `DeferredRegister` | `Registry.register` + `FabricRegistryBuilder` (SYNCED) + `DynamicRegistries` |
| Config | `ModConfigSpec` + `ModContainer.registerConfig` | Aynı spec sınıfları + FCAP `ConfigRegistry` (3 TOML dosyası aynı isimde) |
| Event bus | `NeoForge.EVENT_BUS`, `IEventBus` | Fabric lifecycle/player/item/entity/HUD event'leri + mixin'ler |
| Attachment | `AttachmentType` + `getData` | `AttachmentRegistry` + `getAttached`/`setAttached` (`copyOnDeath` korundu) |
| Item capability | `ItemCapability` | `ItemApiLookup` + varsayılan fallback |
| Networking | `PayloadRegistrar`/`PacketDistributor` | `PayloadTypeRegistry.clientboundPlay/serverboundPlay`, `ServerPlayNetworking`, `ClientPlayNetworking`, `PlayerLookup`; 15 payload korundu, NETVERSION=4 |
| Loot | 4 `IGlobalLootModifier` + 22 JSON | `LootTableEvents.MODIFY` ile programatik havuzlar (aynı tablo/chance/coğul koşullar) |
| Recipe condition | `neoforge:conditions` + `ICondition` | `fabric:load_conditions` + `ResourceConditionType` |
| Özel ingredient | `IngredientType`/`ICustomIngredient` | `CustomIngredientSerializer` (26.2 `items(): Stream<Holder<Item>>`) |
| `SizedIngredient` | NeoForge sınıfı | Yerel `tictim.paraglider.contents.recipe.SizedIngredient` (aynı JSON alanları) |
| Brewing | `RegisterBrewingRecipesEvent` | `MixinPotionBrewing` + `ParagliderBrewingRecipes` |
| Arm pose | `enumextensions.json` + `IArmPoseTransformer` | `HumanoidRenderState` flag'i + `MixinHumanoidModel` |
| Model property | `RegisterConditionalItemModelPropertyEvent` | `ConditionalItemModelProperties.ID_MAPPER` accessor mixin'i |
| Render pipeline | `RegisterRenderPipelinesEvent` | Pipeline'lar vanilla'da kendi kendine oluşuyor; shaders ve `stamina_wheel.png` eklendi |
| HUD | `RegisterGuiLayersEvent` | `HudElementRegistry.addLast` + `GuiGraphicsExtractor` submit mixin'i |
| Keybind | `KeyMapping` + `KeyModifier` | `KeyMappingHelper.registerKeyMapping` (Ctrl+P yerine P; bakınız bilinen sorunlar) |
| Config ekranı | `IConfigScreenFactory` | FCAP `ConfigScreenFactoryRegistry` |
| AT | `accesstransformer.cfg` | `paraglider.accesswidener` (official namespace, descriptor'larla) |
| Plugin loader | `ModList.getAllScanData()` | `paraglider-plugin` entrypoint'leri (`FabricParagliderPluginLoader`) |
| Creative flight ban | `NeoForgeMod.CREATIVE_FLIGHT` attribute | `Abilities.flying` flag'i geçici olarak kapatılıp geri yükleniyor |

Korunan oyun mekanikleri: stamina/wheel, uçuş ve rüzgâr, vessel kapları, bargain sistemi,
loot ödülleri (wither/elder guardian/spawner/trial/tower), köy yapıları, ilerleme kayıtları,
komutlar, paket senkronizasyonu ve tüm config seçenekleri.

## Test sonuçları

| Test | Sonuç |
|---|---|
| `./gradlew clean build` | ✅ BUILD SUCCESSFUL, JAR üretildi |
| `./gradlew runServer` (headless) | ✅ Sunucu başladı, dünya oluşturuldu ("Done"), 1608 recipe / 1710 advancement yüklendi |
| 3 config dosyası | ✅ `paraglider-common.toml`, `paraglider-server.toml`, `paraglider-player-states.toml` oluştu |
| `/help` üzerinden komut | ✅ `/paraglider (query\|set\|give\|take\|bargain\|reloadPlayerStates)` mevcut |
| `xvfb-run ./gradlew runClient` | ✅ Client başladı, item modelleri (dahil `paraglider:paragliding` conditional property) hatasız yüklendi, atlas'lar oluşturuldu |
| Mixin doğrulaması | ✅ Sunucu ve client'ta mixin hatası / injection uyarısı yok |

Önemli: `run/` dizininde test sunucusunun dünya ve config dosyaları bulunuyor; bunlar
doğrulama amaçlıdır, modun bir parçası değildir.

## Yayın öncesi denetim (ikinci tur)

### Diğer Fabric portları araştırması
- **Tictim/Paraglider `release/1.20.1`** — upstream'in kendi resmi Fabric modülü (20.1.0–20.1.4).
  Referans olarak inceleildi: `FabricLootTable` + `LootConditions`/`LootFunctions`,
  `FabricContents`, `FabricParagliderNetwork`, `FabricConfig`, `FabricParagliderPluginLoader`,
  client `MixinGui`/`MixinItemInHandRenderer`, `MixinChunkMap` (chunk-watch rüzgâr senkronu).
- **MrOz59/paraglider-fabric** — 1.21.1 / 21.1.5 (projede zaten referans olarak kullanıldı).
- **BluSpring/Paraglider** (`fabric/1.19.2-crimecraft`), **Nikt4l/Paraglider** (`dev/1.20.2`),
  **Flechazo098/Paraglider-Reborn** (`release/1.20.4`) — eski sürüm portları; 26.2'ye uygulanabilir
  kalıcı bir düzeltme bulunmadı (ör. campfire crash ve ChunkMap çözümleri 26.2'de karşılığını buldu).
- 1.18.x, 1.19.3, 1.21.5 ve 26.x için başka Fabric portu bulunamadı. Bu port 26.2 için ilk port.

### Denetimde düzeltilenler
- Kesirli spawner/trial loot değerleri (`0.5` gibi) artık **şanslı ek drop** olarak çalışıyor
  ( upstream `ConfigurableSpiritOrbLoot` mantığı; `NumberProvider.getInt` override'ı).
- `spiritOrbGens` config seçeneği spawner/trial/spawner loot'una da uygulanıyor (resmi Fabric
  1.20.1 portuyla aynı davranış).
- `LootTableEvents.MODIFY` içinde `source.isBuiltin()` koruması eklendi; datapack override'ları
  çift loot almıyor (resmi Fabric portundaki koruma).
- Elder Guardian loot'una eksik `spiritOrbGens` koşulu eklendi.
- **İstemci tarafında `WindLevel` kaydı eksikti** (NeoForge `LevelEvent.Load` istemcide de
  tetikleniyor, Fabric'te sadece sunucu level event'i var). İstemci level değişiminde
  kayıt/silme eklendi; aksi halde rüzgâr parçacıkları hiç görünmezdi.
- İkinci bağımsız denetimde şunlar düzeltildi:
  - Custom stamina-wheel render pipeline'ları vanilla'nın özel registry'sine kaydediliyor
    (`RenderPipelines.register` private metodu için `@Invoker` mixin'i).
  - Paraglider/Deku Leaf için deri onarımı (`repairable(c:leathers)`) geri eklendi.
  - İstemci tarafındaki sağ tık etkileşimleri de iptal ediliyor (kol sallama animasyonu dahil).
  - Ayar tuşu yeniden **Ctrl+P** (Fabric `KeyMapping`'de modifier olmadığı için tick içinde kontrol).
  - Sunucu başlangıcında istemcinin eski senkronize state-map'i temizleniyor.
  - Debug pipeline'ından gereksiz sampler bağlanması kaldırıldı (shader uyarısı yok).
- Kaynak dosya karşılaştırması: upstream'in 214 asset/data dosyasının **tamamı** JAR'da mevcut
  (yalnızca NeoForge'a özel `META-INF/*` ve `loot_modifiers/*.json` bilerek hariç).

## Uyumluluk ve son doğrulamalar

- **Creative menü:** çalışma zamanında `CREATIVE_MODE_TAB` registry'si sorgulandı;
  `paraglider:paraglider` sekmesi mevcut ✅
- **William Wythers' Overhauled Overworld 2.7.1 (Fabric, MC 26.2) + Cristel Lib 3.1.13:**
  gerçek JAR'lar indirilip modla birlikte çalıştırıldı:
  - Sunucu hatasız başladı, WWOO veri paketleri yüklendi, 1608 recipe yüklendi ✅
  - `/locate structure paraglider:tarrey_town_goddess_statue` WWOO dünyasında yapıyı buldu ✅
  - Client hatasız başladı (mixin/registry çakışması yok) ✅
  - WWOO vanilla loot tablolarını veya köy template pool'larını ezmiyor; bu portun
    `isBuiltin()` koruması ve köy enjeksiyonu ile çakışması yok.
- Geçici uyumluluk/kayıt kodu testten sonra kaldırıldı; teslim JAR'ı temiz kaynaktan derlendi.

## Kritik oyuncu-giriş hatası düzeltmesi (kullanıcı raporu)

Kullanıcı, oyuncu dünyaya katılırken şu çökmeyi bildirdi:
`NullPointerException: SimpleVesselContainer.onChange(...) because "vessels" is null`
(`ServerPlayerMovement.<init>` → `AttachmentProvider.createPlayerMovement` → JOIN event).

**Kök neden:** Fabric Data Attachment API'de `getAttached(type)` eksik değer için **null döner**;
tipin `initializer` değeri yalnızca `getAttachedOrCreate(type)` çağrısında kullanılır. NeoForge
`getData()` karşılığı olarak yaptığım global `getAttached()` dönüşümü vessel/stamina/movement
attachment'larını başlatmadan null döndürüyordu.

**Düzeltme:** `vesselContainer`, `botwStaminaData`, `movementState`, `movementInitialized` için
tüm `getAttached(...)` çağrıları `getAttachedOrCreate(...)` ile değiştirildi. Doğrulama için
sunucuda yapay `ServerPlayer` oluşturup `Movement.get(player)` çağrıldı:
`ATTACH_TEST_OK=ServerPlayerMovement vessels=SimpleVesselContainer` ✅. Geçici test kodu
kaldırıldı ve temiz JAR yeniden derlendi. Bu düzeltme olmadan hiçbir oyuncu dünyaya giremezdi.

## Bilinen farklar / dikkat edilmesi gerekenler

1. **Keybinding:** Ctrl+P davranışı korundu; Fabric `KeyMapping`'de modifier alanı olmadığı için
   Ctrl tuşu client tick içinde kontrol ediliyor.
2. **Creative flight ban:** NeoForge'un `CREATIVE_FLIGHT` attribute'u yerine `Abilities.flying`
   flag'i kullanılıyor. Sunucu tarafında aynı sonuç verir; istemci tarafında kısa bir görsel
   gecikme olabilir.
3. **Config ekranı:** Forge Config API Port üzerinden kayıtlı; Mod Menu önerilir (FCAP uyarısı),
   ancak modun çalışması için gerekli değil.
4. **Anvil onarımı:** Deri ile onarım geri eklendi; grindstone onarımı engellendi. Upstream'in
   `isCombineRepairable=false` kuralı Fabric'te doğrudan karşılık bulmadığından anvil onarımı
   mümkün olabilir.
5. **Loot kapsamı:** `source.isBuiltin()` koruması nedeniyle bir datapack vanilla loot tablosunu
   tamamen değiştirirse modun eklemeleri o tabloya uygulanmaz (upstream resmi Fabric portuyla aynı).
6. **Üçüncü parti eklentiler:** NeoForge `ParagliderItemCapability.CAPABILITY` kullanan
   eklentiler Fabric `ParagliderItemCapability.LOOKUP`'a taşınmalı.
7. `server.properties` uyarısı test sunucusundan geliyor; mod ile ilgisi yok.
