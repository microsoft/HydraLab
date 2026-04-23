#!/bin/bash
# Unified script to install WebDriverAgent on iOS devices
# Supports both iOS < 17 (DeveloperDiskImage) and iOS 17+ (CoreDevice/tunneld)
# Usage: ./install_wda.sh --udid <device_udid>

set -euo pipefail

# Configuration
UDID=""
TEAM_ID="3MT967VXY3"
WDA_PROJECT_PATH="${WDA_PROJECT_PATH:-$(find ~/.appium -name "WebDriverAgent.xcodeproj" -type d 2>/dev/null | head -1)}"
WDA_BUNDLE_ID="com.microsoft.wdar.xctrunner"
SCHEME="WebDriverAgentRunner"
DESTINATION_TIMEOUT=30

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "${BLUE}[STEP]${NC} $1"; }

usage() {
    echo "Usage: $0 --udid <device_udid>"
    echo ""
    echo "Installs WebDriverAgent on an iOS device (supports all iOS versions)."
    echo ""
    echo "Options:"
    echo "  --udid       Device UDID (required)"
    echo "  --team-id    Apple Developer Team ID (default: $TEAM_ID)"
    echo "  --help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --udid 00008030-0005743926A0802E"
    echo "  $0 --udid 00008030-0005743926A0802E --team-id ABCD1234EF"
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --udid)
            UDID="$2"
            shift 2
            ;;
        --team-id)
            TEAM_ID="$2"
            shift 2
            ;;
        --help)
            usage
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            ;;
    esac
done

# Validate required arguments
if [[ -z "$UDID" ]]; then
    log_error "Device UDID is required"
    usage
fi

# Validate WDA project path
if [[ -z "$WDA_PROJECT_PATH" ]] || [[ ! -d "$WDA_PROJECT_PATH" ]]; then
    log_error "WebDriverAgent.xcodeproj not found"
    log_info "Please install Appium XCUITest driver: appium driver install xcuitest"
    log_info "Or set WDA_PROJECT_PATH environment variable"
    exit 1
fi

WDA_DIR=$(dirname "$WDA_PROJECT_PATH")

# ──────────────────────────────────────────────
# Helper: Get iOS version from device
# ──────────────────────────────────────────────
get_ios_version() {
    local version
    version=$(python3 -m pymobiledevice3 lockdown info --udid "$UDID" 2>/dev/null \
        | python3 -c "import sys,json; print(json.load(sys.stdin).get('ProductVersion',''))" 2>/dev/null || echo "")
    echo "$version"
}

# ──────────────────────────────────────────────
# Helper: Get major iOS version number
# ──────────────────────────────────────────────
get_major_version() {
    local version="$1"
    echo "$version" | cut -d. -f1
}

# ──────────────────────────────────────────────
# Helper: Check if tunneld is running
# ──────────────────────────────────────────────
is_tunneld_running() {
    if pgrep -f "pymobiledevice3.*tunneld" > /dev/null 2>&1; then
        return 0
    fi
    # Also check for lockdown start-tunnel (iOS 17.4+)
    if pgrep -f "pymobiledevice3.*start-tunnel" > /dev/null 2>&1; then
        return 0
    fi
    return 1
}

# ──────────────────────────────────────────────
# Helper: Start tunneld (iOS 17+)
# ──────────────────────────────────────────────
start_tunneld() {
    if is_tunneld_running; then
        log_info "tunneld is already running"
        return 0
    fi

    log_step "Starting tunneld (requires sudo)..."
    log_info "tunneld creates a persistent tunnel for iOS 17+ developer services"
    sudo python3 -m pymobiledevice3 remote tunneld &
    local tunneld_pid=$!

    # Wait for tunneld to initialize
    log_info "Waiting for tunneld to initialize..."
    local retries=10
    while [[ $retries -gt 0 ]]; do
        sleep 2
        if is_tunneld_running; then
            log_info "tunneld started successfully (PID: $tunneld_pid)"
            return 0
        fi
        retries=$((retries - 1))
    done

    log_error "Failed to start tunneld"
    return 1
}

# ──────────────────────────────────────────────
# Step 1: Check device connection
# ──────────────────────────────────────────────
log_step "Checking device connection..."
if ! python3 -m pymobiledevice3 usbmux list 2>/dev/null | grep -q "$UDID"; then
    log_error "Device with UDID $UDID is not connected"
    exit 1
fi
log_info "Device found and connected"

# ──────────────────────────────────────────────
# Step 2: Get iOS version
# ──────────────────────────────────────────────
log_step "Detecting iOS version..."
IOS_VERSION=$(get_ios_version)
if [[ -z "$IOS_VERSION" ]]; then
    log_error "Could not detect iOS version for device $UDID"
    exit 1
fi

MAJOR_VERSION=$(get_major_version "$IOS_VERSION")
log_info "iOS version: $IOS_VERSION (major: $MAJOR_VERSION)"

