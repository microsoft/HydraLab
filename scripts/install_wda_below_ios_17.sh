#!/bin/bash
# Script to install WebDriverAgent on iOS devices below iOS 17
# Usage: ./install_wda_below_ios_17.sh --udid <device_udid>

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
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

usage() {
    echo "Usage: $0 --udid <device_udid>"
    echo ""
    echo "Options:"
    echo "  --udid    Device UDID (required)"
    echo "  --help    Show this help message"
    echo ""
    echo "Example:"
    echo "  $0 --udid c7ad90190806994c5c4d62117b4761adc37674c9"
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --udid)
            UDID="$2"
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
    exit 1
fi

WDA_DIR=$(dirname "$WDA_PROJECT_PATH")

log_info "Configuration:"
log_info "  Device UDID: $UDID"
log_info "  Team ID: $TEAM_ID"
log_info "  WDA Project: $WDA_PROJECT_PATH"

# Check if device is connected
log_info "Checking device connection..."
if ! python3 -m pymobiledevice3 usbmux list 2>/dev/null | grep -q "$UDID"; then
    log_error "Device with UDID $UDID is not connected"
    exit 1
fi
log_info "Device found and connected"

# Build and install WDA
log_info "Building and installing WebDriverAgent..."
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

if [[ $? -eq 0 ]]; then
    log_info "WebDriverAgent installed successfully!"
    log_info "Start WDA: python3 -m pymobiledevice3 developer dvt launch --udid $UDID $WDA_BUNDLE_ID"
else
    log_error "Failed to install WebDriverAgent"
    exit 1
fi
