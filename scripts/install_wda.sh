#!/bin/bash
#
# WebDriverAgent (WDA) Installation Script
# Builds and installs WDA on iOS devices for UI testing
# Supports: macOS only (requires Xcode)
#
# Usage: ./install_wda.sh --udid <DEVICE_UDID> --team <TEAM_ID> [OPTIONS]
#
# Co-Authored-By: Warp <agent@warp.dev>

set -o pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Default values
DEVICE_UDID=""
TEAM_ID=""
BUNDLE_ID="com.microsoft.wdar"
WDA_SOURCE="appium"  # appium or github
XCODE_PATH=""
SKIP_BUILD=false
SKIP_INSTALL=false
LIST_DEVICES=false

log_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }
log_step() { echo -e "${CYAN}▶ $1${NC}"; }

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║        WebDriverAgent (WDA) Installation Script           ║"
    echo "║              For iOS UI Test Automation                   ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Required (for build):"
    echo "  --udid <UDID>       Device UDID to build/install WDA"
    echo "  --team <TEAM_ID>    Apple Development Team ID"
    echo ""
    echo "Optional:"
    echo "  --bundle-id <ID>    Bundle ID prefix (default: com.microsoft.wdar)"
    echo "  --xcode <PATH>      Path to Xcode.app (default: current xcode-select)"
    echo "  --source <SOURCE>   WDA source: 'appium' or 'github' (default: appium)"
    echo "  --skip-build        Skip build, only install existing WDA"
    echo "  --skip-install      Skip install, only build WDA"
    echo "  --list-devices      List connected iOS devices and exit"
    echo "  -h, --help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --list-devices"
    echo "  $0 --udid 00008030-0005743926A0802E --team ABC123XYZ"
    echo "  $0 --udid 00008030-0005743926A0802E --team ABC123XYZ --xcode /Applications/Xcode.app"
    echo "  $0 --udid 00008030-0005743926A0802E --team ABC123XYZ --bundle-id com.mycompany.wda"
}

# Check if running on macOS
check_macos() {
    if [[ "$(uname -s)" != "Darwin" ]]; then
        log_error "This script only runs on macOS"
        exit 1
    fi
}

# Check prerequisites
check_prerequisites() {
    log_step "Checking prerequisites..."
    
    local missing=()
    
    # Xcode
    if ! command -v xcodebuild &> /dev/null; then
        missing+=("Xcode (xcodebuild)")
    fi
    
    # Check for device tools
    if ! command -v idevice_id &> /dev/null && ! command -v python3 &> /dev/null; then
        missing+=("libimobiledevice or python3")
    fi
    
    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing prerequisites:"
        for item in "${missing[@]}"; do
            echo "  - $item"
        done
        echo ""
        echo "Install with:"
        echo "  brew install libimobiledevice"
        echo "  pip3 install pymobiledevice3"
        exit 1
    fi
    
    log_success "Prerequisites OK"
}

# List connected iOS devices
list_devices() {
    echo ""
    log_info "Connected iOS Devices:"
    echo "─────────────────────────────────────────────────────────────"
    
    # Try pymobiledevice3 first (iOS 17+ support)
    if command -v python3 &> /dev/null && python3 -c "import pymobiledevice3" 2>/dev/null; then
        python3 -m pymobiledevice3 usbmux list 2>/dev/null | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if not data:
        print('  No devices found')
    for d in data:
        udid = d.get('UniqueDeviceID', d.get('Identifier', 'Unknown'))
        name = d.get('DeviceName', 'Unknown')
        version = d.get('ProductVersion', '?')
        model = d.get('ProductType', '?')
        print(f'  UDID: {udid}')
        print(f'  Name: {name}')
        print(f'  iOS:  {version} ({model})')
        print('  ─────────────────────────────────')
except:
    pass
" 2>/dev/null
    # Fallback to idevice_id
    elif command -v idevice_id &> /dev/null; then
        DEVICES=$(idevice_id -l 2>/dev/null)
        if [[ -z "$DEVICES" ]]; then
            echo "  No devices found"
        else
            while IFS= read -r udid; do
                NAME=$(ideviceinfo -u "$udid" -k DeviceName 2>/dev/null || echo "Unknown")
                VERSION=$(ideviceinfo -u "$udid" -k ProductVersion 2>/dev/null || echo "?")
                MODEL=$(ideviceinfo -u "$udid" -k ProductType 2>/dev/null || echo "?")
                echo "  UDID: $udid"
                echo "  Name: $NAME"
                echo "  iOS:  $VERSION ($MODEL)"
                echo "  ─────────────────────────────────"
            done <<< "$DEVICES"
        fi
    # Fallback to system_profiler
    else
        system_profiler SPUSBDataType 2>/dev/null | grep -A 20 "iPhone\|iPad" | grep -E "Serial Number|iPhone|iPad" | head -20
    fi
    echo ""
}

