# SpawnShield

Самостоятельный плагин спавна для Paper/Gale 26.3: кинематографичный телепорт `/spawn`, полная защита мира спавна и спасение из бездны.

Плагин **не связан с AuraCore** и другими Aura-плагинами: свой пакет `ru.spawnshield`, свои permissions, только Paper API.

## Что делает

- **Красивый телепорт**: отсчёт 3 секунды, затемнение (Blindness), заголовок «НЕ ДВИГАЙТЕСЬ» и подзаголовок «Идёт телепортация», отчёт в action bar, звуки и частицы.
- **Точная посадка**: игрок оказывается ровно в центре блока, с сохранённым yaw/pitch, без инерции и огня.
- **Отмена при движении или уроне**: сообщение в чат и action bar, затемнение сразу снимается.
- **Спасение из бездны**: падение ниже `minHeight - offset` или VOID-урон возвращает на спавн.
- **Защита мира целиком**: блоки, жидкости, огонь, взрывы, поршни, рост, лёд/снег, листья, порталы, рамки, стойки, лодки/вагонетки, голод, погода, спавн мобов и любой урон.
- **Режим редактирования** для администраторов: `/spawn edit true` снимает защиту, `/spawn edit false` возвращает.
- **Совместимость**: отдельный jar, не конфликтует с namespace Aura*. Загружается после EssentialsX, поэтому `/spawn` остаётся за SpawnShield.

## Установка

1. Скачайте `SpawnShield-2.0.0.jar` из релизов или соберите сами.
2. Удалите старый `AuraSpawn-1.0.0.jar` и папку `plugins/AuraSpawn`.
3. Положите jar в `plugins/`.
4. Перезапустите сервер.
5. Встаньте в нужную точку и выполните `/spawn set`.

Файл `plugins/SpawnShield/config.yml` создастся автоматически. Точка спавна хранится в `plugins/SpawnShield/spawn.yml`.

## Команды

| Команда | Что делает |
| --- | --- |
| `/spawn`, `/sp` | Телепорт на спавн |
| `/spawn set` | Сохранить точку спавна |
| `/spawn reload` | Перезагрузить конфиг |
| `/spawn edit [true\|false]` | Включить/выключить защиту для администратора |

## Права

| Permission | По умолчанию | Что даёт |
| --- | --- | --- |
| `spawnshield.use` | всем | `/spawn` |
| `spawnshield.admin` | оператор | `/spawn set`, `reload`, `edit` |
| `spawnshield.bypass` | оператор | Изменения в режиме редактирования |

## Конфиг

Основные ключи:

```yml
protected-worlds:
  - 'spawn'

teleport:
  warmup-seconds: 3
  cancel-distance: 0.35
  blindness: true
  effects: true
  countdown-actionbar: true

void:
  enabled: true
  offset: 8
  warmup-seconds: 0

protection:
  disable-weather: true
  block-hunger: true
  block-mob-spawns: true
  block-entity-damage: true
```

Все тексты — MiniMessage. `%time%` подставляется в отсчёт.

## Сборка

Нужен JDK 25.

```bash
./gradlew build
```

Готовый jar: `build/libs/SpawnShield-2.0.0.jar`.

## Лицензия

MIT
