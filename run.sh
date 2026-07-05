#!/usr/bin/env bash
# ============================================================
# JavaLive Run Script
# ============================================================
# Builds (if needed) and runs the JavaLive example application.
#
# Usage:
#   ./run.sh             — build + run
#   ./run.sh --no-build  — skip build, just run
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

NO_BUILD=false
for arg in "$@"; do
    [[ "$arg" == "--no-build" ]] && NO_BUILD=true
done

echo ""
echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║        JavaLive Example Application             ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
echo ""

if [[ "$NO_BUILD" == false ]]; then
    echo -e "${YELLOW}🔨 Building project first...${NC}"
    mvn clean install -DskipTests -q
    echo -e "${GREEN}✅ Build complete${NC}"
    echo ""
fi

echo -e "${YELLOW}🚀 Starting JavaLive Example on port 8080...${NC}"
echo ""
echo -e "  ${GREEN}🌐 Home:          http://localhost:8080${NC}"
echo -e "  ${GREEN}📊 Dashboard:     http://localhost:8080/dashboard${NC}"
echo -e "  ${GREEN}👥 Users:         http://localhost:8080/users${NC}"
echo -e "  ${GREEN}🔌 Status API:    http://localhost:8080/javalive/status${NC}"
echo -e "  ${GREEN}🗄️  H2 Console:   http://localhost:8080/h2-console${NC}"
echo ""
echo -e "  Press ${YELLOW}Ctrl+C${NC} to stop."
echo ""

mvn -pl javalive-example spring-boot:run