# Switch Xcode version
switch_xcode() {
    if [[ -n "$XCODE_PATH" ]]; then
        if [[ ! -d "$XCODE_PATH" ]]; then
            log_error "Xcode not found at: $XCODE_PATH"
            exit 1
        fi
        
        log_info "Switching to Xcode at: $XCODE_PATH"
        sudo xcode-select -s "$XCODE_PATH/Contents/Developer"
    fi
    
    XCODE_VERSION=$(xcodebuild -version | head -1)
    log_success "Using $XCODE_VERSION"
}

# Get WDA source path
get_wda_path() {
    local wda_path=""
    
    case "$WDA_SOURCE" in
        appium)
            wda_path="$HOME/.appium/node_modules/appium-xcuitest-driver/node_modules/appium-webdriveragent"
            
            if [[ ! -d "$wda_path" ]]; then
                log_warning "Appium WDA not found. Attempting to install..."
                
                if command -v appium &> /dev/null; then
                    appium driver install xcuitest 2>/dev/null || true
                else
                    log_error "Appium not installed. Install with: npm install -g appium"
                    log_info "Or use --source github to download WDA directly"
                    exit 1
                fi
            fi
            ;;
        github)
            wda_path="/tmp/WebDriverAgent"
            
            if [[ ! -d "$wda_path" ]]; then
                log_info "Downloading WebDriverAgent from GitHub..."
                cd /tmp
                rm -rf WebDriverAgent
                git clone https://github.com/appium/WebDriverAgent.git
            fi
            ;;
        *)
            log_error "Invalid source: $WDA_SOURCE. Use 'appium' or 'github'"
            exit 1
            ;;
    esac
    
    if [[ ! -d "$wda_path" ]]; then
        log_error "WDA source not found at: $wda_path"
        exit 1
    fi
    
    echo "$wda_path"
}

# Build WDA
build_wda() {
    local wda_path="$1"
    
    log_step "Building WebDriverAgent..."
    echo "  Device:    $DEVICE_UDID"
    echo "  Team:      $TEAM_ID"
    echo "  Bundle ID: $BUNDLE_ID"
    echo ""
    
    cd "$wda_path"
    
    # Clean DerivedData for fresh build
    rm -rf ~/Library/Developer/Xcode/DerivedData/WebDriverAgent-* 2>/dev/null
    
    xcodebuild -project WebDriverAgent.xcodeproj \
        -scheme WebDriverAgentRunner \
        -destination "id=$DEVICE_UDID" \
        -allowProvisioningUpdates \
        DEVELOPMENT_TEAM="$TEAM_ID" \
        CODE_SIGN_IDENTITY="Apple Development" \
        PRODUCT_BUNDLE_IDENTIFIER="$BUNDLE_ID" \
        clean build-for-testing 2>&1 | while IFS= read -r line; do
            # Show progress indicators
            if [[ "$line" == *"Build Succeeded"* ]] || [[ "$line" == *"BUILD SUCCEEDED"* ]]; then
                echo -e "${GREEN}$line${NC}"
            elif [[ "$line" == *"error:"* ]]; then
                echo -e "${RED}$line${NC}"
            elif [[ "$line" == *"warning:"* ]]; then
                echo -e "${YELLOW}$line${NC}"
            elif [[ "$line" == *"Compiling"* ]] || [[ "$line" == *"Linking"* ]]; then
                echo -e "${CYAN}  $line${NC}"
            fi
        done
    
    # Check build result
    WDA_APP=$(find ~/Library/Developer/Xcode/DerivedData/WebDriverAgent-*/Build/Products/Debug-iphoneos -name "WebDriverAgentRunner-Runner.app" -type d 2>/dev/null | head -1)
    
    if [[ -d "$WDA_APP" ]]; then
        log_success "WDA built successfully"
        echo "  Location: $WDA_APP"
        return 0
    else
        log_error "WDA build failed"
        return 1
    fi
}

