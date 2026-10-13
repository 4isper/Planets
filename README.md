# Planets (NASA APOD)

Каталог астрономических фотографий NASA (APOD — Astronomy Picture of the Day).
Главный экран — сетка с бесконечной прокруткой, второй — детализация
(фото, название, дата, автор, описание) со стрелкой «назад».
Данные кешируются локально, поэтому приложение работает без интернета.

## Технологии

| | |
|---|---|
| UI | Kotlin, Jetpack Compose (Material3), Navigation Compose |
| Сеть | Retrofit 3 + Gson, OkHttp 5 |
| Кеш | Room 2.8 (KMP-ready драйвер `androidx.sqlite`) |
| DI | Hilt 2.60 + KSP 2.3 |
| Изображения | Coil 3 |
| Сборка | AGP 9.4.1, Gradle 9.8.1, JDK 21, compileSdk 37, minSdk 26 |

Все зависимости версионируются в `gradle/libs.versions.toml`.

## Источник данных

В сентябре 2026 года APOD переехал на бэкенд WordPress REST:
`https://science.nasa.gov/wp-json/wp/v2/apod-basic`. Старый эндпоинт
`api.nasa.gov/planetary/apod` в режиме заглушки возвращает логотип NASA и
одинаковый заголовок «NASA Science» на любые даты, поэтому приложение работает
с новым источником. Ключ API не требуется — эндпоинт публичный (legacy API
архивируется 1 декабря 2026).

Параметры запроса: `page`, `per_page` (сервер молча режет до 25), `orderby=date`,
`order=desc`, `_fields` — список запрашиваемых полей.

Адрес фида лежит в `gradle.properties` как `apodBaseUrl` — это не секрет, поэтому
он в репозитории; переопределить для конкретной сборки можно флагом:

```bash
./gradlew assembleDebug -PapodBaseUrl=https://staging.example.com/
```

## Архитектура

Три слоя, разбитые на Gradle-модули. Стрелки зависимостей смотрят внутрь:

```
app          → domain, data:di
data:di      → domain, data:local, data:remote, data:repository
data:repository → domain, data:local, data:remote
data:local, data:remote → (внешние зависимости)
domain       → (внешних зависимостей нет)
```

- **`domain`** — чистый JVM-модуль. Бизнес-правила: `Pagination` (условие
  окончания архива), правила отображения в `Planet`, `PlanetsFeed`,
  `LoadMoreResult`. Контракты: `PlanetRepository`, четыре use case.
- **`data:remote`** — `NasaApiService`, DTO `ApodResponse`, обёртка `ApodPage`
  (пункты + заголовок `X-WP-TotalPages`).
- **`data:local`** — Room: `ApodEntity` (с индексом по `(page, position)`),
  `ApodDao`, `RemoteKeys`, `PlanetsDatabase`.
- **`data:repository`** — `PlanetRepositoryImpl` и мапперы
  DTO ↔ Entity ↔ Domain, стрип HTML.
- **`data:di`** — Hilt-модули `RemoteModule`, `LocalModule`, `RepositoryModule`.
- **`app`** — presentation и дизайн-система:

```
app/src/main/java/com/m4isper/planets/
├── MainActivity.kt
├── PlanetsApplication.kt
├── navigation/         ← NavHost и маршруты
├── presentation/       ← фичи: экран + его ViewModel
│   ├── list/
│   └── detail/
└── ui/                 ← дизайн-система
    ├── theme/          ← цвета, типографика, тема
    ├── components/     ← PlanetCard, LoadError
    └── model/          ← PlanetUi + toUi()
```

Доменная модель не попадает в композаблы: единственное место, где встречаются
`domain` и `PlanetUi` — ViewModel-ы. В `ui/` домен известен только мапперу `toUi()`.

## Нестандартные решения

1. **Пагинация вручную, без Paging 3.** У списка один источник данных — Room,
   а сеть подключена через колбэк: `LazyVerticalGrid` следит за позицией
   последнего видимого элемента через `snapshotFlow` и за 5 элементов до конца
   вызывает `LoadMorePlanetsUseCase`. Триггер намеренно не живёт внутри
   последнего item: если страница целиком состоит из уже закэшированных дат,
   список не растёт, новый item не компонуется и item-эффект второй раз не
   сработает — пагинация встала бы.
