#!/usr/bin/env bash
# ============================================================
# JavaLive Build Script
# ============================================================
# Builds the entire JavaLive multi-module Maven project.
#
# Usage:
#   ./build.sh           — clean build, skip tests
#   ./build.sh test      — clean build WITH tests
#   ./build.sh fast      — compile only (no clean)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

MODE="${1:-skip}"

echo ""
echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           JavaLive Build Script v1.0.0          ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
echo ""

# Check prerequisites
check_java() {
    if ! command -v java &>/dev/null; then
        echo -e "${RED}❌ Java not found. Please install Java 21.${NC}"
        exit 1
    fi
    JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [[ "$JAVA_VER" -lt 21 ]]; then
        echo -e "${RED}❌ Java 21+ required. Found: Java $JAVA_VER${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Java $JAVA_VER detected${NC}"
}

check_maven() {
    if ! command -v mvn &>/dev/null; then
        echo -e "${RED}❌ Maven not found. Please install Maven 3.9+.${NC}"
        exit 1
    fi
    MVN_VER=$(mvn -version 2>&1 | head -1)
    echo -e "${GREEN}✅ $MVN_VER${NC}"
}

check_java
check_maven

echo ""

case "$MODE" in
    test)
        echo -e "${YELLOW}🔨 Building ALL modules WITH tests...${NC}"
        mvn clean install
        ;;
    fast)
        echo -e "${YELLOW}⚡ Fast compile (no clean, no tests)...${NC}"
        mvn compile -DskipTests
        ;;
    *)
        echo -e "${YELLOW}🔨 Building ALL modules (tests skipped)...${NC}"
        mvn clean install -DskipTests
        ;;
esac

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           ✅ BUILD SUCCESSFUL                    ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  To run the example:                            ║${NC}"
echo -e "${GREEN}║    ./run.sh                                     ║${NC}"
echo -e "${GREEN}║  Then open: http://localhost:8080               ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
echo ""
