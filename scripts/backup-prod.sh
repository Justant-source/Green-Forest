#!/bin/bash
# prod DB + 활동 로그 일간 백업 스크립트
# 보관 기간: 14일 (14일 이전 스냅샷 자동 삭제)
#
# 사용법:
#   ./scripts/backup-prod.sh
#
# 크론 설정 (매일 새벽 3시 KST, 서버 타임존 Asia/Seoul):
#   0 3 * * * /home/justant/Data/Green-Forest/scripts/backup-prod.sh >> /var/log/greenforest-backup.log 2>&1

set -euo pipefail

# ===== 설정 =====
BACKUP_BASE_DIR="${BACKUP_BASE_DIR:-/home/justant/backups/green-forest}"
RETAIN_DAYS=14
DATE=$(date +%Y-%m-%d)
BACKUP_DIR="${BACKUP_BASE_DIR}/${DATE}"

# prod .env 에서 DB 패스워드 읽기
ENV_FILE="$(dirname "$0")/../.env.prod"
if [ -f "$ENV_FILE" ]; then
    export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

DB_CONTAINER="${DB_CONTAINER:-greenforest-mysql-prod}"
DB_NAME="${DB_NAME:-vgc_db}"
DB_USER="${DB_USER:-root}"
LOG_VOLUME_PATH="${LOG_VOLUME_PATH:-}"  # 비어있으면 docker cp 사용

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] ===== 백업 시작: $DATE ====="

# ===== 1. DB 백업 =====
echo "[$(date '+%Y-%m-%d %H:%M:%S')] DB 덤프 시작..."
docker exec "$DB_CONTAINER" \
    mysqldump -u "$DB_USER" -p"${DB_PASSWORD:-}" \
    --single-transaction --routines --triggers \
    "$DB_NAME" \
    | gzip > "${BACKUP_DIR}/db_${DATE}.sql.gz"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] DB 덤프 완료: ${BACKUP_DIR}/db_${DATE}.sql.gz"

# ===== 2. 활동 로그 백업 =====
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 로그 백업 시작..."
LOGS_BACKUP_DIR="${BACKUP_DIR}/logs"
mkdir -p "$LOGS_BACKUP_DIR"

BACKEND_CONTAINER="${BACKEND_CONTAINER:-greenforest-backend-prod}"

# app.log, error.log, activity.log 복사
for LOG_FILE in app.log error.log activity.log; do
    docker cp "${BACKEND_CONTAINER}:/app/logs/${LOG_FILE}" \
        "${LOGS_BACKUP_DIR}/${LOG_FILE}" 2>/dev/null && \
        gzip -f "${LOGS_BACKUP_DIR}/${LOG_FILE}" && \
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ${LOG_FILE} 백업 완료" || \
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] WARN: ${LOG_FILE} 없음 (건너뜀)"
done

# archive 폴더 전체 복사
docker cp "${BACKEND_CONTAINER}:/app/logs/archive" "${LOGS_BACKUP_DIR}/" 2>/dev/null && \
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] archive 로그 백업 완료" || \
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] WARN: archive 폴더 없음 (건너뜀)"

# ===== 3. 14일 이전 스냅샷 삭제 =====
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 오래된 백업 정리 (보관 기간: ${RETAIN_DAYS}일)..."
find "$BACKUP_BASE_DIR" -maxdepth 1 -type d -name "????-??-??" \
    -mtime +${RETAIN_DAYS} -exec rm -rf {} \; -print | \
    sed "s/^/[$(date '+%Y-%m-%d %H:%M:%S')] 삭제: /"

# ===== 4. 결과 요약 =====
BACKUP_SIZE=$(du -sh "$BACKUP_DIR" 2>/dev/null | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] ===== 백업 완료 (크기: ${BACKUP_SIZE}) ====="
