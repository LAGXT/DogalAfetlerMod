# Makine ve ortam

Kurulum işçisi (`kurulum`) 2026-11-05'te ölçtü. Paralellik, RAM kuralı ve görsel test yolu bu dosyaya göre belirlenir
(docs/GOREV.md §2).

## Donanım ve işletim sistemi

| Öğe | Değer | Nasıl ölçüldü |
|---|---|---|
| İşletim sistemi | CachyOS Linux (Arch tabanlı), çekirdek `7.3.1-1-cachyos`, x86_64 | `/etc/os-release`, `uname -r` |
| CPU | 11th Gen Intel Core i5-1135G7 @ 2.40 GHz (en çok 4.2 GHz), 4 çekirdek / 8 iş parçacığı | `LC_ALL=C lscpu`, `nproc` |
| RAM | Toplam 15.4 GiB (`MemTotal: 16105068 kB`); ölçüm anında kullanılabilir 6–8 GiB (sahibinin açık uygulamaları ~8 GiB kullanıyor) | `free -g`, `free -m`, `/proc/meminfo` |
| Swap | 27 GiB (ölçümde ~1 GiB dolu) | `free -g` |
| GPU | Intel Iris Xe Graphics (TigerLake-LP GT2, tümleşik), Mesa 26.2.6, OpenGL 4.6 Core | `lspci`, `DISPLAY=:0 glxinfo -B` |
| Ekran | Dahili panel `eDP-1`, 1920x1080 @ 60 Hz, ölçek 1.25 | `hyprctl monitors` |
| Boş disk | `/home/lagxt`: 229 GiB'ın 192 GiB'ı boş | `df -h /home/lagxt` |

## Ekran oturumu

- Wayland oturumu, compositor **Hyprland 0.57.1** (`XDG_CURRENT_DESKTOP=Hyprland`). XWayland `:0` üzerinde çalışıyor.
- Ajan kabuğunda `WAYLAND_DISPLAY`, `DISPLAY` ve `HYPRLAND_INSTANCE_SIGNATURE` **boş** gelir. Soketler var:
  `/run/user/1000/wayland-1`, `/tmp/.X11-unix/X0`, Hyprland imzası `/run/user/1000/hypr/<imza>/` altında.
  `runClient`'ı kabuktan açan işçi `DISPLAY=:0` (XWayland; LWJGL/GLFW 1.20.1'de X11 kullanır) vermelidir; `hyprctl`
  için `HYPRLAND_INSTANCE_SIGNATURE=$(ls /run/user/1000/hypr/)` gerekir.
- **hypruse MCP** bu oturumda kurulu (`desktop`, `launch`, `hypr`, `screenshot`, `zoom`, `ui` ...): istemci penceresi ajan
  workspace'lerinde (90–99) tutulur, ekran görüntüsü hypruse `screenshot` ya da oyunun F2'siyle (`run*/screenshots/`)
  alınır. Görünmeyen workspace'te kare çizilmezse FPS ölçümü ve çekim için sahibine önceden sorulur (GOREV §2).

## Yazılım

| Araç | Sürüm / yol | Not |
|---|---|---|
| Java (sistem varsayılanı) | Temurin 21.0.13, `~/.local/share/mise/installs/java/21` (mise) | Dokunulmadı. `/usr/lib/jvm` boş. |
| **Java 17 (proje)** | Temurin **17.0.21+9**, `JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9` | Adoptium API'den arşiv indirilip `~/.jdks/`'e açıldı; sistem paketi yok. |
| Gradle | Wrapper 8.8 (MDK), `org.gradle.daemon=false`, `-Xmx3G` | `~/.gradle` ilk derlemede oluştu (ortak önbellek). |
| ForgeGradle / Forge | FG `[6.0,6.2)`, Forge **1.20.1-47.4.28** (promotions_slim: recommended = latest = 47.4.28) | |
| git | 2.56.0 | Global `user.name`/`user.email` **yok**; commit'ler `git -c user.name=... -c user.email=...` ile atılır. |
| Python (sistem) | 3.14.9; sistemde Pillow 13.0.0 ve numpy 2.5.4 de var | Proje bunları kullanmaz. |
| **Python (proje)** | `KÖK/.venv` (Python 3.14.9): Pillow 13.0.0, numpy 2.5.4, nbtlib 2.0.4 | `python3 -m venv .venv && .venv/bin/pip install -r tools/requirements.txt` |
| ffmpeg | n9.0.2 | Ses üretimi ve `ebur128` ölçümü için hazır. |
| ImageMagick | 7.1.2-35 (`magick`) | `convert` eskimiş uyarısı verir; `magick` kullanılır. |
| glxinfo / eglinfo / vulkaninfo | kurulu | |
| Yerel ayar | `LANG=tr_TR.UTF-8` | Gradle çıktısında `BUİLD SUCCESSFUL` yazılır (Türkçe İ); başarıyı çıkış kodundan okuyun. Kodda büyük/küçük harf dönüşümü `Locale.ROOT` ile yapılır. |