2. **Конец архива — правило домена.** `Pagination.hasMore` сравнивает текущую
   страницу с `X-WP-TotalPages` (сейчас 572 страницы по 25 записей), поэтому
   архив может расти без правок кода. `LoadMorePlanetsUseCase` возвращает
   `LoadMoreResult.Loaded` или `EndReached`, а репозиторий не решает за домен,
   когда остановиться.
3. **Дедупликация и стабильный порядок.** Первичный ключ — `date` (у APOD дата
   уникальна), вставка `INSERT OR IGNORE`, выдача сортируется по `(page, position)`,
   поэтому порядок кеша не меняется между перезапусками.
4. **Offline-first.** `refresh()` сначала грузит сеть и только после успеха в одной
   транзакции меняет кеш — при обрыве связи старые данные не теряются, список
   сразу показывает кеш. Детализация читает запись из Room по `date`.
5. **Запросы не пересекаются.** Флаги занятости выставляются в `update { }`
   синхронно, до запуска корутины, иначе два вызова в одном кадре (свайп плюс
   триггер догрузки) прошли бы гард и рассинхронизировали кеш с `remote_keys`.
6. **HTML в фиде.** `explanation` и `credit` приходят фрагментами HTML
   (`<strong>`, `<a>`, сущности). Домен хранит чистый текст: `stripHtml` снимает
   теги и сущности, `stripApodHtml` отрезает служебный хвост NASA
   («APOD's main NASA site is moving…», «Tomorrow's picture:…»).
   Реализация на чистом Kotlin, без `android.text.Html`, чтобы работали JVM-тесты.
7. **Картинка в `hdurl`.** В новом API `url` — ссылка на страницу, изображение
   лежит в `hdurl` (для видео там кадр-превью). Отдельной ссылки на видео API не
   отдаёт, поэтому кнопка на детализации просто открывает страницу NASA.
8. **Экономия трафика.** Без `_fields` фид вшивает в каждую запись полную
   HTML-страницу (`basic_html`): 25 записей весят ~290 КБ вместо ~75 КБ.
9. **Coil 3 требует явный сетевой фетчер.** `coil-compose` тянет только
   `coil-core`; без `coil-network-okhttp` картинки не грузятся вообще, причём
   молча. Подключён `libs.coil.network.okhttp`.
10. **R8 только в `app`.** `isMinifyEnabled` и `isShrinkResources` включены в
    приложении. В библиотечных модулях R8 включать нельзя: shrink-ресурсов там
    запрещает сам AGP, а само-сжатие даёт вырезанные AAR, из-за чего Hilt теряет
    `*_Factory`, а Room и Retrofit — DAO, entity и сервисы. Правила для
    потребителей едут через `consumerProguardFiles` (`data/remote` держит keep
    для DTO, которые Gson читает рефлексией). Release-APK — ~1.5 МБ против
    ~13.5 МБ debug.
11. **Логирование.** `HttpLoggingInterceptor` в debug работает на уровне `BASIC`:
    метод, URL, код и тайминги. `BODY` писал бы в logcat ~75 КБ JSON на страницу.

## Тесты

33 unit-теста, без эмулятора и без мок-фреймворка — use cases собираются обычными
конструкторами, репозиторий подменяется написанным вручную `FakePlanetRepository`.

```bash
./gradlew :domain:test :data:repository:testDebugUnitTest :app:testDebugUnitTest
```

- `domain` — правила `Pagination` и `Planet`, поведение use cases
- `data:repository` — мапперы и стрип HTML
- `app` — машина состояний `PlanetsListViewModel`: первичная загрузка, ошибка с
  сохранением причины, ретрай, конец архива, защита от параллельных вызовов и от
  гонки refresh/loadMore

Для тестов ViewModel подключён `kotlinx-coroutines-test` и включён
`unitTests.isReturnDefaultValues = true` (иначе `android.util.Log` кидает
исключение вместо записи).

## Сборка

```bash
./gradlew assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease  # app/build/outputs/apk/release/ (подпись не настроена)
```
