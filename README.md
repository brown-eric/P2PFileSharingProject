# CNT4007 P2P File Sharing Project

This project implements a BitTorrent-inspired peer-to-peer (P2P) file sharing protocol in Java. Each peer can connect to others, exchange handshake and bitfield messages, and will soon support the full protocol for file piece exchange.

---

## Table of Contents

- [Project Overview](#project-overview)
- [How to Build and Run](#how-to-build-and-run)
- [How the Protocol Works (So Far)](#how-it-works)
- [TODO / Future Improvements](#todo)
- [Team](#team)

---

## Project Overview

- **Language:** Java 17+
- **Goal:** Simulate a BitTorrent-like P2P file sharing system.

---

## Features

- Handshake and bitfield exchange for initial peer connection
- Interest and not-interested messaging based on file piece possession
- Choking and unchoking (preferred and optimistic neighbor selection)
- Piece request and piece message protocol for segmented file downloads
- Automatic piece requesting and storage until full file acquisition
- Basic console logging of protocol events (connections, requests, transfers)

---

## How to Build and Run

### 1. **Compile the Project**

Open a terminal in the project root and run:

`javac -d out src/peer/*.java`

This compiles all Java files in `src/peer/` and puts the `.class` files in the `out/` directory.

## Running Peers

Each peer runs in its own terminal window. The format is:
`java -cp out peer.PeerProcess <peerId> <port> <comma-separated-peer-IDs>`

**Example for 3 peers:**

Terminal 1:
`java -cp out peer.PeerProcess 1001 6008 1001,1002,1003`

Terminal 2:
`java -cp out peer.PeerProcess 1002 6009 1001,1002,1003`

Terminal 3:
`java -cp out peer.PeerProcess 1003 6010 1001,1002,1003`

---

## How it Works

- Peer 1001 starts with the full file, others with none. Peers connect and exchange bitfields.
- Each peer requests missing pieces from unchoked connections.
- When a piece is received, it is stored and the next missing piece is requested.
- UploadManager periodically updates preferred (based on download rate) and optimistic neighbors, sending choke/unchoke messages accordingly.
- Console logs show all protocol events and piece transfers.

---

## TODO

- Implement 'have' message for new piece propagation
- Support more advanced choke/unchoke logic and peer preference
- Piece verification and file assembly (write to disk)
- Persistent logging and error handling improvements

---

## Team

- Eric Brown
- Jevan Tenaglia
- Garret McClay

---

## Notes

- All code is in the `src/peer/` directory.
- Config files are in the `config/` directory (not yet being used)
