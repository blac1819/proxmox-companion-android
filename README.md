# Proxmox Companion for Android

A native Android management client and monitor for **Proxmox Virtual Environment (PVE)** clusters and standalone hypervisors. Built to provide homelab administrators and enterprise engineers with fast, responsive, and secure control over their virtual machines, LXC containers, nodes, and storage directly from an Android device.

---

## Features

### 1. Multi-Server & Cluster Management
- Configure and switch between multiple Proxmox VE hosts or clusters.
- **Dual Authentication Modes:**
  - **API Token Auth (Recommended):** Non-expiring tokens with Token ID and Secret UUID (`PVEAPIToken=USER@REALM!TOKENID=SECRET`).
  - **Ticket & Password Auth:** Full credential authentication with automatic CSRF prevention tokens and session cookie renewals.
- **Self-Signed SSL Support:** Automatic bypass switch for self-signed certificates standard on homelab PVE nodes, with custom `SSLSocketFactory` and `HostnameVerifier`.

### 2. Live Cluster & Node Health Dashboard
- Real-time node status indicators (Online / Offline).
- Graphical CPU load and core count meters.
- Physical memory usage and percentages.
- Root filesystem storage capacity and availability.
- Node uptime, system load average (1m, 5m, 15m), and PVE/kernel release versions.

### 3. Virtual Machines (QEMU) Management
- Full QEMU guest inventory with live state badges (`RUNNING`, `STOPPED`, `PAUSED`).
- Memory, vCPU allocation, and live I/O monitoring (Disk Read/Write rates, Network In/Out traffic).
- Hardware and virtualization configuration inspector (boot disks, sockets, memory, OS types).
- **Power Operations:**
  - **Start:** Power up virtual machines.
  - **Shutdown:** ACPI graceful guest shutdown.
  - **Stop:** Instant forced power-off (with confirmation dialog protection).
  - **Reboot:** Soft reboot.
  - **Reset:** Hard reset.
  - **Suspend / Resume:** State pausing and resumption.

### 4. LXC Containers Management
- Lightweight container inventory and real-time state.
- CPU utilization, RAM usage, swap space meters, and container rootfs storage gauges.
- Configuration and network interface inspector.
- Container power controls (Start, Shutdown, Stop, Reboot).

### 5. Storage Pool Overview
- Monitor ZFS pools, Directory, LVM, NFS, Ceph, and SMB storage volumes.
- Total, used, and available space gauges.
- Content types supported (`images`, `rootdir`, `vztmpl`, `iso`, `backup`).

### 6. Cluster Task & Operation Log Viewer
- Real-time audit log of all cluster operations (`qmstart`, `vzdump`, `migrate`, `vzstop`, etc.).
- Monospace terminal log viewer for inspecting stdout/stderr of any task UPID.

### 7. Embedded Web Console & noVNC
- Built-in WebView with hardware acceleration, viewport scaling, and SSL bypass.
- Direct access to the Proxmox Web UI and virtual machine/container noVNC display.

---

## Project Structure

```
prox/
├── build/                           # Intermediate build artifacts
├── dist/
│   └── ProxmoxCompanion.apk        # Final signed Android APK (80 KB)
├── libs/
│   └── json.jar                    # JSON library for unit test harness
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml     # Permissions, activity registrations, theme
│   │   ├── res/
│   │   │   ├── drawable/           # Custom vector icon, badges, buttons, meters
│   │   │   ├── layout/             # Responsive layouts for all activities and cards
│   │   │   └── values/             # Colors, dark theme, strings, styles
│   │   └── java/
│   │       └── com/antigravity/proxmox/
│   │           ├── MainActivity.java           # Main dashboard and tab navigator
│   │           ├── ServerEditActivity.java     # Server profile configuration
│   │           ├── VmDetailActivity.java       # QEMU VM detailed view & power actions
│   │           ├── LxcDetailActivity.java      # LXC container detailed view & actions
│   │           ├── ConsoleActivity.java        # Embedded noVNC and PVE Web UI
│   │           ├── TaskLogActivity.java        # Monospace cluster task log viewer
│   │           ├── api/
│   │           │   ├── ProxmoxClient.java      # Core Proxmox VE REST API client
│   │           │   ├── ProxmoxApiException.java# Custom API exception handler
│   │           │   └── SslUtils.java           # Trust-all SSL / Hostname verifier
│   │           ├── model/                      # Data models (Node, VM, LXC, Storage, Task)
│   │           ├── storage/ProfileManager.java # SharedPreferences profile persistence
│   │           └── util/                       # Formatting and UI utilities
│   └── test/
│       └── java/com/antigravity/proxmox/
│           └── ProxmoxClientTest.java          # Comprehensive unit & integration tests
├── build.sh                         # Toolchain compilation and signing script
├── debug.keystore                   # RSA 2048-bit debug signing keystore
├── mock_pve_server.py               # Mock PVE API server for automated integration tests
└── README.md
```

---

## Building the APK

The application builds cleanly using the native Android toolchain (`aapt2`, `d8`, `javac`, and `apksigner`):

```bash
bash build.sh
```

### Build Pipeline Steps:
1. Compiles XML layouts and drawables via `aapt2 compile`.
2. Generates `R.java` and links APK resources via `aapt2 link`.
3. Compiles Java source files with `javac` against Android API 34.
4. Dexes class files into Dalvik Executable `classes.dex` using Google `d8`.
5. Injects `classes.dex` into `base.apk` via `zip`.
6. Signs the package with APK Signature Scheme v2/v3 using `apksigner`.
7. Copies the resulting APK to `/storage/emulated/0/Download/ProxmoxCompanion.apk`.

---

## Testing

Run the automated integration and unit test suite against the local mock Proxmox server:

```bash
# 1. Start mock server in the background
python mock_pve_server.py &
MOCK_PID=$!

# 2. Compile and run test suite
javac -cp libs/json.jar -d build/test_classes $(find src/main/java src/test/java -name "*.java" | grep -v Activity | grep -v ProfileManager)
java -cp libs/json.jar:build/test_classes com.antigravity.proxmox.ProxmoxClientTest

# 3. Stop mock server
kill -9 $MOCK_PID
```

---

## Installing on Android

The compiled APK is available at:
- **Project Distribution:** `dist/ProxmoxCompanion.apk`
- **Device Downloads Folder:** `/storage/emulated/0/Download/ProxmoxCompanion.apk`

To install:
1. Open the **Files** / **Downloads** app on your Android device and tap `ProxmoxCompanion.apk` to install.
2. Alternatively, if Shizuku / ADB is connected:
   ```bash
   adb install -r dist/ProxmoxCompanion.apk
   ```
