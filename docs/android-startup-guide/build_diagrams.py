"""Render the teaching diagrams with mingrammer/diagrams, not a DOT fallback.

Run: python build_diagrams.py
Requires Graphviz's dot on PATH. All labels describe this repository or are
explicitly marked conceptual. No timing is invented in these diagrams.
"""
from pathlib import Path
from diagrams import Diagram, Cluster, Edge, Node

HERE = Path(__file__).resolve().parent
ASSETS = HERE / "assets"
ASSETS.mkdir(exist_ok=True)
GRAPH = {
    "fontname": "Arial", "fontsize": "19", "bgcolor": "#ffffff",
    "pad": "0.35", "nodesep": "0.35", "ranksep": "0.55",
    "splines": "spline", "dpi": "160", "labelloc": "t",
}
NODE = {
    "fontname": "Arial", "fontsize": "12", "shape": "box",
    "style": "rounded,filled", "fillcolor": "#eff6ff", "color": "#7b9cbf",
    "fontcolor": "#172b4d", "fixedsize": "false", "width": "2.3",
    "height": "0.8", "margin": "0.15,0.12", "labelloc": "c",
}
EDGE = {"fontname": "Arial", "fontsize": "10", "color": "#60758c"}


def diagram(name, title, direction="TB"):
    return Diagram(title, filename=str(ASSETS / name), show=False,
                   direction=direction, outformat=["png", "svg"],
                   graph_attr=GRAPH, node_attr=NODE, edge_attr=EDGE)


def box(text, color=None):
    attrs = dict(NODE)
    if color:
        attrs["fillcolor"] = color
    return Node(text, **attrs)


with diagram("01-build-run", "1. Từ mã nguồn đến ứng dụng chạy trên Android"):
    source = box("Mã Kotlin + thư viện\nUI Compose, ViewModel, Repository")
    compiler = box("Kotlin compiler\nBiên dịch mã Kotlin thành bytecode")
    dex = box("D8 / R8 trong Android build\nTạo DEX; R8 có thể tối ưu mã")
    apk = box("APK được đóng gói và ký\nDEX + tài nguyên + manifest + profile")
    runtime = box("Cài trên Android\nART thực thi mã của app", "#e8f5ed")
    source >> compiler >> dex >> apk >> runtime

with diagram("02-app-startup", "2. Luồng khởi động thực tế: MainActivity → Splash → Login"):
    tap = box("Người dùng bấm biểu tượng app")
    process = box("Android tạo process khi cold start\nKhởi tạo ứng dụng / thành phần liên quan")
    activity = box("MainActivity.onCreate()\nsetContent { AppNavigation() }")
    auth = box("Splash: tạo AuthViewModel qua Hilt\nAuthPreference + AuthRepository")
    token = box("checkAuth()\nĐọc access_token từ DataStore")
    login = box("Không có token\nĐiều hướng LoginScreen\nReportDrawn()", "#e8f5ed")
    account = box("Có token: getAccount() qua API\nThành công → Main; lỗi → Login", "#fff2d9")
    tap >> process >> activity >> auth >> token
    token >> Edge(label="chưa đăng nhập: kịch bản đo") >> login
    token >> Edge(label="ngoài phạm vi đo lần này", style="dashed") >> account

with diagram("03-before", "3. Trước sửa: các đối tượng được tạo sớm (đọc từ mã nguồn)"):
    with Cluster("Đường mở app khi chưa đăng nhập"):
        auth = box("Hilt tạo AuthViewModel")
        repository = box("AuthRepositoryImpl cần ApiService ngay", "#ffe8e5")
        api = box("Tạo Retrofit / OkHttp\nConverter và thành phần phụ thuộc", "#ffe8e5")
        preference = box("Tạo AuthPreference\nGson() được tạo ngay", "#ffe8e5")
        token = box("Đọc token: không có → LoginScreen")
        credential = box("CredentialManager.create(context)\nđược gọi khi vào màn hình", "#ffe8e5")
        auth >> repository >> api
        auth >> preference
        [api, preference] >> token >> credential
    note = box("Không có token vốn đã không gọi getAccount()\nTạo client mạng ≠ gửi HTTP request", "#fff2d9")
    credential >> Edge(style="dashed", label="lưu ý") >> note

