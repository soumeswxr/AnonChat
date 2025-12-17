# AnonChat

AnonChat is an experimental Android messaging app that allows **anonymous, peer-to-peer chat over Tor onion services**, inspired by projects like **Ricochet Refresh**.

There are **no accounts, no phone numbers, no servers**.  
Each user is identified only by their **.onion address**.

## > ⚠️ This project is under active development and is **not production-ready**.

---

## 🧅 Core Idea

- Every user runs their **own Tor onion service**
- Chats happen **directly between onion services**
- No central server, no metadata collection
- Designed for privacy, anonymity, and simplicity

---

## ✨ Features (Planned / In Progress)

- [x] Tor integration using `guardianproject/tor-android`
- [x] Onion identity generation
- [x] One-to-one chats (onion ↔ onion)
- [x] Minimal data layer (Chat, Message, Peer)
- [ ] Onion client/server message transport
- [ ] Basic message protocol (JSON)
- [ ] End-to-end encryption (planned)
- [ ] Persistence (Room DB)
- [ ] Clean Jetpack Compose UI

---
## 🛠 Tech Stack

```
Language: Kotlin

UI: Jetpack Compose

Tor: guardianproject/tor-android

Architecture: MVVM (planned)

Networking: Tor onion services (no clearnet servers)
```

## 🚧 Current Status

This project is in an early experimental stage.

Right now the focus is on:

1. Correct architecture

2. Safe Tor integration

3. Clean separation of concerns

Do not rely on this app for real anonymity yet.

## 🧩 Inspiration

Ricochet / Ricochet Refresh

Tor Project


## 📜 Disclaimer

This project is for educational and research purposes.

The author is not responsible for misuse, legal issues, or security assumptions made by users.

## 🤝 Contributions

This is currently a personal learning project.
Contributions, ideas, and reviews are welcome once the core architecture stabilizes.
