# CNT4007 P2P File Sharing Project

This project implements a BitTorrent-inspired peer-to-peer (P2P) file sharing protocol in Java. Each peer can connect to others, exchange handshake and bitfield messages, and will soon support the full protocol for file piece exchange.

---

## Table of Contents

- [Project Overview](#project-overview)
- [How to Build and Run](#how-to-build-and-run)
- [How the Protocol Works (So Far)](#how-the-protocol-works-so-far)
- [Team](#team)

---

## Project Overview

- **Language:** Java 17+
- **Goal:** Simulate a BitTorrent-like P2P file sharing system.
- **Current Status:** Handshake and bitfield message exchange between two peers is working.

---

## How to Build and Run

### 1. **Compile the Project**

Open a terminal in the project root and run:

`javac -d out src/peer/*.java`

This compiles all Java files in `src/peer/` and puts the `.class` files in the `out/` directory.

---

### 2. **Run Two Peers (Handshake + Bitfield Demo)**

Open two terminals. In the first terminal, start the server peer:

`java -cp out peer.PeerProcess server 1001 6008`

In the second terminal, start the client peer:

`java -cp out peer.PeerProcess client 1002 6008`

**Expected Output:**

- Both peers will print:
    - Confirmation of connection
    - The peer ID of the other side
    - Confirmation that a bitfield message was sent and received

Example: <br>
Listening as peer 1001 on port 6008 <br>
Connected to peer with ID: 1002 <br>
Sent bitfield message. <br>
Received bitfield message with length: 2


---

## How the Protocol Works (So Far)

- **Handshake:** Each peer sends a 32-byte handshake message on connect.
- **Bitfield:** After handshake, each peer sends a bitfield message indicating which pieces of the file it has.
- **Peer State:** Each peer tracks which pieces it owns (currently hardcoded for demo).

---

## Team

- Eric Brown
- Jevan Tenaglia
- Garret McClay

---

## Notes

- All code is in the `src/peer/` directory.
- Config files are in the `config/` directory (not yet used in this demo)
