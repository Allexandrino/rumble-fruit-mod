#!/usr/bin/env python3
"""RCON-клиент с сопоставлением ответов по request id."""
import socket, struct, sys, time

HOST, PORT, PASSWORD = "127.0.0.1", 25575, "test123"

class Rcon:
    def __init__(self):
        self.sock = socket.create_connection((HOST, PORT), timeout=15)
        self.sock.settimeout(15)
        self.req_id = 0
        self.pending = {}

    def _recv_exact(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("closed")
            buf += chunk
        return buf

    def _read_frame(self):
        hdr = self._recv_exact(4)
        (size,) = struct.unpack("<i", hdr)
        data = self._recv_exact(size)
        rid, rtype = struct.unpack("<ii", data[:8])
        return rid, data[8:-2].decode("utf-8", "replace")

    def _send_frame(self, rtype, payload):
        self.req_id += 1
        body = struct.pack("<ii", self.req_id, rtype) + payload.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(body)) + body)
        return self.req_id

    def login(self):
        rid = self._send_frame(3, PASSWORD)
        got, _ = self._read_frame()
        if got == -1:
            raise PermissionError("RCON auth failed")

    def cmd(self, c):
        rid = self._send_frame(2, c)
        if rid in self.pending:
            return self.pending.pop(rid)
        while True:
            got, text = self._read_frame()
            if got == rid:
                return text
            self.pending[got] = text  # чужой ответ — отложим

if __name__ == "__main__":
    r = Rcon()
    r.login()
    for c in sys.argv[1:]:
        print(f">>> {c}")
        try:
            print(r.cmd(c) or "(пусто)")
        except Exception as e:
            print(f"ОШИБКА: {e}")
        time.sleep(0.2)
