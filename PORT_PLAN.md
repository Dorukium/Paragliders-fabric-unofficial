# Paraglider 26.2.1 (NeoForge) → Fabric 26.2 Portlama Planı

> Durum: **PLAN ONAYLANDI VE TÜM AŞAMALAR TAMAMLANDI.**
> Uygulama sonuçları, testler ve bilinen farklar için `PORT_REPORT.md` dosyasına bakın.

---

## 1. Analiz bulguları (doğrulanmış gerçekler)

### 1.1 Kaynaklar
| Kaynak | Bulgu |
|---|---|
| `Paraglider-neoforge-26.2.1.jar` | 242 `.class`, 104 `src/main/resources` + 132 generated resource. JAR içeriği `META-INF/neoforge.mods.toml` ile sürüm `26.2.1`. |
| Orijinal repo | `Tictim/Paraglider` → etiket **`26.2.1` = commit `21027ae`** (`release/26.2`). 181 Java dosyası. JAR ile kaynak sınıf/imza listesi karşılaştırıldı, **eşleşiyor**. |
| Referans Fabric port | `MrOz59/paraglider-fabric` → tek commit, upstream **21.1.5 / MC 1.21.1** tabanlı. Yalnızca API dönüşüm kalıbı için referans; 1.21.1 oyun kodu kopyalanmayacak. |

### 1.2 NeoForge bağımlılığı haritası
- 181 Java dosyasından **31'i** doğrudan `net.neoforged.*` import ediyor (104 import satırı).
- Kalan ~150 dosya büyük ölçüde vanilla 26.2 kodu → **olduğu gibi taşınabilir**.
- Kullanılan NF sistemleri: `@Mod`/`ModContainer`/`IEventBus`, `DeferredRegister` (17 adet), `RegisterEvent`/`NewRegistryEvent`/`DataPackRegistryEvent`, `NeoForge.EVENT_BUS` (~20 listener), `ModConfigSpec`/`ModConfig`, `AttachmentType` + `getData`, `ItemCapability` + `getCapability`, `PayloadRegistrar`/`PacketDistributor`/`IPayloadContext`, `IGlobalLootModifier` + `data/neoforge/loot_modifiers`, `ICondition`, `ICustomIngredient`, `SlotDisplay`, `ConsumeEffect` registry, `IArmPoseTransformer` + `enumextensions.json`, `KeyModifier`/`KeyConflictContext`, `RegisterGuiLayersEvent`, `RegisterRenderPipelinesEvent`, `RegisterRenderStateModifiersEvent`, `RegisterConditionalItemModelPropertyEvent`, `ModList.getAllScanData()` (plugin tarayıcı), `FMLPaths`/`FMLEnvironment`.
- **Kritik bulgu:** 26.2.1 upstream'inde **Curios/Trinkets yok** (paraglider artık sadece elde). Eski porttaki Trinkets/Cardinal bağımlılıkları **gerekmiyor**.

