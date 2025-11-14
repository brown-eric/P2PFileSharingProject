# CNT4007 P2P File Sharing Project

This project implements a BitTorrent-inspired peer-to-peer (P2P) file sharing protocol in Java. Each peer can connect to others, exchange handshake and bitfield messages, and supports the full protocol for file piece exchange and complete BitTorrent-like behavior.



---

## Table of Contents

- [Project Overview](#project-overview)
- [Features](#features)
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

- Handshake and bitfield exchange for initial peer connection abd file discovery
- Interest and not-interested messaging based on file piece possession
- Choking and unchoking with periodic preferred neighbor and optimistic unchoke selection parameters from config
- Piece request and piece message protocol for segmented file downloads
- Automatic random piece requesting and storage until full file acquisition
- Completion propagation and process shutdown when all peers are complete
- Each peer logs required protocol events: connects, choke/unchoke, interests, piece downloads, completions, and all message types
- Peer directories are managed for file input/output

---

## How to Build and Run

### **Compile the Project**

Open a terminal in the project root and run:

`javac -d out src/peer/*.java`

This compiles all Java files in `src/peer/` and puts the `.class` files in the `out/` directory.

[//]: # (###  **Required Config Files**)

[//]: # ()
[//]: # (- `Common.cfg` — Specifies global parameters &#40;# preferred neighbors, time intervals, file name, file size, piece size&#41;)

[//]: # (- `PeerInfo.cfg` — Specifies each peer's ID, host, port, and whether it starts with the file)



### Running Peers

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
- When a piece is received, it is stored and the next missing piece is requested.
- Peers request missing pieces, receive and store pieces, send HAVE messages for new pieces, and write pieces to disk.
- Completion status is propagated; when all peers are complete, all processes shut down.

---

## TODO

- Implement logging mechanics for all events: connection made/received, preferred neighbor changes, optimistic unchoke updates, choke/unchoke, HAVE/interested/not interested/PIECE messages, completion.
- Include timestamps on logs mentioned above.
- Thorough testing for larger files.
- Read project description docs for anything else.

---

## Team

- Eric Brown
- Jevan Tenaglia
- Garret McClay

---

## Notes

- All code is in the `src/peer/` directory.
- Config files are in the `config/` directory (not yet being used, DO NOT SUBMIT CONFIG)