with diagram("04-after", "4. Sau sửa: trì hoãn việc chưa cần, giữ nguyên nhánh xác thực"):
    with Cluster("Trước khi màn hình Login sẵn sàng"):
        auth = box("Hilt tạo AuthViewModel")
        lazy_api = box("AuthRepositoryImpl nhận Lazy<ApiService>\nChưa gọi get() → chưa cần tạo dịch vụ", "#e8f5ed")
        lazy_gson = box("AuthPreference giữ lazy { Gson() }\nĐọc token không cần Gson", "#e8f5ed")
        login = box("Không có token → LoginScreen\nGiữ lazy CredentialManager; ReportDrawn()", "#e8f5ed")
        auth >> [lazy_api, lazy_gson]
        [lazy_api, lazy_gson] >> login
    with Cluster("Lần dùng đầu tiên: chi phí được dời sang đây"):
        api = box("Gửi đăng nhập / có token\napiService.get() tạo dịch vụ rồi gọi API")
        gson = box("Đọc / ghi giỏ hàng\nGson được tạo khi thật sự cần")
        google = box("Bấm đăng nhập Google\nCredentialManager được tạo và sử dụng")
    lazy_api >> Edge(style="dashed", label="khi cần API") >> api
    lazy_gson >> Edge(style="dashed", label="khi cần JSON giỏ hàng") >> gson
    login >> Edge(style="dashed", label="khi bấm Google") >> google

with diagram("05-art", "5. ART: minh họa cơ chế, không phải mọi mã đều được AOT"):
    dex = box("Mã DEX đã nằm trong APK")
    with Cluster("Cơ chế runtime, mô hình giản lược"):
        interpreter = box("Interpreter\nThực thi mã chưa được biên dịch")
        jit = box("JIT\nBiên dịch khi chạy / thu thập hành vi")
        aot = box("AOT có hướng dẫn từ profile\nBiên dịch trước đường mã được chọn", "#e8f5ed")
    execute = box("CPU chạy mã tương ứng\nThời gian còn phụ thuộc I/O, vẽ UI, hệ thống")
    dex >> [interpreter, jit, aot]
    [interpreter, jit, aot] >> execute

with diagram("06-baseline", "6. Baseline Profile: quy trình thực sự đã triển khai"):
    collect = box("BaselineProfileGenerator\nMở app → đợi chữ đăng nhập\nThu đường chạy trên thiết bị")
    hrf = box("Quy tắc HRF được sinh từ thực thi\nbaseline-prof.txt (hành trình startup)")
    build = box("AGP / R8 xử lý profile cùng app\nRewrite quy tắc theo mã đã minify")
    apk = box("APK E có binary Baseline Profile\nassets/dexopt/baseline.prof")
    compilation = box("Macrobenchmark: Partial + Require\nÁp dụng profile trước lượt đo", "#e8f5ed")
    cold = box("Cold start → TTID / TTFD\nRequire giúp phát hiện profile không dùng được")
    collect >> hrf >> build >> apk >> compilation >> cold

with diagram("07-startup", "7. Startup Profile: áp dụng lúc build và kiểm chứng trên APK F"):
    capture = box("Generator: includeInStartupProfile=true\nHành trình mở app đến Login")
    startup = box("startup-prof.txt\nTập đường mã liên quan khởi động")
    r8 = box("R8 đọc Startup Profile\nHướng dẫn bố trí mã trong DEX\n(sơ đồ cơ chế)")
    f = box("APK F\nLazy + R8 + Baseline + Startup")
    evaluate = box("Kiểm tra metadata R8: startup=true\nĐối chiếu SHA-256 DEX với APK", "#e8f5ed")
    choice = box("Bật cả hai profile mặc định ở F\nĐo E/F riêng để đánh giá tốc độ", "#e8f5ed")
    capture >> startup >> r8 >> f >> evaluate >> choice

with diagram("08-evidence", "8. Kiểm chứng: không lấy số đẹp nhất, không đo thời gian build"):
    apks = box("APK A và E đã lưu + SHA-256\nDừng Gradle; không build giữa các khối")
    with Cluster("Thứ tự xác nhận ABBA; 5 cold starts mỗi khối"):
        a1 = box("A1: 5 lần\nEager; không R8; None")
        e1 = box("E1: 5 lần\nLazy + R8 + Baseline AOT", "#e8f5ed")
        e2 = box("E2: 5 lần\nCùng cấu hình E", "#e8f5ed")
        a2 = box("A2: 5 lần\nCùng cấu hình A")
        a1 >> e1 >> e2 >> a2
    raw = box("4 JSON gốc + instrumentation logs\nA: 10 giá trị; E: 10 giá trị / chỉ số")
    analysis = box("Export-StartupResults.ps1\nTrung vị + min/max + CSV + biểu đồ")
    limits = box("TTID giảm 14.00%; TTFD giảm 25.92%\nTác động tổng hợp trên máy ảo\nChưa chứng minh từng kỹ thuật riêng", "#fff2d9")
    apks >> a1
    a2 >> raw >> analysis >> limits

print(f"Rendered 8 diagrams to {ASSETS} with mingrammer/diagrams")