### 1.3 Doğrulanmış araç zinciri (probe projesi `/tmp/opencode/loom-probe` ile gerçek build)
```
Gradle 9.9.2  +  fabric-loom 1.17.21  +  Java 25  → BUILD SUCCESSFUL
```
- MC **26.2 artık obfuscate edilmiyor** (version manifest'inde `client_mappings` yok). Loom bunu `isUnobfuscated()` ile algılıyor.
- Bu yüzden build şu şekilde olmalı:
  - `gradle.properties` → `fabric.loom.disableObfuscation=true`
  - **mappings satırı YOK** (`loom.officialMojangMappings()` kullanılamaz — 26.2 için mappings yayımlanmıyor)
  - `modImplementation` yerine **`implementation`** (no-remap modunda `modImplementation` configuration'ı oluşmuyor)
  - `options.release = 25`
- Doğrulanan bağımlılık sürümleri:
  - Fabric Loader **0.19.5** (0.20.x de mevcut; 0.19.5 istendiği gibi kullanılacak)
  - Fabric API **0.161.0+26.2** (fabric.mod.json: loader ≥0.18.4, Java ≥25, MC ~26.2-)
  - Forge Config API Port **26.2.1** — sınıf: `fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry.INSTANCE.register(modId, ModConfig.Type, IConfigSpec[, filename])` (javap ile doğrulandı)
  - Kullanılabilir Fabric API modülleri: `fabric-data-attachment-api-v1` 2.2.19, `fabric-networking-api-v1` 6.3.4, `fabric-loot-api-v3` 3.0.17, `fabric-registry-sync-v0`, `fabric-resource-conditions-api-v1`, `fabric-key-mapping-api-v1`, `fabric-rendering-v1`, `fabric-screen-api-v1` vb.
- Doğrulanan API imzaları (26.2):
  - `PayloadTypeRegistry.clientboundPlay()` / `serverboundPlay()` / `clientboundConfiguration()` / `serverboundConfiguration()` — eski `playS2C()`/`playC2S()` **artık yok**.
  - `AttachmentRegistry.create(Identifier, Consumer<Builder>)`, `Builder.copyOnDeath()`, `syncWith(...)`.

---

## 2. Temel strateji

1. **Taban = upstream 26.2.1 kaynağı.** 181 Java dosyası `reference/original/src/main/java`'dan kopyalanır; 1.21.1 Fabric portundan **hiçbir oyun kodu alınmaz**.
2. **Sadece 31 NeoForge bağımlı dosya + bootstrap/event/network/mixin/client katmanı dönüştürülür.**
3. Eski Fabric portu yalnızca şu kalıplar için referans: `LootTableEvents.MODIFY` kullanımı, `FabricRegistryBuilder`/`DynamicRegistries` çağrıları, `CommandRegistrationCallback`, `MixinPotionBrewing`, `MixinPlayerChunkSender`, `fabric.mod.json` iskeleti.
4. **Aşamalı ilerleme:** her aşamanın sonunda `./gradlew build` çalıştırılır; hatalar o aşamada çözülmeden sonraki aşamaya geçilmez.
5. **Özellik kaybı yok:** mekanikler/itemler/bloklar/tarifler/yapılar/konfigler korunur. Fabric'te karşılığı olmayan NeoForge özellikleri (ör. `AddAttributeTooltipsEvent`, `IConfigScreenFactory`) için eşdeğer alternatif yazılır; yazılamazsa **sessizce çıkarılmaz, raporlanır**.

---

## 3. Hedef proje yapısı

```
paraglidermod/
├── PORT_PLAN.md                (bu dosya)
├── build.gradle                (Loom 1.17.21, Gradle 9.9.2)
├── gradle.properties           (disableObfuscation=true, sürümler)
├── settings.gradle
├── gradlew + wrapper           (Gradle 9.9.2)
├── LICENSE / NOTICE.md / TEMPLATE_LICENSE.txt
├── src/main/java/tictim/paraglider/...     (upstream 26.2.1 + Fabric dönüşümleri)
├── src/main/resources/
│   ├── fabric.mod.json
│   ├── paraglider.mixins.json / paraglider.client.mixins.json
│   ├── paraglider.accesswidener
│   ├── assets/paraglider/**     (JAR'dan, birebir)
│   └── data/paraglider/**       (JAR'dan; condition JSON'ları Fabric'e çevrilir)
└── README.md
```

---

## 4. Aşamalar (her aşama = build kapısı)

### Aşama 1 — İskelet, metadata, kaynaklar
- Gradle 9.9.2 wrapper, `settings.gradle`, `build.gradle` (Loom 1.17.21, `implementation` bağımlılıkları), `gradle.properties`.
- `fabric.mod.json`: id `paraglider`, sürüm `26.2.1+fabric`, entrypoints `main`→`tictim.paraglider.FabricParagliderMod`, `client`→`tictim.paraglider.FabricParagliderClientMod`; depends: `fabricloader >=0.19.5`, `fabric-api >=0.161.0`, `forgeconfigapiport >=26.2.1`, `java >=25`, `minecraft ~26.2-`.
- Tüm `assets/` + `data/` dosyaları upstream 26.2.1 JAR'ından kopyalanır.
- `neoforge:conditions` → `fabric:load_conditions`; `{"neoforge:ingredient_type":...}` → `{"fabric:type":...}`; `data/neoforge/loot_modifiers/**` kaldırılır (Yerine Aşama 5'te kod).
- Access transformer → access widener:
  - `StructureTemplatePool.rawTemplates` → `accessible mutable`
  - `StructureTemplatePool.templates` → `accessible`
  - `GuiGraphicsExtractor` sınıfı → `accessible class`
- Mixin config'leri `JAVA_25` ile oluşturulur.
- **Build kapısı:** boş entrypoint sınıflarıyla `./gradlew build` başarılı olmalı.

### Aşama 2 — Bootstrap, registry, config, plugin sistemi
- `FabricParagliderMod implements ModInitializer`, `FabricParagliderClientMod implements ClientModInitializer` (upstream `ParagliderMod`/`ParagliderClientMod` mantığı taşınır; `@Mod`, `ModContainer`, `IEventBus` kaldırılır).
- 17 `DeferredRegister` → doğrudan `Registry.register(BuiltInRegistries..., id, value)`; custom registry `FabricRegistryBuilder` + `attribute(SYNCED)`; datapack registry `DynamicRegistries.register/registerSynced`.
- `EntityAttributeModificationEvent` → `FabricDefaultAttributeRegistry.register(EntityTypes.PLAYER, Player.createAttributes()...)`.
- Config sınıfları **değiştirilmeden** korunur; kayıt: `ConfigRegistry.INSTANCE.register("paraglider", ModConfig.Type.COMMON, commonCfg.spec)` vb. (3 dosya: common, server, player-states).
- Plugin loader: `FabricLoader.getInstance().getAllMods()` + annotation taraması (eski portun `paraglider-plugin` entrypoint kalıbı), `@ParagliderPlugin` marker korunur.
- `enumextensions.json` → kaldırılır; arm pose Aşama 6'da mixin ile yapılır.
- **Build kapısı.**

### Aşama 3 — Event sistemi, mixin'ler, komutlar
- NeoForge event listener'ları Fabric API event'lerine çevrilir:
  - `RegisterCommandsEvent` → `CommandRegistrationCallback`
  - `ServerAboutToStart`/`Stopping` → `ServerLifecycleEvents`
  - `TagsUpdated` / `AddServerReloadListenersEvent` → `ServerLifecycleEvents.END_DATA_PACK_RELOAD`
  - `PlayerInteractEvent.*` → `UseBlockCallback`/`UseEntityCallback`/`UseItemCallback`/`Attack*Callback`
  - `PlayerTickEvent.Post`, `LivingEntityUseItemEvent.Tick`, `ClientTick`, chunk-watch, render-hand, crosshair → mixin (26.2 imzalarıyla **upstream'den yeniden yazılır**)
  - `StartTracking`, dimension change, login/logout, world/chunk tick → Fabric entity/lifecycle event'leri
- Mixin'ler: `MixinPlayer`, `MixinLocalPlayer`, `MixinRaid`, `MixinEnderDragonFight` upstream 26.2.1'den; ayrıca Fabric-only `MixinPlayerChunkSender`, `MixinPotionBrewing`, `ParagliderItemStackMixin`, `ParagliderGrindstoneMenuMixin`, client mixin'leri.
- `ParagliderCommands.register()` ağacı korunur (IdentifierArgument, permission seviyeleri, registryAccess).
- **Build kapısı.**

### Aşama 4 — Attachment/capability ve networking
- `AttachmentType` → `AttachmentRegistry.create(...)` (playerMovement, vesselContainer). `copyOnDeath()` için Fabric `Builder.copyOnDeath()` kullanılır; `getData()` çağrıları `entity.getAttached(...)`/`getAttachedOrCreate(...)` ile değiştirilir.
- Item capability (`stack.getCapability(ParagliderItemCapability.CAPABILITY)`) → Fabric `ItemApiLookup` + varsayılan fallback (`ParagliderUtils.getCaps` aynı davranışı korur).
- 15 packet sınıfı upstream'den taşınır (Codec/StreamCodec zaten 26.2 uyumlu). Registration: `PayloadTypeRegistry.clientboundPlay()/serverboundPlay()` (config-phase sync için `clientboundConfiguration()`), `ClientPlayNetworking`/`ServerPlayNetworking`. `NETVERSION` 4'e yükseltilir.
- `PlayerLookup` ile hedef oyuncu seçimi; receiver'larda gereksiz thread execute kaldırılır.
- **Build kapısı.**

### Aşama 5 — Oyun mekanikleri, loot, worldgen, recipe
- Stamina/movement/wind/vessel/bargain sistemleri upstream'den birebir taşınır (vanilla 26.2 API).
- Global loot modifier'lar → `LootTableEvents.MODIFY` ile programatik loot havuzları (eski portun `ParagliderLootTables` kalıbı, 26.2 `HolderLookup.Provider` imzasıyla). Wither fallback, ToTW dye roll, spawner kesirli count dahil **tüm** modifier'lar.
- Loot condition/function/consume-effect/slot-display/ingredient type kayıtları Fabric registry'lerine yapılır.
- Structure NBT'leri + `data/paraglider/worldgen/**` + biome tag'leri birebir kopyalanır. Custom structure piece kodları 26.2 API'sine uyarlanır; köy inject'i reload listener'da yapılır (idempotent hale getirilir).
- Tarifler: 26.2 JSON formatı korunur; condition/custom-ingredient JSON'ları Fabric formatına çevrilir. `CosmeticRecipe`, `SimpleBargainCodec`, `WaterBottleIngredientType` 26.2 codec API'siyle yazılır. Brewing recipe'leri `MixinPotionBrewing` ile eklenir.
- **Build kapısı.**

### Aşama 6 — Client: HUD, render, kontroller
- Stamina wheel render state (`StaminaWheelRenderState`, `StaminaWheelState`, shaders, yeni `stamina_wheel.png`) upstream'den taşınır. Fabric HUD extraction hook / `Gui` mixin ile `GuiGraphicsExtractor` elde edilir.
- `ParagliderRenderTypes` → `RenderPipelines.register(...)` (`RegisterRenderPipelinesEvent` yok).
- GUI layer'lar → `HudElementRegistry` / HUD mixin.
- Conditional item model property → Fabric model predicate registry veya mixin (doğrulanacak).
- Arm pose: `HumanoidRenderState` tabanlı `MixinPlayerModel` (enum extension yerine).
- Keybinding: `KeyBindingHelper.registerKeyBinding` + sade `KeyMapping` (KeyModifier/KeyConflictContext kaldırılır; Ctrl+P davranışı korunur).
- Partiküller: `BuiltInRegistries` + `ParticleFactoryRegistry`.
- Ayar ekranları (`ParagliderSettingsScreen`, `StaminaWheelSettingScreen`), ayar I/O'su korunur; config ekranı FCAP `ConfigScreenFactoryRegistry` ile bağlanır.
- **Build kapısı.**

### Aşama 7 — Doğrulama ve teslim
- `./gradlew clean build` → JAR üretimi.
- Headless dedicated-server smoke test: mod yükleniyor mu, registry'ler çakışıyor mu, komutlar çalışıyor mu, config dosyaları oluşuyor mu, datapack reload hatasız mı?
- JAR içeriği kontrolü: tüm asset/data dosyaları, `fabric.mod.json`, mixin config'leri, AW.
- Bilinen sorunlar + test sonuçları `README.md`/rapora yazılır.

---

## 5. Bağımlılıklar (nihai liste)

| Bağımlılık | Sürüm | Not |
|---|---|---|
| Minecraft | 26.2 | unobfuscated, mappings yok |
| Fabric Loader | 0.19.5 | Fabric API ≥0.18.4 istiyor, uyumlu |
| Fabric API | 0.161.0+26.2 | attachment, networking v1, loot v3, registry sync, resource conditions vb. |
| Forge Config API Port | 26.2.1 | `ModConfigSpec` sınıflarını birebir korumak için |
| Java | 25 | toolchain + `options.release = 25` |
| Gradle / Loom | 9.9.2 / 1.17.21 | probe ile doğrulandı |
| Trinkets / Cardinal Components | **YOK** | upstream 26.2.1'de gerekmiyor; FAPI Attachment API yeterli |

---

## 6. Riskler ve dikkat noktaları

- **Enum extension kaldırılıyor:** NeoForge'un `HumanoidModel.ArmPose` enum'unu genişletme mekanizması Fabric'te yok; aynı görsel sonuç render-state mixin'i ile verilecek. (Davranış korunacak, uygulama değişecek.)
- **`commonToClient` packet:** upstream `SyncPlayerStateMapMsg` config fazında gönderiliyor. Fabric'te `clientboundConfiguration()` + `ClientConfigurationNetworking` kullanılacak; sorun olursa play-fazına indirilip nedeni raporlanacak.
- **Loot modifier sırası:** `LootTableEvents.MODIFY` ile datapack override'larına karşı `isBuiltin()` koruması gerekiyor; trial-chamber tablo ID'leri 26.2'de doğrulanacak.
- **Village inject idempotence:** reload'larda tekrar ekleme yapılmaması için temizleme eklenecek.
- **Render pipeline / conditional model property:** Fabric API'de doğrudan karşılık olup olmadığı Aşama 6'da netleşecek; yoksa mixin yazılacak, sessizce atlanmayacak.
- **Addon uyumluluğu:** NeoForge plugin/capability API'sini kullanan üçüncü parti eklentiler Fabric sürümüne uyarlanmalı; bu portun kapsamı dışında, raporda belirtilecek.

---

## 7. Onay isteği

Bu plan onaylanırsa Aşama 1'den başlayıp her aşamada derleyerek ilerleyeceğim.
Onay vermeden önce değiştirmek istediğiniz bir nokta (ör. Loom sürümü, config yaklaşımı, bağımlılık tercihi) varsa söyleyin.
