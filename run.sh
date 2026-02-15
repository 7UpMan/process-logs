#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <main-class> [args...]" >&2
  echo "Example: $0 com.s4apps.processlog.ProcessLog --help" >&2
  echo "Example: $0 com.s4apps.processlog.ProcessLog -v --database *" >&2
  echo "Make sure to build the project first with: ( cd $(dirname "$0") && mvn -q -DskipTests package )" >&2
  exit 1
fi

main_class="$1"
shift

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$script_dir"

shopt -s nullglob
jar_candidates=(
  "$project_root"/target/*.jar
)

fat_jar=""
if [[ ${#jar_candidates[@]} -gt 0 ]]; then
  fat_jar="$(ls -t "$project_root"/target/*.jar | grep -v -E -- '-(sources|javadoc|original|tests)\.jar$' | head -n 1 || true)"
fi

warn_build() {
  printf "\033[33mWARNING: %s\033[0m\n" "$1" >&2
  printf "Build with: ( cd %s && mvn -q -DskipTests package )\n" "$project_root" >&2
}

if [[ -z "$fat_jar" || ! -f "$fat_jar" ]]; then
  warn_build "Fat jar not found in $project_root/target"
  exit 1
fi

if find "$project_root/src/main" "$project_root/pom.xml" -type f -newer "$fat_jar" | grep -q .; then
  warn_build "Fat jar appears out of date: $fat_jar"
fi

java -cp "$fat_jar" "$main_class" "$@"