# ──────────────────────────────────────────────
# Step 3: Version-specific prerequisites
# ──────────────────────────────────────────────
if [[ "$MAJOR_VERSION" -ge 17 ]]; then
    log_info "=== iOS 17+ path (CoreDevice / RemoteXPC) ==="

    # 3a. Ensure Developer Mode is enabled
    log_step "Verifying Developer Mode..."
    log_info "Developer Mode must be enabled on device:"
    log_info "  Settings → Privacy & Security → Developer Mode → ON"
    log_info "  (Requires device restart if just enabled)"

    # 3b. Start tunneld
    log_step "Ensuring tunneld is running..."
    start_tunneld

    # 3c. Auto-mount personalized Developer Disk Image
    log_step "Mounting Developer Disk Image (personalized for iOS 17+)..."
    if python3 -m pymobiledevice3 mounter auto-mount --udid "$UDID" 2>&1; then
        log_info "Developer Disk Image mounted successfully"
    else
        log_warn "DDI mount returned non-zero (may already be mounted)"
    fi

else
    log_info "=== iOS < 17 path (DeveloperDiskImage) ==="

    # 3a. Mount DeveloperDiskImage (legacy)
    log_step "Mounting DeveloperDiskImage..."
    if python3 -m pymobiledevice3 mounter auto-mount --udid "$UDID" 2>&1; then
        log_info "DeveloperDiskImage mounted successfully"
    else
        log_warn "DeveloperDiskImage mount returned non-zero (may already be mounted)"
    fi
fi

# ──────────────────────────────────────────────
# Step 4: Build and install WDA (common for all versions)
# ──────────────────────────────────────────────
log_info ""
log_info "Configuration:"
log_info "  Device UDID:  $UDID"
log_info "  iOS Version:  $IOS_VERSION"
log_info "  Team ID:      $TEAM_ID"
log_info "  WDA Project:  $WDA_PROJECT_PATH"
log_info "  Bundle ID:    $WDA_BUNDLE_ID"
log_info ""

log_step "Building and installing WebDriverAgent..."
cd "$WDA_DIR"

xcodebuild clean -project WebDriverAgent.xcodeproj -scheme "$SCHEME" -quiet 2>/dev/null || true

log_info "Building and deploying WDA to device..."
xcodebuild build-for-testing test-without-building \
    -project WebDriverAgent.xcodeproj \
    -scheme "$SCHEME" \
    -destination "id=$UDID" \
    -destination-timeout "$DESTINATION_TIMEOUT" \
    -allowProvisioningUpdates \
    DEVELOPMENT_TEAM="$TEAM_ID" \
    CODE_SIGN_IDENTITY="iPhone Developer" \
    PRODUCT_BUNDLE_IDENTIFIER="$WDA_BUNDLE_ID" \
    USE_DESTINATION_ARTIFACTS=YES

BUILD_RESULT=$?

# ──────────────────────────────────────────────
# Step 5: Verify installation
# ──────────────────────────────────────────────
if [[ $BUILD_RESULT -eq 0 ]]; then
    log_info "WebDriverAgent installed successfully!"
    echo ""
    log_info "=== Next Steps ==="

    if [[ "$MAJOR_VERSION" -ge 17 ]]; then
        log_info "For iOS 17+, ensure tunneld is running before using WDA:"
        log_info "  sudo python3 -m pymobiledevice3 remote tunneld"
        log_info ""
        log_info "Launch WDA (with tunnel):"
        log_info "  python3 -m pymobiledevice3 developer dvt launch --udid $UDID --tunnel '' ${WDA_BUNDLE_ID}.xctrunner"
    else
        log_info "Launch WDA:"
        log_info "  python3 -m pymobiledevice3 developer dvt launch --udid $UDID ${WDA_BUNDLE_ID}.xctrunner"
    fi

    log_info ""
    log_info "Port forward WDA:"
    log_info "  python3 -m pymobiledevice3 usbmux forward --serial $UDID 8100 8100"
    log_info ""
    log_info "Verify WDA is running:"
    log_info "  curl http://127.0.0.1:8100/status"
else
    log_error "Failed to install WebDriverAgent"
    log_info ""
    log_info "Troubleshooting:"
    log_info "  1. Check that Team ID '$TEAM_ID' is correct (--team-id option)"
    log_info "  2. Ensure device is unlocked and trusted"
    if [[ "$MAJOR_VERSION" -ge 17 ]]; then
        log_info "  3. Verify Developer Mode is ON (Settings → Privacy & Security → Developer Mode)"
        log_info "  4. Verify tunneld is running: ps aux | grep tunneld"
    else
        log_info "  3. Verify DeveloperDiskImage is mounted"
    fi
    log_info "  5. Try opening WebDriverAgent.xcodeproj in Xcode and fixing signing manually"
    exit 1
fi