# Create IPA from built WDA
create_ipa() {
    log_step "Creating WDA IPA..."
    
    WDA_APP=$(find ~/Library/Developer/Xcode/DerivedData/WebDriverAgent-*/Build/Products/Debug-iphoneos -name "WebDriverAgentRunner-Runner.app" -type d 2>/dev/null | head -1)
    
    if [[ ! -d "$WDA_APP" ]]; then
        log_error "WDA app not found. Build first."
        return 1
    fi
    
    cd /tmp
    rm -rf Payload WDA.ipa
    mkdir -p Payload
    cp -r "$WDA_APP" Payload/
    zip -rq WDA.ipa Payload
    rm -rf Payload
    
    if [[ -f "/tmp/WDA.ipa" ]]; then
        log_success "WDA IPA created: /tmp/WDA.ipa"
        return 0
    else
        log_error "Failed to create IPA"
        return 1
    fi
}

# Install WDA on device
install_wda() {
    log_step "Installing WDA on device: $DEVICE_UDID"
    
    if [[ ! -f "/tmp/WDA.ipa" ]]; then
        log_error "WDA IPA not found at /tmp/WDA.ipa"
        return 1
    fi
    
    # Try tidevice first (most reliable)
    if command -v tidevice &> /dev/null; then
        log_info "Using tidevice for installation..."
        tidevice -u "$DEVICE_UDID" install /tmp/WDA.ipa
        
    # Try ios-deploy
    elif command -v ios-deploy &> /dev/null; then
        log_info "Using ios-deploy for installation..."
        ios-deploy --id "$DEVICE_UDID" --bundle /tmp/WDA.ipa
        
    # Try ideviceinstaller
    elif command -v ideviceinstaller &> /dev/null; then
        log_info "Using ideviceinstaller for installation..."
        ideviceinstaller -u "$DEVICE_UDID" -i /tmp/WDA.ipa
        
    # Try pymobiledevice3
    elif python3 -c "import pymobiledevice3" 2>/dev/null; then
        log_info "Using pymobiledevice3 for installation..."
        python3 -m pymobiledevice3 apps install --udid "$DEVICE_UDID" /tmp/WDA.ipa
        
    else
        log_error "No installation tool found. Install one of:"
        echo "  pip3 install tidevice"
        echo "  brew install ios-deploy"
        echo "  brew install ideviceinstaller"
        return 1
    fi
    
    if [[ $? -eq 0 ]]; then
        log_success "WDA installed on device"
        return 0
    else
        log_error "WDA installation failed"
        return 1
    fi
}

# Verify WDA installation
verify_wda() {
    log_step "Verifying WDA installation..."
    
    # Check if app is installed
    local installed=false
    
    if command -v tidevice &> /dev/null; then
        if tidevice -u "$DEVICE_UDID" applist 2>/dev/null | grep -q "$BUNDLE_ID"; then
            installed=true
        fi
    elif command -v ideviceinstaller &> /dev/null; then
        if ideviceinstaller -u "$DEVICE_UDID" -l 2>/dev/null | grep -q "$BUNDLE_ID"; then
            installed=true
        fi
    fi
    
    if [[ "$installed" == true ]]; then
        log_success "WDA is installed with bundle ID: ${BUNDLE_ID}.xctrunner"
    else
        log_warning "Could not verify WDA installation"
    fi
}

