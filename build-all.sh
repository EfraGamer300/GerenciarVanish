#!/bin/bash
# VanishPlus v1.2.0 — Build all MC versions for Spigot/Paper/Purpur
# Requires: Java 21, Gradle 8.14+
# Supports: 1.19, 1.19.2-4, 1.20-1.20.6, 1.21-1.21.9, 26.1.2

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
export PATH=$JAVA_HOME/bin:$PATH

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
    "26.1.2:1.21.5-R0.1-SNAPSHOT"
)

OUTPUT_DIR="build/release"
mkdir -p "$OUTPUT_DIR"

echo "============================================"
echo "  VanishPlus v1.2.0 — Multi-Version Build"
echo "  MC: 1.19 -> 26.1.2"
echo "  Backend: Spigot/Paper/Purpur (Folia compatible)"
echo "============================================"

OK=0
FAIL=0
FAILED_LIST=""

for entry in "${VERSIONS[@]}"; do
    mc_short="${entry%%:*}"
    api_ver="${entry##*:}"
    mc_tag=$(echo "$mc_short" | tr '.' '_')
    OUTPUT_JAR="$OUTPUT_DIR/VanishPlus-v1.2.0-mc${mc_short}.jar"

    echo ""
    echo ">>> MC ${mc_short} (API: ${api_ver})"

    if ./gradlew shadowJar -Pmc="${api_ver}" --no-daemon --quiet 2>/dev/null; then
        BUILT_JAR=$(find build/libs -name "VanishPlus*.jar" ! -name "*sources*" 2>/dev/null | head -1)
        if [ -n "$BUILT_JAR" ] && [ -f "$BUILT_JAR" ]; then
            cp "$BUILT_JAR" "$OUTPUT_JAR"
            SIZE=$(du -h "$OUTPUT_JAR" | cut -f1)
            echo "    OK (${SIZE})"
            ((OK++))
        else
            echo "    FAIL: JAR output not found"
            FAILED_LIST="$FAILED_LIST ${mc_short}"
            ((FAIL++))
        fi
    else
        echo "    FAIL: Build error"
        FAILED_LIST="$FAILED_LIST ${mc_short}"
        ((FAIL++))
    fi

    # Clean between builds to force recompile
    rm -rf build/libs/*.jar .gradle/8.14/fileHashes 2>/dev/null
done

echo ""
echo "============================================"
echo "  RESULT: ${OK} OK, ${FAIL} failed"
if [ -n "$FAILED_LIST" ]; then
    echo "  Failed:${FAILED_LIST}"
fi
echo "============================================"
echo ""
echo "Release JARs (${OUTPUT_DIR}/):"
ls -lh "${OUTPUT_DIR}"/*.jar 2>/dev/null || echo "  (none)"
echo ""
echo "Total: $(du -sh "${OUTPUT_DIR}" 2>/dev/null | cut -f1)"
