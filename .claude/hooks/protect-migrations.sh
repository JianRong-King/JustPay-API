#!/usr/bin/env bash
# PreToolUse (Edit|Write|MultiEdit): block edits to committed Flyway migrations.

input=$(cat)
file=$(printf '%s' "$input" \
  | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 \
  | sed 's/^"file_path"[[:space:]]*:[[:space:]]*"//; s/"$//' \
  | tr '\\' '/' | tr -s '/')

case "$file" in
  */db/migration/V*__*.sql) ;;
  *) exit 0 ;;
esac

root="${CLAUDE_PROJECT_DIR:-.}"
if git -C "$root" ls-files --error-unmatch "$file" >/dev/null 2>&1; then
  echo "Blocked: $(basename "$file") is already committed (applied). Never edit an applied migration. Create a new one with /new-migration." >&2
  exit 2
fi
exit 0
