#!/bin/bash
set -e

VERSIONS=(
    "1.19:1.19-R0.1-SNAPSHOT"
    "1.19.2:1.19.2-R0.1-SNAPSHOT"
    "1.19.3:1.19.3-R0.1-SNAPSHOT"
    "1.19.4:1.19.4-R0.1-SNAPSHOT"
    "1.20:1.20-R0.1-SNAPSHOT"
    "1.20.1:1.20.1-R0.1-SNAPSHOT"
    "1.20.2:1.20.2-R0.1-SNAPSHOT"
    "1.20.4:1.20.4-R0.1-SNAPSHOT"
    "1.20.6:1.20.6-R0.1-SNAPSHOT"
    "1.21:1.21-R0.1-SNAPSHOT"
    "1.21.3:1.21.3-R0.1-SNAPSHOT"
    "1.21.4:1.21.4-R0.1-SNAPSHOT"
    "1.21.7:1.21.7-R0.1-SNAPSHOT"
    "1.21.9:1.21.9-R0.1-SNAPSHOT"
    "26.1.2:26.1.2.build.66-stable"
)

OUTPUT_DIR="build/versions"
mkdir -p "$OUTPUT_DIR"

echo "=========================================="
echo "  GerenciarVanish - Build Multi-Versao"
echo "=========================================="

OK=0
FAIL=0

for entry in "${VERSIONS[@]}"; do
    mc_ver="${entry%%:*}"
    api_ver="${entry##*:}"
    OUTPUT_JAR="$OUTPUT_DIR/GerenciarVanish-1.0.0-mc${mc_ver}.jar"

    echo ""
    echo "MC $mc_ver (API: $api_ver)"

    if ./gradlew shadowJar -Pmc="$api_ver" --no-daemon -q 2>/dev/null; then
        BUILT_JAR=$(find build/libs -name "*.jar" ! -name "*sources*" 2>/dev/null | head -1)
        if [ -n "$BUILT_JAR" ]; then
            cp "$BUILT_JAR" "$OUTPUT_JAR"
            echo "  OK -> $OUTPUT_JAR"
            ((OK++))
        else
            echo "  FAIL: JAR nao encontrado"
            ((FAIL++))
        fi
    else
        echo "  FAIL: Build error"
        ((FAIL++))
    fi
done

echo ""
echo "=========================================="
echo "  RESULTADO: $OK OK, $FAIL falhas"
echo "=========================================="
ls -la "$OUTPUT_DIR"/*.jar 2>/dev/null || echo "  (nenhum JAR)"
