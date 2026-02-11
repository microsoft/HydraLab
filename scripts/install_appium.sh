#!/bin/bash
#
# Appium Installation Script
# Supports: macOS and Linux
# Usage: ./install_appium.sh [--ios] [--android] [--all]
#
# Co-Authored-By: Warp <agent@warp.dev>

set -o pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default options
INSTALL_IOS=false
INSTALL_ANDROID=false
APPIUM_VERSION="latest"

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║             Appium Installation Script                    ║"
    echo "║         For iOS and Android Test Automation               ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Detect OS
detect_os() {
    case "$(uname -s)" in
        Darwin*)    OS="macos" ;;
        Linux*)     OS="linux" ;;
        MINGW*|CYGWIN*|MSYS*) OS="windows" ;;
        *)          OS="unknown" ;;
    esac
    echo "$OS"
}

# Check if command exists
command_exists() {
    command -v "$1" &> /dev/null
}

# Install Homebrew (macOS)
install_homebrew() {
    if [[ "$(detect_os)" == "macos" ]] && ! command_exists brew; then
        log_info "Installing Homebrew..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
        
        # Add to PATH for Apple Silicon
        if [[ -f /opt/homebrew/bin/brew ]]; then
            eval "$(/opt/homebrew/bin/brew shellenv)"
        fi
        log_success "Homebrew installed"
    fi
}

# Install Node.js
install_nodejs() {
    if command_exists node; then
        NODE_VERSION=$(node --version)
        log_success "Node.js already installed: $NODE_VERSION"
        return 0
    fi

    log_info "Installing Node.js..."
    
    case "$(detect_os)" in
        macos)
            brew install node
            ;;
        linux)
            # Use NodeSource for latest LTS
            curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
            sudo apt-get install -y nodejs
            ;;
    esac
    
    if command_exists node; then
        log_success "Node.js installed: $(node --version)"
    else
        log_error "Failed to install Node.js"
        exit 1
    fi
}

# Install Appium
install_appium() {
    log_info "Installing Appium ${APPIUM_VERSION}..."
    
    if [[ "$APPIUM_VERSION" == "latest" ]]; then
        npm install -g appium
    else
        npm install -g "appium@${APPIUM_VERSION}"
    fi
    
    if command_exists appium; then
        log_success "Appium installed: $(appium --version)"
    else
        log_error "Failed to install Appium"
        exit 1
    fi
}

# Install iOS dependencies (macOS only)
install_ios_dependencies() {
    if [[ "$(detect_os)" != "macos" ]]; then
        log_warning "iOS testing is only supported on macOS"
        return 1
    fi

    log_info "Installing iOS dependencies..."

    # Check Xcode
    if ! command_exists xcodebuild; then
        log_error "Xcode is required for iOS testing. Please install from App Store."
        exit 1
    fi
    log_success "Xcode found: $(xcodebuild -version | head -1)"

    # Accept Xcode license
    sudo xcodebuild -license accept 2>/dev/null || true

    # Install Xcode Command Line Tools
    xcode-select --install 2>/dev/null || true

    # Install iOS dependencies via Homebrew
    log_info "Installing iOS tools via Homebrew..."
    brew install libimobiledevice ideviceinstaller ios-deploy carthage 2>/dev/null || true

    # Install XCUITest driver
    log_info "Installing Appium XCUITest driver..."
    appium driver install xcuitest

    # Install pymobiledevice3 for iOS 17+ support
    log_info "Installing pymobiledevice3 for iOS 17+ support..."
    pip3 install --user pymobiledevice3

    # Verify WDA path
    WDA_PATH="$HOME/.appium/node_modules/appium-xcuitest-driver/node_modules/appium-webdriveragent"
    if [[ -d "$WDA_PATH" ]]; then
        log_success "WebDriverAgent found at: $WDA_PATH"
    else
        log_warning "WebDriverAgent not found. It will be downloaded on first use."
    fi

    log_success "iOS dependencies installed"
}

