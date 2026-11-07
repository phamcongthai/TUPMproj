"""Build a self-contained, offline HTML reading edition of README.md."""
import base64
import importlib.metadata
import json
import mimetypes
from pathlib import Path
import re
import subprocess
import sys
from datetime import datetime, timezone

import markdown

HERE = Path(__file__).resolve().parent
source = (HERE / "README.md").read_text(encoding="utf-8-sig")
body = markdown.markdown(source, extensions=["tables", "fenced_code", "toc", "sane_lists"])


def embed_image(match):
    path = (HERE / match.group(1)).resolve()
    if not path.is_relative_to(HERE) or not path.is_file():
        raise ValueError(f"Missing or unsafe local image: {path}")
    mime = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    data = base64.b64encode(path.read_bytes()).decode("ascii")
    return f'src="data:{mime};base64,{data}"'


body = re.sub(r'src="(assets/[^\"]+)"', embed_image, body)
style = """
body{font:17px/1.75 'Segoe UI',Arial,sans-serif;color:#1f2937;background:#f2f5f9;margin:0}
main{max-width:1000px;margin:32px auto;background:white;padding:46px 56px;border-radius:12px}
h1{font-size:2em;line-height:1.3;color:#133b5c}h2{margin-top:2.3em;color:#133b5c;border-top:1px solid #dbe3ed;padding-top:22px}
h3{color:#176e73;margin-top:1.6em}a{color:#155db0}strong{color:#152f48}
img{max-width:100%;height:auto;display:block;margin:24px auto;border:1px solid #e3e8ee}
table{border-collapse:collapse;width:100%;font-size:.9em;margin:20px 0}th,td{border:1px solid #d9e1eb;padding:9px 12px;text-align:left;vertical-align:top}th{background:#eff5fb}
code{font-family:Consolas,monospace;font-size:.88em;background:#eef2f7;padding:2px 4px;overflow-wrap:anywhere}
pre{background:#eff3f8;padding:18px;overflow-x:auto;border-left:4px solid #176e73}pre code{padding:0;background:none;overflow-wrap:normal}
li{margin:.5em 0}.note{padding:14px;background:#e9f5ef;border-radius:6px}
@media(max-width:700px){main{margin:0;padding:20px}body{font-size:16px}table{display:block;overflow:auto}}
@media print{body{background:white;font-size:11pt}main{max-width:none;margin:0;padding:0}h2,h3{break-after:avoid}img,table,pre{break-inside:avoid}a{color:inherit}pre{white-space:pre-wrap}}
"""
html = f'''<!doctype html><html lang="vi"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Nghiên cứu và thực nghiệm tối ưu hiệu năng khởi động ứng dụng Android bằng Baseline Profiles và Startup Profiles trên ứng dụng đặt đồ ăn</title><style>{style}</style></head>
<body><main><p class="note">Bản đọc offline — ảnh đã nhúng. Mã vẽ sơ đồ và dữ liệu gốc có trong gói ZIP đi kèm.</p>{body}</main></body></html>'''
(HERE / "README.html").write_text(html, encoding="utf-8")
dot_version = subprocess.run(["dot", "-V"], capture_output=True, text=True, check=True)
metadata = {
    "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
    "python": sys.version,
    "graphvizPython": importlib.metadata.version("graphviz"),
    "markdown": importlib.metadata.version("markdown"),
    "dot": (dot_version.stdout + dot_version.stderr).strip(),
    "diagramLibrary": "https://github.com/mingrammer/diagrams",
    "diagrams": {"version": importlib.metadata.version("diagrams"), "count": 8,
                 "implementation": "build_diagrams.py imports Diagram, Cluster, Edge, Node directly"},
    "charts": "System.Windows.Forms.DataVisualization.Charting from raw Macrobenchmark JSON",
}
(HERE / "generation.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Built offline HTML: {HERE / 'README.html'}")
