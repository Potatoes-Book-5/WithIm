#!/usr/bin/env python3
"""Combine the wireframe SVGs into docs/design/wireframes/wireframes.pdf.

Each SVG becomes one page, in SCR order, with the SCR ID, screen name and
variant printed under it. Headless Chrome renders the pages so Korean text
uses the system Apple SD Gothic Neo font, which is embedded in the PDF.

Usage: python3 harness/build-wireframes-pdf.py [--chrome PATH] [--out PATH]
"""
from __future__ import annotations

import argparse
import datetime as dt
import html
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WF = ROOT / "docs/design/wireframes"
APP_NAME = "WithIm"
APP_NAME_KO = "임과 함께"

# (file, SCR ID, screen name, variant, orientation · state)
PAGES = [
    ("SCR-001_landscape_parked.svg", "SCR-001", "홈", "", "landscape · parked"),
    ("SCR-002_landscape_parked.svg", "SCR-002", "아파트 상세", "", "landscape · parked"),
    ("SCR-003_landscape_parked.svg", "SCR-003", "관심 조건 설정", "", "landscape · parked"),
    ("SCR-004_portrait_driving.svg", "SCR-004", "주행 중 순위 경쟁", "", "portrait · driving"),
    ("SCR-004_landscape_driving.svg", "SCR-004", "주행 중 순위 경쟁", "", "landscape · driving"),
    ("SCR-005_portrait_driving.svg", "SCR-005", "알림 표시", "관심 매물 알림", "portrait · driving"),
    ("SCR-005_landscape_driving.svg", "SCR-005", "알림 표시", "대장 교체 알림", "landscape · driving"),
    ("SCR-005_landscape_driving_interest.svg", "SCR-005", "알림 표시", "관심 매물 알림", "landscape · driving"),
    ("SCR-005_landscape_driving_interest_scrapped.svg", "SCR-005", "알림 표시", "관심 매물 스크랩 완료", "landscape · driving"),
    ("SCR-006_landscape_parked.svg", "SCR-006", "동의와 설정", "", "landscape · parked"),
    ("SCR-007_landscape_parked.svg", "SCR-007", "스크랩 목록", "", "landscape · parked"),
    ("SCR-008_landscape_parked.svg", "SCR-008", "주행 요약", "", "landscape · parked"),
    ("SCR-009_portrait_driving.svg", "SCR-009", "주행 중 지도 모드", "", "portrait · driving"),
    ("SCR-009_landscape_driving.svg", "SCR-009", "주행 중 지도 모드", "", "landscape · driving"),
    ("SCR-009_portrait_driving_fallback.svg", "SCR-009", "주행 중 지도 모드", "대체안(레이더)", "portrait · driving"),
    ("SCR-010_landscape_parked.svg", "SCR-010", "마이 페이지", "", "landscape · parked"),
    ("SCR-011_landscape_parked.svg", "SCR-011", "주차 후 추천 매물 모아보기", "", "landscape · parked"),
]

CHROME_CANDIDATES = [
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/Applications/Chromium.app/Contents/MacOS/Chromium",
    "google-chrome",
    "chromium",
]

FONT = "'Apple SD Gothic Neo', 'Noto Sans KR', 'Pretendard', sans-serif"

CSS = f"""
@page {{ size: 2560px 1600px; margin: 0; }}
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
html, body {{ background: #fff; font-family: {FONT}; color: #14171C; }}
.page {{ width: 2560px; height: 1600px; position: relative; overflow: hidden;
        page-break-after: always; break-after: page; }}
.page:last-child {{ page-break-after: auto; break-after: auto; }}
.shot {{ display: block; width: 2560px; height: 1440px; }}
.foot {{ position: absolute; left: 0; right: 0; bottom: 0; height: 160px;
        border-top: 2px solid #D5D9E0; display: flex; align-items: center;
        padding: 0 72px; gap: 36px; font-size: 32px; color: #5B6270; }}
.foot .id {{ font-size: 46px; font-weight: 800; color: #14171C; }}
.foot .variant {{ font-size: 36px; font-weight: 700; color: #2F5BEA; }}
.foot .file {{ font-size: 28px; color: #8A909C; }}
.foot .no {{ margin-left: auto; font-size: 32px; color: #8A909C; }}
.cover {{ padding: 120px 140px 0; }}
.kicker {{ font-size: 40px; font-weight: 700; color: #2F5BEA; }}
.title {{ font-size: 132px; font-weight: 800; letter-spacing: -2px; margin-top: 16px; line-height: 1.1; }}
.title .ko {{ font-size: 64px; font-weight: 600; color: #5B6270; margin-left: 28px; letter-spacing: 0; }}
.meta {{ font-size: 32px; color: #5B6270; margin-top: 24px; }}
.cols {{ display: flex; gap: 120px; margin-top: 72px; }}
table {{ border-collapse: collapse; width: 1440px; font-size: 30px; }}
td {{ padding: 9px 16px; border-bottom: 1px solid #E1E4E9; }}
td.no {{ width: 80px; color: #8A909C; }}
td.id {{ width: 190px; font-weight: 700; }}
td.mode {{ width: 360px; color: #5B6270; }}
td .variant {{ color: #2F5BEA; font-weight: 600; }}
.note {{ font-size: 30px; line-height: 1.7; color: #5B6270; width: 740px; }}
.note b {{ color: #14171C; }}
"""