# Install Android dependencies
install_android_dependencies() {
    log_info "Installing Android dependencies..."

    case "$(detect_os)" in
        macos)
            # Install Java
            if ! command_exists java; then
                log_info "Installing OpenJDK..."
                brew install openjdk
                sudo ln -sfn "$(brew --prefix)/opt/openjdk/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk.jdk 2>/dev/null || true
            fi
            log_success "Java found: $(java -version 2>&1 | head -1)"

            # Install Android SDK via Homebrew
            if [[ -z "$ANDROID_HOME" ]]; then
                log_info "Installing Android SDK..."
                brew install --cask android-commandlinetools 2>/dev/null || brew install android-commandlinetools 2>/dev/null || true
                
                # Set ANDROID_HOME
                export ANDROID_HOME="$HOME/Library/Android/sdk"
                mkdir -p "$ANDROID_HOME"
                
                echo "" >> ~/.zshrc
                echo "# Android SDK" >> ~/.zshrc
                echo "export ANDROID_HOME=\"$HOME/Library/Android/sdk\"" >> ~/.zshrc
                echo "export PATH=\"\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/tools:\$ANDROID_HOME/tools/bin:\$PATH\"" >> ~/.zshrc
            fi
            ;;
        linux)
            # Install Java
            if ! command_exists java; then
                sudo apt-get install -y openjdk-17-jdk
            fi

            # Set up Android SDK
            if [[ -z "$ANDROID_HOME" ]]; then
                export ANDROID_HOME="$HOME/Android/Sdk"
                mkdir -p "$ANDROID_HOME"
                
                echo "" >> ~/.bashrc
                echo "# Android SDK" >> ~/.bashrc
                echo "export ANDROID_HOME=\"$HOME/Android/Sdk\"" >> ~/.bashrc
                echo "export PATH=\"\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/tools:\$PATH\"" >> ~/.bashrc
            fi
            ;;
    esac

    # Install UIAutomator2 driver
    log_info "Installing Appium UIAutomator2 driver..."
    appium driver install uiautomator2

    log_success "Android dependencies installed"
}

# Build WebDriverAgent for iOS device
build_wda() {
    if [[ "$(detect_os)" != "macos" ]]; then
        log_warning "WDA build is only supported on macOS"
        return 1
    fi

    WDA_PATH="$HOME/.appium/node_modules/appium-xcuitest-driver/node_modules/appium-webdriveragent"
    
    if [[ ! -d "$WDA_PATH" ]]; then
        log_error "WebDriverAgent not found. Run 'appium driver install xcuitest' first."
        return 1
    fi

    echo ""
    read -p "Enter Device UDID (or press Enter to skip WDA build): " DEVICE_UDID
    
    if [[ -z "$DEVICE_UDID" ]]; then
        log_info "Skipping WDA build. You can build later with:"
        echo "  cd $WDA_PATH"
        echo "  xcodebuild -project WebDriverAgent.xcodeproj -scheme WebDriverAgentRunner \\"
        echo "    -destination 'id=YOUR_DEVICE_UDID' -allowProvisioningUpdates build-for-testing"
        return 0
    fi

    read -p "Enter Development Team ID: " TEAM_ID
    if [[ -z "$TEAM_ID" ]]; then
        log_error "Team ID is required"
        return 1
    fi

    read -p "Enter Bundle ID prefix (default: com.yourcompany.wda): " BUNDLE_ID
    BUNDLE_ID=${BUNDLE_ID:-com.yourcompany.wda}

    log_info "Building WebDriverAgent..."
    cd "$WDA_PATH"
    
    xcodebuild -project WebDriverAgent.xcodeproj \
        -scheme WebDriverAgentRunner \
        -destination "id=$DEVICE_UDID" \
        -allowProvisioningUpdates \
        DEVELOPMENT_TEAM="$TEAM_ID" \
        CODE_SIGN_IDENTITY="Apple Development" \
        PRODUCT_BUNDLE_IDENTIFIER="$BUNDLE_ID" \
        clean build-for-testing

    if [[ $? -eq 0 ]]; then
        log_success "WebDriverAgent built successfully"
        
        # Create IPA for installation
        log_info "Creating WDA IPA..."
        cd /tmp && rm -rf Payload WDA.ipa && mkdir -p Payload
        WDA_APP=$(find ~/Library/Developer/Xcode/DerivedData/WebDriverAgent-*/Build/Products/Debug-iphoneos -name "WebDriverAgentRunner-Runner.app" -type d 2>/dev/null | head -1)
        
        if [[ -d "$WDA_APP" ]]; then
            cp -r "$WDA_APP" Payload/
            zip -rq WDA.ipa Payload
            log_success "WDA IPA created at /tmp/WDA.ipa"
            
            # Install if tidevice is available
            if command_exists tidevice; then
                log_info "Installing WDA on device..."
                tidevice -u "$DEVICE_UDID" install /tmp/WDA.ipa
                log_success "WDA installed on device"
            else
                log_info "Install WDA manually: tidevice -u $DEVICE_UDID install /tmp/WDA.ipa"
            fi
        fi
    else
        log_error "WebDriverAgent build failed"
        return 1
    fi
}

