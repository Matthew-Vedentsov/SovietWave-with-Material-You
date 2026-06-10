# SovietWave Radio 📻

[RU] Потоковое радио-приложение для прослушивания музыки в стиле Sovietwave. 


[EN] A streaming radio application dedicated to Sovietwave music.

---

## Features / Возможности

### Eng
*   **High Quality Streaming**: Listen Sovietwave with low latency.
*   **Smart Metadata**: Real-time track information (Artist, Title) with automatic artwork fetching from iTunes API.
*   **Dynamic UI**: Modern Jetpack Compose interface with "Pulse" animations and full landscape support.
*   **Favorites System**: Save your favorite tracks locally and access them anytime.
*   **Broadcast History**: See what was playing recently and find artist links (SoundCloud, VK, etc.).
*   **Media3 Integration**: Full background playback support with rich notifications and lock screen controls.
*   **Robust Networking**: Optimized for various network conditions, including IPv4 fallback for reliable artwork loading.

### RU
*   **Высокое качество потока**: Слушайте  Sovietwave  с минимальной задержкой.
*   **Умные метаданные**: Информация о треке в реальном времени (Артист, Название) с автоматической загрузкой обложек через iTunes API.
*   **Динамичный интерфейс**: Современный UI на Jetpack Compose с анимацией "пульсации" и поддержкой альбомной ориентации.
*   **Система избранного**: Сохраняйте любимые треки в локальную базу и возвращайтесь к ним позже.
*   **История трансляции**: Просматривайте недавно проигранные треки и переходите по ссылкам на исполнителей (SoundCloud, VK и др.).
*   **Интеграция с Media3**: Полноценная фоновая работа с управлением через уведомления и экран блокировки.
*   **Стабильная сеть**: Оптимизировано для разных условий сети, включая обход проблем с IPv6 для надежной загрузки обложек.

---

## Tech Stack / Технологии

*   **Language**: Kotlin
*   **UI**: Jetpack Compose (Material 3)
*   **Playback**: Android Media3 / ExoPlayer
*   **Networking**: OkHttp, HttpURLConnection
*   **Storage**: Jetpack DataStore (Preferences)
*   **Image Loading**: Coil
*   **Architecture**: MVVM with StateFlow & Coroutines

---

## Development / Разработка

### Build / Сборка
1. Clone the repository / Клонируйте репозиторий.
2. Open in Android Studio (Ladybug or newer).
3. Build the `:app` module.

### API
The app uses the **WaveRadio API** for metadata and history, and **iTunes Search API** for artwork.
