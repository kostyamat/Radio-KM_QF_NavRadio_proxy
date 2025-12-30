<p align="center">
  <a href="https://www.paypal.com/paypalme/kostyamat">
    <img src="https://thumbs.dreamstime.com/b/cute-kawaii-coffee-mug-character-smiling-steam-isolated-white-adorable-cartoon-happy-face-decorative-lace-pattern-401912575.jpg" alt="Buy me a coffee" width="200"/>
    <br>
    <strong>If you found my work helpful, buy me a coffee! It keeps me motivated ☕</strong>
  </a>
</p>



[English](#english) | [Українська](#українська)

---

<a name="english"></a>

# NavRadio Proxy (Radio-KM)

This application is a proxy that completely replaces the limited, stock FM Radio app on **QF-based custom Android head units** with the far superior **NavRadio+** (or NavRadio+free).

While its primary goal is this replacement, it also fixes two long-standing, annoying bugs that the original NavRadio+ developer has ignored and which my old Magisk module could not solve:

*   The **system media widget** now correctly displays information from NavRadio+.
*   The physical **"Mode" button** on the head unit now correctly cycles to the radio app.

> **Important:** This application is a complete replacement for my obsolete Magisk module `QF_Player_proxy_module_1.2`. You **must** uninstall the old module via Magisk before installing this application to avoid potential conflicts.

## Developer's Guide: How the Hacks Work

This section explains the core mechanisms used to integrate a third-party radio app into a system that is hard-coded for a stock app.

### 1. "Mode" Button Integration (The "Cloaking" Hack)

**Goal:** Intercept the launch from the "Mode" button cycle, launch NavRadio+, but trick the system into thinking our proxy is still in the foreground.

**Mechanism:**

1.  **Launch Detection:** The system launches `FmMainActivity`. We detect that the launch came specifically from the "Mode" button cycle by checking the intent's referrer. The key is the `getReferrer()` method, which must match the value of `CAROUSEL_REFERRER` (a constant for `"com.qf.framework"`).

2.  **Launch NavRadio+:** The proxy immediately finds and starts the NavRadio+ activity.

3.  **Applying the "Cloak":** Instead of closing, `FmMainActivity` calls the `applyCloak()` function. This function schedules a delayed task (350ms) that re-launches `FmMainActivity` itself with the `FLAG_ACTIVITY_REORDER_TO_FRONT` flag. Since the activity has a `@android:style/Theme.Translucent.NoTitleBar` theme, it becomes an invisible window positioned **on top** of the now-visible NavRadio+ app.

4.  **Swallowing the First Touch:** This invisible "cloak" activity now receives all touch events. The `onTouchEvent` is overridden to listen for `MotionEvent.ACTION_DOWN`. Upon the first touch, it calls `uncloakAndFinish()` (which closes the invisible activity) and returns `true`. Returning `true` consumes the event, preventing it from passing down to the NavRadio+ activity underneath. This is the "swallowed" click.

5.  **Cleanup:** A `BroadcastReceiver` (`finishProxyReceiver`) listens for the system action `android.intent.action.MODE_SWITCH`. This ensures that if the user presses the "Mode" button again to cycle away from the radio, the invisible cloak activity is properly closed.

### 2. System Widget Fix (The "Voice Command" Hack)

**Goal:** Force the system media widget to update with information from NavRadio+ when the user returns to the home screen.

**Mechanism:**

1.  **Trigger:** A persistent foreground service, `WidgetWatcherService`, listens for the `Intent.ACTION_CLOSE_SYSTEM_DIALOGS` broadcast, which is reliably triggered when the user presses the Home button.

2.  **Audio Source Detection:** Upon trigger, `checkAndFixWidget()` is called. Crucially, it does **not** use standard Android APIs. Instead, it reads a non-standard, QF-platform-specific system property: `sys.qf.last_audio_src`.

3.  **Verification:** The app checks if the value of this property contains the package name for NavRadio (`com.navimods.radio`). This is a 100% reliable way on this hardware to confirm that NavRadio+ was the last active audio source.

4.  **The Hack:** If verified, the service starts `FmMainActivity` with the custom action `ACTION_PERFORM_HACK`. This, in turn, calls `performWidgetHack()`.

5.  **Forcing the Update:** `performWidgetHack()` executes the core of the hack: it creates and starts an `Intent.ACTION_VOICE_COMMAND` aimed at a dummy activity (`PseudoVoiceActivity`). This system-level intent forces the platform's media manager to re-evaluate and re-query the current active media session. It fetches the latest metadata from NavRadio+ and pushes it to all system listeners, including the media widget. `PseudoVoiceActivity` does nothing and closes instantly.

---

## User Guide

### Installation Requirements (Crucial!)

To install this app as a system update and successfully bypass signature verification, you **MUST** meet the following requirements:

*   **Root Access** is required.
*   **Magisk** must be installed.
*   **Zygisk** must be enabled in your Magisk settings.
*   You must install the **PMPatch** Magisk Module to disable Android's signature verification check.
    *   [Download PMPatch v1.2.0](https://github.com/vova7878-modules/PMPatch/releases/download/v1.2.0/PMPatch3.zip)

*Failure to install PMPatch will prevent you from installing this application as a system update.*

### Installation Steps

1.  Install the provided APK file as a simple update for the system's original FM Radio app.
2.  If Google Play Protect prompts you with a warning, please allow it to proceed or select "Install anyway."
3.  The application will automatically detect if you have `NavRadio+` or `NavRadio+free` installed. No manual selection is needed! If both are present, the paid `NavRadio+` version will always be prioritized.

### Uninstallation

Since the system now recognizes this app as a core system component, you cannot uninstall it directly. You have two options for removal:

#### Option 1: Using ADB (Recommended)

1.  Connect your device to your computer (e.g., via WiFi ADB).
2.  Run the following command in your terminal:
    ```
    adb uninstall com.android.fmradio.ext
    ```
3.  Reboot your head unit.

#### Option 2: Via App Settings

1.  Go to `Settings -> Apps` (or `Applications`).
2.  Open the list of all apps.
3.  Tap the three dots (overflow menu) in the upper right corner -> Select "Show System."
4.  Find **Radio-KM** in the list.
5.  Tap the three dots again -> Select "Uninstall updates."


[Go to top](#)

---

<a name="українська"></a>

# NavRadio Proxy (Radio-KM)

Цей застосунок є проксі, що повністю замінює обмежене, стокове FM-радіо на вашому **кастомному головному пристрої на базі платформи QF** на значно кращий **NavRadio+** (або NavRadio+free).

Хоча його основна мета — ця заміна, він також виправляє дві давні, надокучливі помилки, які розробник NavRadio+ ігнорував, і які мій старий модуль для Magisk вирішити не міг:

*   **Системний медіа-віджет** тепер коректно відображає інформацію з NavRadio+.
*   Фізична **кнопка "Mode"** на головному пристрої тепер коректно перемикається на радіо.

> **Важливо:** Цей застосунок є повною заміною мого застарілого модуля Magisk `QF_Player_proxy_module_1.2`. Ви **повинні** видалити старий модуль через Magisk перед встановленням цього додатка, щоб уникнути потенційних конфліктів.

## Посібник для Розробника: Як Працюють Хаки

Цей розділ пояснює ключові механізми, використані для інтеграції стороннього радіо-додатка в систему, яка жорстко запрограмована для роботи зі стоковим.

### 1. Інтеграція Кнопки "Mode" (Хак "Плащ")

**Мета:** Перехопити запуск із циклу перемикання додатків по кнопці "Mode", запустити NavRadio+, але обдурити систему, щоб вона думала, що наш проксі все ще на передньому плані.

**Механізм:**

1.  **Виявлення Запуску:** Система запускає `FmMainActivity`. Ми виявляємо, що запуск відбувся саме з циклу кнопки "Mode", перевіряючи реферер інтенту. Ключовим є метод `getReferrer()`, який має повернути значення, що відповідає константі `CAROUSEL_REFERRER` (що є `"com.qf.framework"`).

2.  **Запуск NavRadio+:** Проксі негайно знаходить і запускає активність NavRadio+.

3.  **Застосування "Плаща":** Замість закриття, `FmMainActivity` викликає функцію `applyCloak()`. Ця функція планує відкладене завдання (на 350 мс), яке перезапускає `FmMainActivity` з прапором `FLAG_ACTIVITY_REORDER_TO_FRONT`. Оскільки активність має тему `@android:style/Theme.Translucent.NoTitleBar`, вона стає невидимим вікном, що розташовується **поверх** щойно запущеного NavRadio+.

4.  **Поглинання Першого Дотику:** Це невидиме вікно-"плащ" тепер отримує всі події дотику. Метод `onTouchEvent` перевизначено для прослуховування `MotionEvent.ACTION_DOWN`. При першому дотику він викликає `uncloakAndFinish()` (що закриває невидиму активність) і повертає `true`. Повернення `true` споживає подію, не даючи їй дійти до активності NavRadio+ під нею. Це і є той самий "проковтнутий" клік.

5.  **Очищення:** `BroadcastReceiver` (`finishProxyReceiver`) прослуховує системну дію `android.intent.action.MODE_SWITCH`. Це гарантує, що якщо користувач знову натисне кнопку "Mode", щоб перейти до іншого додатка, невидимий плащ буде коректно закрито.

### 2. Виправлення Системного Віджета (Хак "Голосова Команда")

**Мета:** Змусити системний медіа-віджет оновитися з інформацією від NavRadio+, коли користувач повертається на головний екран.

**Механізм:**

1.  **Тригер:** Постійний сервіс, `WidgetWatcherService`, прослуховує системну подію `Intent.ACTION_CLOSE_SYSTEM_DIALOGS`, яка надійно спрацьовує при натисканні кнопки "Додому".

2.  **Визначення Джерела Аудіо:** Після спрацьовування тригера викликається `checkAndFixWidget()`. Важливо, що він **не використовує** стандартні Android API. Замість цього він зчитує нестандартну, специфічну для платформи QF системну властивість (property): `sys.qf.last_audio_src`.

3.  **Перевірка:** Додаток перевіряє, чи містить значення цієї властивості назву пакета NavRadio (`com.navimods.radio`). На цьому залізі це 100% надійний спосіб підтвердити, що NavRadio+ був останнім активним джерелом аудіо.

4.  **Хак:** Якщо перевірка успішна, сервіс запускає `FmMainActivity` з кастомною дією `ACTION_PERFORM_HACK`. Це, у свою чергу, викликає `performWidgetHack()`.

5.  **Примусове Оновлення:** `performWidgetHack()` виконує ядро хаку: створює та запускає інтент `Intent.ACTION_VOICE_COMMAND`, націлений на фіктивну активність `PseudoVoiceActivity`. Цей інтент системного рівня змушує медіа-менеджер платформи переоцінити та перезапитати поточну активну медіа-сесію. Він отримує свіжі метадані від NavRadio+ і передає їх усім системним слухачам, включно з медіа-віджетом. `PseudoVoiceActivity` нічого не робить і миттєво закривається.

---

## Інструкція для Користувача

### Вимоги для Встановлення (Важливо!)

Щоб встановити цей додаток як системне оновлення та успішно обійти перевірку підпису, ви **ПОВИННІ** відповідати таким вимогам:

*   Потрібен **Root-доступ**.
*   Має бути встановлений **Magisk**.
*   **Zygisk** має бути увімкнений у налаштуваннях Magisk.
*   Ви повинні встановити модуль Magisk **PMPatch**, щоб вимкнути системну перевірку підписів:
    *   [Завантажити PMPatch v1.2.0](https://github.com/vova7878-modules/PMPatch/releases/download/v1.2.0/PMPatch3.zip)

*Без встановлення PMPatch ви не зможете встановити цей додаток як системне оновлення.*

### Кроки Встановлення

1.  Встановіть наданий APK-файл як просте оновлення для оригінального системного FM-радіо.
2.  Якщо Google Play Protect покаже попередження, дозвольте йому продовжити або виберіть "Все одно встановити".
3.  Додаток автоматично визначить, чи встановлено у вас `NavRadio+` або `NavRadio+free`. Ручний вибір не потрібен! Якщо встановлено обидві версії, пріоритет завжди буде у платної версії `NavRadio+`.

### Видалення

Оскільки система тепер розпізнає цей додаток як основний системний компонент, ви не можете видалити його безпосередньо. Є два варіанти видалення:

#### Варіант 1: Через ADB (Рекомендовано)

1.  Підключіть пристрій до комп'ютера (напр., через WiFi ADB).
2.  Виконайте в терміналі таку команду:
    ```
    adb uninstall com.android.fmradio.ext
    ```
3.  Перезавантажте головний пристрій.

#### Варіант 2: Через Налаштування Додатків

1.  Перейдіть до `Налаштування -> Програми` (або `Додатки`).
2.  Відкрийте список усіх програм.
3.  Натисніть на три крапки (меню) у верхньому правому куті -> виберіть "Показати системні".
4.  Знайдіть у списку **Radio-KM**.
5.  Знову натисніть на три крапки -> виберіть "Видалити оновлення".

[На початок](#)