# Verify installation
verify_installation() {
    echo ""
    log_info "Verifying installation..."
    echo ""
    
    echo "Component Versions:"
    echo "─────────────────────────────────────────"
    
    if command_exists node; then
        echo "  Node.js:    $(node --version)"
    fi
    
    if command_exists npm; then
        echo "  npm:        $(npm --version)"
    fi
    
    if command_exists appium; then
        echo "  Appium:     $(appium --version)"
    fi
    
    echo ""
    echo "Installed Drivers:"
    echo "─────────────────────────────────────────"
    appium driver list --installed 2>/dev/null || echo "  (none)"
    
    echo ""
    
    if [[ "$INSTALL_IOS" == true ]] && [[ "$(detect_os)" == "macos" ]]; then
        echo "iOS Tools:"
        echo "─────────────────────────────────────────"
        command_exists xcodebuild && echo "  Xcode:              $(xcodebuild -version | head -1)"
        command_exists idevice_id && echo "  libimobiledevice:   ✓"
        command_exists ios-deploy && echo "  ios-deploy:         ✓"
        python3 -m pymobiledevice3 --version 2>/dev/null && echo "  pymobiledevice3:    $(python3 -m pymobiledevice3 --version 2>/dev/null)"
        echo ""
    fi
    
    log_success "Installation complete!"
}

# Print usage
print_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --ios          Install iOS testing dependencies (macOS only)"
    echo "  --android      Install Android testing dependencies"
    echo "  --all          Install both iOS and Android dependencies"
    echo "  --build-wda    Build and install WebDriverAgent for iOS device"
    echo "  --version VER  Install specific Appium version (default: latest)"
    echo "  -h, --help     Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --ios                    # Install Appium with iOS support"
    echo "  $0 --android                # Install Appium with Android support"
    echo "  $0 --all                    # Install everything"
    echo "  $0 --ios --build-wda        # Install iOS + build WDA"
    echo "  $0 --version 2.5.0 --ios    # Install specific Appium version"
}

# Parse arguments
BUILD_WDA=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --ios)
            INSTALL_IOS=true
            shift
            ;;
        --android)
            INSTALL_ANDROID=true
            shift
            ;;
        --all)
            INSTALL_IOS=true
            INSTALL_ANDROID=true
            shift
            ;;
        --build-wda)
            BUILD_WDA=true
            shift
            ;;
        --version)
            APPIUM_VERSION="$2"
            shift 2
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

# If no platform specified, detect and suggest
if [[ "$INSTALL_IOS" == false ]] && [[ "$INSTALL_ANDROID" == false ]]; then
    echo ""
    log_warning "No platform specified. Use --ios, --android, or --all"
    print_usage
    exit 1
fi

# Main installation flow
main() {
    print_banner
    
    log_info "Detected OS: $(detect_os)"
    echo ""

    # Prerequisites
    if [[ "$(detect_os)" == "macos" ]]; then
        install_homebrew
    fi
    
    install_nodejs
    install_appium
    
    # Platform-specific
    if [[ "$INSTALL_IOS" == true ]]; then
        install_ios_dependencies
    fi
    
    if [[ "$INSTALL_ANDROID" == true ]]; then
        install_android_dependencies
    fi
    
    # Optional WDA build
    if [[ "$BUILD_WDA" == true ]]; then
        build_wda
    fi
    
    verify_installation
    
    echo ""
    echo "─────────────────────────────────────────"
    echo "Next steps:"
    echo "─────────────────────────────────────────"
    echo "1. Start Appium server:  appium"
    
    if [[ "$INSTALL_IOS" == true ]] && [[ "$(detect_os)" == "macos" ]]; then
        echo "2. For iOS 17+, start tunneld:"
        echo "   sudo python3 -m pymobiledevice3 remote tunneld"
        echo "3. Build WDA if not done:  $0 --build-wda"
    fi
    
    echo ""
}

main
