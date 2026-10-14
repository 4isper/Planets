# Planets (NASA APOD)

Каталог астрономических фотографий NASA (APOD — Astronomy Picture of the Day).
Главный экран — сетка с бесконечной прокруткой, второй — детализация
(фото, название, дата, автор, описание) со стрелкой «назад».
Данные кешируются локально, поэтому приложение работает без интернета.

## Технологии

| | |
|---|---|
| UI | Kotlin 2.4.21, Jetpack Compose (Material3), Navigation3 1.2.0 (`NavDisplay`) |
| Сеть | Retrofit 3.0.0 + Gson, OkHttp 5 |
| Кеш | Room 2.8.5 |
| DI | Hilt 2.60.1 + KSP 2.3.12 |
| Изображения | Coil 3.6.3 (+ `coil-network-okhttp`), Telephoto 0.19.0 (zoomable-viewer) |
| Текст | Jsoup 1.23.2 (HTML-фрагменты → `AnnotatedString`) |
| Сборка | AGP 9.4.1, Gradle 9.8.1, JDK 21, compileSdk 37, minSdk 26, Compose BOM 2026.09.00 |

Все зависимости версионируются в `gradle/libs.versions.toml`.

## Источник данных

В сентябре 2026 года APOD переехал на бэкенд WordPress REST:
`https://science.nasa.gov/wp-json/wp/v2/apod-basic`. Старый эндпоинт
`api.nasa.gov/planetary/apod` в режиме заглушки возвращает логотип NASA и
одинаковый заголовок «NASA Science» на любые даты, поэтому приложение работает
с новым источником. Ключ API не требуется — эндпоинт публичный (legacy API
архивируется 1 декабря 2026).

Параметры запроса: `page`, `per_page` (сервер молча режет до 25) и `_fields` —
список запрашиваемых полей. Без `_fields` фид вшивает в каждую запись полную
HTML-страницу (`basic_html`): 25 записей весят ~290 КБ вместо ~75 КБ. Порядок
не настраивается: `orderby` и `order` не зарегистрированы и игнорируются, новые
записи всегда первыми — на этом и стоит пагинация.

Одна запись берётся отдельным маршрутом: `wp-json/wp/v2/apod-basic/{YYMMDD}`,
где дата — шесть цифр без разделителей (`2026-10-12` → `261012`). Фильтров по
дню у списка нет: `date`, `slug`, `apod_date` и `search` не зарегистрированы как
query-аргументы и молча игнорируются, а объявленная сервером пара
`date_from`/`date_to` отвечает `400`. Отсутствующая дата — `404
apod_basic_not_found`, и это ответ, а не сбой: повторять его бессмысленно.

Две особенности формата, на которые опирается код:

- `url` — ссылка на страницу NASA (`permalink ?: url`), изображение — `hdurl`
  (у `video`/`iframe` там кадр-превью, иначе `hdUrl ?: url`). Отдельной ссылки
  на видео API не отдаёт, поэтому кнопка на детализации открывает `permalink`
  («смотреть видео» / «открыть оригинал»).