# Test WDA launch
test_wda_launch() {
    log_step "Testing WDA launch..."
    
    # Launch WDA
    if python3 -c "import pymobiledevice3" 2>/dev/null; then
        python3 -m pymobiledevice3 developer dvt launch --tunnel "$DEVICE_UDID" "${BUNDLE_ID}.xctrunner" 2>&1 &
        WDA_PID=$!
        sleep 5
        
        # Try to connect
        if curl -s --connect-timeout 3 http://127.0.0.1:8100/status &>/dev/null; then
            log_success "WDA is responding on port 8100"
            kill $WDA_PID 2>/dev/null
            return 0
        else
            log_warning "WDA launched but not responding. May need port forwarding."
            log_info "Try: iproxy 8100 8100 -u $DEVICE_UDID"
            kill $WDA_PID 2>/dev/null
            return 1
        fi
    else
        log_info "Skipping launch test (pymobiledevice3 not available)"
    fi
}

# Print summary
print_summary() {
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}                    Installation Complete                   ${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "  Device:     $DEVICE_UDID"
    echo "  Bundle ID:  ${BUNDLE_ID}.xctrunner"
    echo "  IPA:        /tmp/WDA.ipa"
    echo ""
    echo "Next steps for iOS 17+:"
    echo "─────────────────────────────────────────"
    echo "1. Start tunneld (keep running):"
    echo "   sudo python3 -m pymobiledevice3 remote tunneld"
    echo ""
    echo "2. Launch WDA:"
    echo "   python3 -m pymobiledevice3 developer dvt launch --tunnel $DEVICE_UDID ${BUNDLE_ID}.xctrunner"
    echo ""
    echo "3. Port forward (if needed):"
    echo "   iproxy 8100 8100 -u $DEVICE_UDID"
    echo ""
    echo "4. Verify WDA status:"
    echo "   curl http://127.0.0.1:8100/status"
    echo ""
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --udid)
            DEVICE_UDID="$2"
            shift 2
            ;;
        --team)
            TEAM_ID="$2"
            shift 2
            ;;
        --bundle-id)
            BUNDLE_ID="$2"
            shift 2
            ;;
        --xcode)
            XCODE_PATH="$2"
            shift 2
            ;;
        --source)
            WDA_SOURCE="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --skip-install)
            SKIP_INSTALL=true
            shift
            ;;
        --list-devices)
            LIST_DEVICES=true
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# Main
main() {
    print_banner
    check_macos
    
    # List devices mode
    if [[ "$LIST_DEVICES" == true ]]; then
        list_devices
        exit 0
    fi
    
    # Validate required arguments
    if [[ -z "$DEVICE_UDID" ]]; then
        log_error "Device UDID is required. Use --udid <UDID>"
        echo ""
        list_devices
        exit 1
    fi
    
    if [[ -z "$TEAM_ID" ]] && [[ "$SKIP_BUILD" == false ]]; then
        log_error "Team ID is required for building. Use --team <TEAM_ID>"
        echo ""
        echo "Find your Team ID in Xcode:"
        echo "  Xcode → Settings → Accounts → Select team → View Details"
        exit 1
    fi
    
    check_prerequisites
    switch_xcode
    
    WDA_PATH=$(get_wda_path)
    log_success "WDA source: $WDA_PATH"
    
    # Build
    if [[ "$SKIP_BUILD" == false ]]; then
        if ! build_wda "$WDA_PATH"; then
            exit 1
        fi
    fi
    
    # Create IPA
    if ! create_ipa; then
        exit 1
    fi
    
    # Install
    if [[ "$SKIP_INSTALL" == false ]]; then
        if ! install_wda; then
            exit 1
        fi
        verify_wda
    fi
    
    print_summary
}

main
