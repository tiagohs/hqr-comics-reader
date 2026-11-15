#!/usr/bin/env python3
"""Dela Gothic One (OFL) cut to kana, punctuation and the JIS level-1 kanji, for the vertical title of manga works.

Usage: python3 scripts/fonts/subset_japanese.py   (needs `pip install fonttools`)
Writes shared/ui/src/commonMain/composeResources/font/dela_gothic_one_jp.ttf.
"""
import subprocess
import sys
import tempfile
import urllib.request
from pathlib import Path

SOURCE = 'https://raw.githubusercontent.com/google/fonts/main/ofl/delagothicone/DelaGothicOne-Regular.ttf'
OUT = Path(__file__).resolve().parents[2] / 'shared/ui/src/commonMain/composeResources/font/dela_gothic_one_jp.ttf'


def codepoints():
    cps = set(range(0x3000, 0x3100)) | set(range(0xFF00, 0xFFF0))
    for lead in range(0x88, 0x99):  # Shift_JIS lead bytes of JIS X 0208 level 1
        for trail in list(range(0x40, 0x7F)) + list(range(0x80, 0xFD)):
            try:
                cps.add(ord(bytes([lead, trail]).decode('shift_jis')))
            except UnicodeDecodeError:
                pass
    return sorted(cps)


def main():
    with tempfile.TemporaryDirectory() as tmp:
        src = Path(tmp) / 'full.ttf'
        urllib.request.urlretrieve(SOURCE, src)
        unicodes = ','.join(f'U+{c:04X}' for c in codepoints())
        subprocess.run([sys.executable, '-m', 'fontTools.subset', str(src), f'--unicodes={unicodes}', f'--output-file={OUT}'], check=True)
    print(OUT, OUT.stat().st_size)


if __name__ == '__main__':
    main()
