#!/usr/bin/env python3
"""
Mengubah font WOFF (versi 1) menjadi TTF/OTF untuk Android, tanpa pustaka tambahan.

WOFF 1.0 hanyalah tabel-tabel font asli yang masing-masing dikompres zlib
(https://www.w3.org/TR/WOFF/), jadi isinya tidak berubah, hanya dibungkus ulang.

Dipakai untuk font Amiri (SIL OFL 1.1) yang dibundel di aplikasi TV:
  python tools/woff_to_ttf.py <masukan.woff> app/src/main/res/font/amiri_regular.ttf
Sumber WOFF: paket npm @fontsource/amiri (subset Arab).
"""
import struct
import sys
import zlib


def woff_to_sfnt(data: bytes) -> bytes:
    (signature, flavor, _length, num_tables, _reserved, _total_sfnt, _maj, _min,
     _meta_off, _meta_len, _meta_orig, _priv_off, _priv_len) = struct.unpack(">4sIIHHIHHIIIII", data[:44])
    if signature != b"wOFF":
        raise ValueError("bukan berkas WOFF versi 1")

    tables = []
    for i in range(num_tables):
        tag, offset, comp_len, orig_len, checksum = struct.unpack(">4sIIII", data[44 + i * 20: 64 + i * 20])
        raw = data[offset: offset + comp_len]
        table = zlib.decompress(raw) if comp_len < orig_len else raw
        if len(table) != orig_len:
            raise ValueError(f"panjang tabel {tag!r} tidak cocok")
        tables.append((tag, checksum, table))

    # Header sfnt: searchRange dkk. sesuai spesifikasi OpenType.
    entry_selector = max(0, num_tables.bit_length() - 1)
    search_range = (1 << entry_selector) * 16
    range_shift = num_tables * 16 - search_range
    out = bytearray(struct.pack(">IHHHH", flavor, num_tables, search_range, entry_selector, range_shift))

    offset = 12 + num_tables * 16
    directory = bytearray()
    body = bytearray()
    for tag, checksum, table in sorted(tables, key=lambda t: t[0]):
        directory += struct.pack(">4sIII", tag, checksum, offset + len(body), len(table))
        body += table
        body += b"\0" * ((4 - len(table) % 4) % 4)  # tiap tabel rata 4 byte
    return bytes(out + directory + body)


if __name__ == "__main__":
    src, dst = sys.argv[1], sys.argv[2]
    with open(src, "rb") as f:
        sfnt = woff_to_sfnt(f.read())
    with open(dst, "wb") as f:
        f.write(sfnt)
    print(f"{dst}: {len(sfnt)} byte")
