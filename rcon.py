#!/usr/bin/env python3
"""Минимальный RCON-клиент для теста сервера."""
import socket, struct, sys, time

HOST, PORT, PASSWORD = "127.0.0.1", 25575, "test123"

class Rcon:
    def __init__(self):
        self.sock = socket.create_connection((HOST, PORT), timeout=10)
        self.req_id = 0

    def _send(self, rtype, payload):
        self.req_id += 1
        body = struct.pack("<ii", self.req_id, rtype) + payload.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(body)) + body)
        # читаем ответ
        hdr = self._recv_exact(4)
        (size,) = struct.unpack("<i", hdr)
        data = self._recv_exact(size)
        rid, _ = struct.unpack("<ii", data[:8])
        result = data[8:-2].decode("utf-8", "replace")
        # ванильный RCON иногда шлёт пустые добивки — сливаем их,
        # иначе ответы сдвигаются и приходят к следующей команде
        self.sock.settimeout(0.15)
        try:
            while True:
                hdr = self.sock.recv(4)
                if len(hdr) < 4:
                    break
                (sz,) = struct.unpack("<i", hdr)
                self._recv_exact(sz)
        except (socket.timeout, ConnectionError):
            pass
        finally:
            self.sock.settimeout(10)
        return result

    def _recv_exact(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("closed")
            buf += chunk
        return buf

    def login(self):
        return self._send(3, PASSWORD)

    def cmd(self, c):
        return self._send(2, c)

if __name__ == "__main__":
    r = Rcon()
    r.login()
    for c in sys.argv[1:]:
        print(f">>> {c}")
        try:
            print(r.cmd(c) or "(пусто)")
        except Exception as e:
            print(f"ОШИБКА: {e}")
        time.sleep(1)