## Ağ erişimi

| Hedef | Sonuç |
|---|---|
| `https://maven.minecraftforge.net` | `HTTP/2 200` (`curl -sSI`) |
| `https://api.modrinth.com` | `HTTP/2 301` (kök yönlendirir); `/v2/project/<slug>/version` JSON döndü, Modrinth maven'dan bağımlılık çözüldü |
| `https://files.minecraftforge.net/.../promotions_slim.json` | erişilebilir; `1.20.1-recommended = 47.4.28` |
| `https://api.adoptium.net` | erişilebilir (JDK 17 arşivi buradan) |

Uyumluluk modları (Modrinth API, 2026-11-05): e4mc `6.2.3-forge` (2026-10-01, en güncel Forge 1.20.1), Cold Sweat
`2.4.5` (2026-11-01; Forge 1.20.1 sürüm kimliği `z3qJDJxm`). `-Puyum=e4mc|coldsweat|ikisi` ile açılır, varsayılan kapalı.

## Skill'ler ve MCP

| Skill | Durum |
|---|---|
| `test-driven-development` | Kurulu: `~/.omp/plugins/cache/plugins/superpowers-marketplace___superpowers___7.0.2/skills/` (omp superpowers 7.0.2) |
| `systematic-debugging` | Kurulu (aynı yer) |
| `verification-before-completion` | Kurulu (aynı yer) |
| `requesting-code-review` | Kurulu (aynı yer) |

`~/.claude/skills`, `~/.codex/skills` ve `~/.agents/skills` altında yalnız 21st.dev skill'leri var; yukarıdaki dördü orada
yok. `~/.pi` yok. Bu oturumda erişilebilen MCP sunucuları: context7 (kütüphane belgeleri), deepwiki, hypruse (masaüstü),
rea (tersine mühendislik), discord-docs.

## Proje sabitleri

- `KÖK=/home/lagxt/İndirilenler/DogalAfetlerMod` (yolda Türkçe `İ` var; Gradle ve Forge sorunsuz çalıştı).
- `JAVA_HOME=/home/lagxt/.jdks/jdk-17.0.21+9`; komutlar `JAVA_HOME=... ./gradlew <görev>` biçiminde çalıştırılır.
- **Test dünyası seed'i: `20261105`** (`gradle.properties: test_dunyasi_seed`). Dünya `run-server/test`'te üretildi ve
  `run/saves/test`'e kopyalandı (`LevelName=test`, `allowCommands=1`). Quick Play: `./gradlew runClient -PquickPlay=test`.