def find_chrome(explicit: str | None) -> str:
    for c in [explicit] if explicit else CHROME_CANDIDATES:
        if c and (Path(c).exists() or shutil.which(c)):
            return c
    sys.exit("Chrome not found. Pass --chrome PATH.")


def label(name: str, variant: str) -> str:
    return f"{name} · {variant}" if variant else name


def build_html(tmp: Path, today: str) -> str:
    rows = []
    for i, (_, scr, name, variant, mode) in enumerate(PAGES, start=2):
        v = f' <span class="variant">· {html.escape(variant)}</span>' if variant else ""
        rows.append(
            f'<tr><td class="no">{i}</td><td class="id">{scr}</td>'
            f"<td>{html.escape(name)}{v}</td><td class=\"mode\">{mode}</td></tr>"
        )
    total = len(PAGES) + 1
    cover = f"""
<section class="page"><div class="cover">
  <div class="kicker">Pleos Connect 차량 앱</div>
  <div class="title">{APP_NAME} 와이어프레임<span class="ko">{APP_NAME_KO}</span></div>
  <div class="meta">2560×1440 · Connect-L 12.9인치 기준 · {today} · 화면 {len(PAGES)}개</div>
  <div class="cols">
    <table>{''.join(rows)}</table>
    <div class="note">
      <b>{APP_NAME}</b>({APP_NAME_KO})는 차량 위치 주변 아파트 실거래가 순위를 보여 주고,
      관심 조건에 맞는 매물이 가까우면 음성으로 알려 주는 Pleos Connect 앱입니다.<br><br>
      회색 영역(상태바, Driving View, 하단 독, 다른 앱 창)은 시스템 영역 참조용이고,
      디자인 대상은 흰색 앱 창입니다. 색상과 글꼴은 공식 토큰 확정 전의 임시 값입니다.<br><br>
      원본 SVG와 화면 명세는 docs/design/wireframes/와 docs/design/screens/에 있습니다.
    </div>
  </div>
</div>
<div class="foot"><span class="id">표지</span><span class="no">1 / {total}</span></div>
</section>"""
    pages = [cover]
    for i, (file, scr, name, variant, mode) in enumerate(PAGES, start=2):
        src = tmp / file
        shutil.copyfile(WF / file, src)
        v = f'<span class="variant">{html.escape(variant)}</span>' if variant else ""
        pages.append(
            f'<section class="page"><img class="shot" src="{file}" alt="{scr}">'
            f'<div class="foot"><span class="id">{scr} {html.escape(name)}</span>{v}'
            f"<span>{mode}</span><span class=\"file\">{file}</span>"
            f'<span class="no">{i} / {total}</span></div></section>'
        )
    return (
        '<!doctype html><html lang="ko"><head><meta charset="utf-8">'
        f"<title>{APP_NAME} 와이어프레임</title><style>{CSS}</style></head>"
        f"<body>{''.join(pages)}</body></html>"
    )


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--chrome")
    ap.add_argument("--out", default=str(WF / "wireframes.pdf"))
    ap.add_argument("--date", default=dt.date.today().isoformat())
    args = ap.parse_args()

    listed = {p[0] for p in PAGES}
    present = {p.name for p in WF.glob("SCR-*.svg")}
    if listed != present:
        sys.exit(
            f"PAGES is out of date. Missing: {sorted(present - listed)}; "
            f"unknown: {sorted(listed - present)}"
        )

    chrome = find_chrome(args.chrome)
    with tempfile.TemporaryDirectory() as d:
        tmp = Path(d)
        page = tmp / "wireframes.html"
        page.write_text(build_html(tmp, args.date), encoding="utf-8")
        subprocess.run(
            [
                chrome, "--headless", "--disable-gpu", "--no-pdf-header-footer",
                "--allow-file-access-from-files", f"--user-data-dir={tmp / 'profile'}",
                f"--print-to-pdf={args.out}", page.as_uri(),
            ],
            check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=180,
        )
    print(f"Wrote {args.out} ({len(PAGES) + 1} pages)")


if __name__ == "__main__":
    main()