- `explanation`, `credit`/`copyright` приходят фрагментами HTML (`<strong>`,
  `<em>`, `<a>`, сущности). Домен хранит их как raw HTML с вырезанным
  служебным хвостом NASA («APOD's main NASA site is moving…»,
  «Tomorrow's picture:…»): `cutApodBoilerplateHtml` режет хвост, но теги
  оставляет, UI рендерит их через Jsoup в `AnnotatedString` (`toRichText()`:
  bold/italic/кликабельные ссылки). Чистый текст без тегов хранят только
  `title`/`alt` (`stripHtml` на чистом Kotlin), `stripApodHtml` — plain-вариант
  для legacy-строк кеша без тегов.

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
domain       → (только `javax.inject`, `kotlinx-coroutines-core`)
```

- **`domain`** — JVM-модуль без Android-зависимостей. Бизнес-правила: `Pagination` (условие
  окончания архива), правила отображения в `Planet`, `PlanetsFeed`.
  Контракты: `PlanetRepository`, пять use case (`GetPlanets`, `RefreshPlanets`,
  `LoadMorePlanets` + `LoadMoreResult`, `GetPlanetDetails`,
  `LoadPlanetDetails` + `LoadPlanetDetailsResult`).
- **`data:remote`** — `NasaApiService`, DTO `ApodResponse` (`model/`), обёртка
  `ApodPage` (`getApodPage`, `MAX_PAGE_SIZE = 25`, заголовок `X-WP-TotalPages`)
  и `ApodRecord` (`getApodRecord`, `toApodPathSegment` для `YYMMDD`).
- **`data:local`** — Room: `ApodEntity` (с индексом по `(page, position)`),
  `ApodDao`, `RemoteKeys` + `RemoteKeysDao`, `PlanetsDatabase`.
- **`data:repository`** — `PlanetRepositoryImpl` и мапперы
  DTO ↔ Entity ↔ Domain (`ApodMappers`, `PaginationMappers`, `HtmlText`:
  `stripHtml` для `title`/`alt`, `cutApodBoilerplateHtml` для `explanation`/`credit`).
- **`data:di`** — Hilt-модули `RemoteModule`, `LocalModule`, `RepositoryModule`.
- **`app`** — presentation и дизайн-система:

```
app/src/main/java/com/m4isper/planets/
├── MainActivity.kt
├── PlanetsApplication.kt
├── navigation/         ← NavHost на Navigation3 (`NavDisplay`, `NavKey`)
├── presentation/       ← фичи
│   ├── list/           ← экран + ViewModel сетки
│   └── detail/         ← экран + ViewModel + `PlanetDetailViewModelFactory` (Hilt EntryPoint: Navigation3 не даёт `SavedStateHandle`) + `ImageViewer` (Telephoto zoom)
└── ui/                 ← дизайн-система
    ├── theme/          ← цвета, типографика, тема
    ├── components/     ← PlanetCard, LoadError
    └── model/          ← PlanetUi + toUi(), PlanetCardUi + toCardUi(), RichText + toRichText() (Jsoup)
```

Доменная модель не попадает в композаблы: единственное место, где встречаются
`domain` и UI-модели — ViewModel-ы и мапперы `toUi()` / `toCardUi()` / `toRichText()`.
Карточки сетки (`PlanetCardUi`) — только plain-строки без Jsoup, поэтому маппятся
в Main-потоке; rich-текст (`PlanetUi`) маппится на фоновом диспетчере.

## Тесты

67 unit-тестов, без эмулятора и без мок-фреймворка — use cases собираются обычными
конструкторами, репозиторий подменяется написанным вручную `FakePlanetRepository`.

```bash
./gradlew :data:remote:testDebugUnitTest :domain:test :data:repository:testDebugUnitTest :app:testDebugUnitTest
```

- `domain` — правила `Pagination` (включая конец архива по пустой странице)
  и `Planet`, поведение use cases
- `data:remote` — формат `YYMMDD` для маршрута одной записи
- `data:repository` — мапперы и HTML: `stripHtml` для `title`/`alt`,
  `cutApodBoilerplateHtml` с сохранением тегов для `explanation`/`credit`
- `app` — машины состояний обоих экранов: первичная загрузка, ошибка с
  сохранением причины, ретрай, конец архива, защита от параллельных вызовов и от
  гонки refresh/loadMore, «записи нет в фиде» как терминальное состояние,
  тёплый кеш при упавшей сети (включая случай, когда кеш приходит позже ответа),
  плюс `RichText` (Jsoup → `AnnotatedString`) и `PlanetCardUi`-маппинг

Для тестов ViewModel подключён `kotlinx-coroutines-test` и включён
`unitTests.isReturnDefaultValues = true` (иначе `android.util.Log` кидает
исключение вместо записи).

## Сборка

```bash
./gradlew assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease  # app/build/outputs/apk/release/ (подпись не настроена)
```
