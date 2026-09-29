# 🔋 Akkumulátor-tervező | PENdroid 2026 📱

<div align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Nyelv-Java-007396?style=for-the-badge&logo=java&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Környezet-Android%20Studio-3DDC84?style=for-the-badge&logo=android-studio&logoColor=white" alt="Android Studio">
  <img src="https://img.shields.io/badge/Min%20SDK-Android%2011.0%20(API%2030)-blue?style=for-the-badge" alt="Min SDK">
</div>

<br>

> A **PENdroid középiskolás verseny** első online fordulójára készült Android alkalmazás. Célja, hogy egy hosszú nap előtt megbecsülje, kibírja-e a telefon akkumulátora töltés nélkül a tervezett használat mellett.

---

## 👥 A Csapat: `()=>{}`

Háromfős csapatunk a következő felosztásban dolgozott az alkalmazáson:

| Név | Szerepkör / Feladatok | GitHub Profil |
| :--- | :--- | :--- |
| **Babos Pál** | ... | @babospal(https://github.com/babospal) |
| **Buda Bálint** | ... | @budabalint(https://github.com/budabalint) |
| **Csehi Dániel Tamás** | ... | @csehidanieltamas(https://github.com/csehidanieltamas) |

---

## ✨ Funkciók

### 🎯 Kötelezően megvalósított funkciók (Alapkövetelmények)
- [x] **Paraméterek bekérése:** Jelenlegi töltöttség, működési idő, tevékenységek (videó, játék, zene, navigáció) és azok időtartama.
- [x] **Fogyasztás kalkuláció:** Tevékenységenkénti becsült fogyasztás (%/óra) alapú számítás.
- [x] **Tartalék kezelés:** Nap végére megőrzendő minimális akkumulátorszint figyelembevétele.
- [x] **Döntéstámogatás:** Jelzi, hogy a terv tartható-e. Ha nem, javaslatot tesz a tevékenységek csökkentésére.
- [x] **Hibakezelés:** Figyelmeztetés hiányos vagy logikátlan bemeneti paraméterek (pl. 100%-nál nagyobb töltöttség megadása) esetén.

### 🌟 Plusz pontot érő funkciók (Extra)
- [ ] 🔋 **Automatikus töltöttség-lekérdezés** a rendszerből (nem kell kézzel beírni a kezdő értéket).
- [ ] 📊 **Grafikus fogyasztási diagram** a vizuális átláthatóságért.
- [ ] 💡 **Energiatakarékos javaslatok** megjelenítése kritikus szint esetén.
- [ ] 🔔 **Értesítések (Notifications)** küldése, ha az akku a beállított tartalékhoz közelít.
- [ ] 🎬 **Egyedi animációk** a letisztult felhasználói élményért.
- [ ] 👤 **Használati profilok kezelése** (pl. "Munka nap", "Utazás", "Hétvége" profilok mentése).

---

## 📸 Képernyőfotók (Screenshots)

<div align="center">
  <!-- KÉPEK LINKJEI! -->
  <img src="https://via.placeholder.com/250x500.png?text=Kezdőképernyő" width="200" alt="Kezdőképernyő"> &nbsp;
  <img src="https://via.placeholder.com/250x500.png?text=Tevékenységek" width="200" alt="Tevékenységek hozzáadása"> &nbsp;
  <img src="https://via.placeholder.com/250x500.png?text=Eredmény+diagram" width="200" alt="Eredmény diagram">
</div>

---

## 🛠️ Technológiai háttér
* **Fejlesztőkörnyezet:** Android Studio
* **Programozási nyelv:** Java
* **Minimum SDK verzió:** API 30 (Android 11.0)
* **Készítés dátuma:** 2026. október

---

## 🚀 Futtatás és Telepítés

1. Klónozd a repót a gépedre:
   ```bash
   git clone [https://github.com/csehidanieltamas/PENdroid_Akkumulator_Mero.git](https://github.com/csehidanieltamas/PENdroid_Akkumulator_Mero.git)
