#!/usr/bin/env bash
# PostToolUse (Edit|Write|MultiEdit): flag broken project rules in the saved Java file.

input=$(cat)
file=$(printf '%s' "$input" \
  | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 \
  | sed 's/^"file_path"[[:space:]]*:[[:space:]]*"//; s/"$//' \
  | tr '\\' '/' | tr -s '/')

[ -f "$file" ] || exit 0

problems=""
add() { [ -n "$2" ] && problems+="$1"$'\n'"$2"$'\n'; }

case "$file" in
  */src/main/java/*.java)
    add "double/float used (money must be BigDecimal):" "$(grep -nE '\b(double|float|Double|Float)\b' "$file")"
    add "@Autowired found (constructor injection only):" "$(grep -n '@Autowired' "$file")"
    add "Lombok found (not allowed):" "$(grep -n 'lombok' "$file")"
    add "RoundingMode other than HALF_EVEN:" "$(grep -nE 'RoundingMode\.[A-Z_]+' "$file" | grep -v 'HALF_EVEN')"
    ;;
  */src/test/java/*.java)
    add "Test skipped or weakened (never skip tests; ask the user):" "$(grep -nE '@Disabled|assumeTrue|assumeFalse' "$file")"
    ;;
  *) exit 0 ;;
esac

if [ -n "$problems" ]; then
  printf 'Rule check failed in %s\n%s' "$(basename "$file")" "$problems" >&2
  exit 2
fi
exit 0
