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

- `url` — ссылка на страницу NASA, изображение лежит в `hdurl` (у видео там
  кадр-превью). Отдельной ссылки на видео API не отдаёт, поэтому кнопка на
  детализации просто открывает страницу.
- `explanation` и `credit` приходят фрагментами HTML (`<strong>`, `<a>`,
  сущности). Домен хранит чистый текст: `stripHtml` снимает теги и сущности,
  `stripApodHtml` отрезает служебный хвост NASA («APOD's main NASA site is
  moving…», «Tomorrow's picture:…»). Реализация на чистом Kotlin, без
  `android.text.Html`, чтобы работали JVM-тесты.

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

## Тесты

57 unit-тестов, без эмулятора и без мок-фреймворка — use cases собираются обычными
конструкторами, репозиторий подменяется написанным вручную `FakePlanetRepository`.

```bash
./gradlew :data:remote:testDebugUnitTest :domain:test :data:repository:testDebugUnitTest :app:testDebugUnitTest
```

- `domain` — правила `Pagination` (включая конец архива по пустой странице)
  и `Planet`, поведение use cases
- `data:remote` — формат `YYMMDD` для маршрута одной записи
- `data:repository` — мапперы и стрип HTML
- `app` — машины состояний обоих экранов: первичная загрузка, ошибка с
  сохранением причины, ретрай, конец архива, защита от параллельных вызовов и от
  гонки refresh/loadMore, «записи нет в фиде» как терминальное состояние,
  тёплый кеш при упавшей сети (включая случай, когда кеш приходит позже ответа)

Для тестов ViewModel подключён `kotlinx-coroutines-test` и включён
`unitTests.isReturnDefaultValues = true` (иначе `android.util.Log` кидает
исключение вместо записи).

## Сборка

```bash
./gradlew assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease  # app/build/outputs/apk/release/ (подпись не настроена)
```