- Çalışma klasörleri: `runClient` → `run/`, `runServer` → `run-server/`, `runGameTestServer` → `run-gametest/`,
  `runData` → `run-data/`. Hepsi `.gitignore`'da; `runServer` ilk açılışta `eula.txt` ve `server.properties`'i,
  `runClient` `options.txt`'e `pauseOnLostFocus:false`'u kendisi yazar (yeni worktree'lerde de).

## Paralellik ve RAM önerisi

Ölçülen tepe değerler (Faz 0): `./gradlew build` ~3 GiB (Gradle `-Xmx3G`), `runServer`/`runGameTestServer` Gradle JVM'i +
sunucu JVM'i (varsayılan yığın RAM'in 1/4'ü ≈ 3.9 GiB). GOREV §2'ye göre her `runClient` yaklaşık 5–6 GiB.
Sahibinin uygulamaları ~8 GiB tuttuğu için kullanılabilir RAM 6–8 GiB arasında.

| Durum | Önerilen en çok eşzamanlı ağır süreç |
|---|---|
| Derleme, test, runData, runGameTestServer, runServer | **2** (her biri başlamadan `free -g` → kullanılabilir ≥ 4 GiB ve `pgrep -fc GradleWrapperMain` < 2) |
| Bir `runClient` açıkken | **1** (yalnız istemci); istemci + bir derleme ancak kullanılabilir ≥ 10 GiB ise |
| Çok oyunculu test (§6 #7) | Gradle'sız sunucu (`run.sh`, `-Xmx2G`) + **1** istemci gerçekçi; 2–3 istemci (~12–18 GiB) bu makinede sığmaz → eksik oyuncular GameTest sahte oyuncularıyla tamamlanır ve raporda belirtilir |

Swap 27 GiB olduğu için RAM aşımı çökme yerine ciddi yavaşlama olarak görünür; FPS/TPS ölçümleri (§6 #10) başka ağır
süreç yokken yapılmalıdır.

## Faz 4 öncesi ölçüm (2026-11-06, K-E19)

| Öğe | Değer | Nasıl ölçüldü |
|---|---|---|
| İş parçacığı | 8 (4 çekirdek) | `nproc` |
| RAM (boş Gradle yokken) | toplam 15 727 MiB, kullanılabilir **12 093 MiB** (11,8 GiB), boş 6 695 MiB, swap 1,8 / 27 GiB dolu | `free -m`, `free -g` (19:4x, bir başka işçinin Gradle'ı henüz başlamamışken) |
| RAM (bir Gradle + GameTest çalışırken) | kullanılabilir 8 979 MiB | `free -m` (19:5x) |

**Karar (K-E19 a):** kullanılabilir RAM 12 GB eşiğinin altında (11,8 GiB; sahibinin uygulamaları ~3,6 GiB kullanıyor ve
değişken). Bir `runGameTestServer` (Gradle `-Xmx3G` + sunucu JVM'i) kullanılabilir RAM'i ~3 GiB düşürüyor; üç eş zamanlı ağır
süreç ~9 GiB ister ve 4 GiB payı korunamaz. Tavan **iki** `./gradlew` kalır (GOREV §2). Kullanılabilir RAM sürekli ≥ 12 GB
ölçülürse (sahibi uygulamalarını kapatınca) şef üçe çıkarabilir.

**Gradle daemon ölçümü (K-E19 b):** `cleanTest test --offline` (472 JUnit), ikişer ardışık koşu, log'lar
`.vibe/kanit/faz3-kapanis/zaman-*.log`.

| Düzen | Koşu 1 | Koşu 2 | Fork |
|---|---|---|---|
| A: bugünkü (`org.gradle.daemon=false`, `jvmargs=-Xmx3G`) | 48 sn | 53 sn | her çağrıda "single-use Daemon" |
| B: A + `GRADLE_OPTS=-Xmx3G` | 37 sn | 33 sn | yine "single-use Daemon" |
| C: `--daemon` | 38 sn | 13 sn | ilki daemon açar, ikincisi sıcak daemon'u kullanır |

Kazanç yalnız sıcak daemon'la (C koşu 2). Daemon boşta 3 saat yaşar, `-Xmx3G`'ye kadar büyür ve ağır süreç kapısının sayacına
girmez; iki Gradle tavanlı bu makinede RAM payını yer. Daemon kapalı kalır, `gradle.properties` değişmedi.

## Pratik notlar

- **Gradle sayacı:** `pgrep -fc GradleWrapperMain`, komut satırında aynı kelimeyi taşıyan çağıran kabuğu da sayar
  (boş makinede `1` dönebilir). Doğru sayım: `pgrep -c -f '[G]radleWrapperMain'`.
- **Sunucu konsolu:** `runServer` stdin'i sunucuya iletir (KARARLAR K0-18). Otomasyonda sunucu pty'siz ve stdin'i bir
  boruya bağlı açılır, `Done (` görülünce `stop` yazılır. pty'li arka plan servisi olarak açılan Gradle sarmalayıcısı
  duruyor (`T`) ve çıktı akmıyor.
- **Süreler (Faz 0):** ilk `build` (Forge decompile + önbellek) ~4 dk, sonrakiler ~40 sn; `runData` ~2.5 dk;
  `runGameTestServer` ~1.5–2 dk; `runServer` açılışı ~65 sn (`Done` ~10–40 sn, ilk dünya üretimi daha uzun).
