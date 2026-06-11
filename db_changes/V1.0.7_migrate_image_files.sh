#!/bin/bash
# V1.0.7 图片目录层级修复 - 物理文件迁移脚本

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)/uploads/images"
DB_FILE="$(cd "$(dirname "$0")/.." && pwd)/superpower.db"
DB_BAK="$(cd "$(dirname "$0")/.." && pwd)/superpower.db.bak_before_image_fix"
MOVED=0
SKIPPED=0
MISSING=0

echo "=== 图片物理文件迁移开始 ==="
echo "图片目录: $BASE_DIR"

# 导出所有映射到临时文件
TMPFILE=$(mktemp)
sqlite3 -separator '|' "$DB_BAK" "
SELECT ir.id, ir.version_id, ir.category, ir.domain, ir.product, ir.stored_name
FROM image_resource ir
WHERE ir.version_id IN (3,4,5)
AND ir.category IS NOT NULL AND ir.category != ''
AND ir.domain IS NOT NULL AND ir.domain != ''
AND ir.product NOT IN (
    SELECT DISTINCT de.\"col_产品系统\" FROM data_entry de WHERE de.version_id=3 AND de.level=3 AND de.\"col_产品系统\" != ''
)
" > "$TMPFILE"

TOTAL=$(wc -l < "$TMPFILE")
echo "需要迁移: $TOTAL 个文件"
echo ""

while IFS='|' read -r id vid cat dom prod sname; do
    new_info=$(sqlite3 -separator '|' "$DB_FILE" "SELECT product, domain, category FROM image_resource WHERE id=$id")
    new_prod=$(echo "$new_info" | cut -d'|' -f1)
    new_dom=$(echo "$new_info" | cut -d'|' -f2)
    new_cat=$(echo "$new_info" | cut -d'|' -f3)
    
    if [ -z "$new_prod" ]; then
        continue
    fi
    
    new_dir="$BASE_DIR/$vid/$new_cat/$new_dom/$new_prod"
    new_path="$new_dir/$sname"
    
    if [ -f "$new_path" ]; then
        SKIPPED=$((SKIPPED+1))
        continue
    fi
    
    old_found=""
    old_path="$BASE_DIR/$vid/$cat/$dom/$prod/$sname"
    if [ -f "$old_path" ]; then
        old_found="$old_path"
    fi
    
    if [ -z "$old_found" ]; then
        alt_prod=$(echo "$prod" | sed 's/^1\.1\.1\./1.1.3./')
        alt_path="$BASE_DIR/$vid/$cat/$dom/$alt_prod/$sname"
        if [ -f "$alt_path" ]; then
            old_found="$alt_path"
        fi
    fi
    
    if [ -n "$old_found" ]; then
        mkdir -p "$new_dir"
        mv "$old_found" "$new_path"
        MOVED=$((MOVED+1))
    else
        MISSING=$((MISSING+1))
        echo "  缺失: id=$id $vid/$prod/$sname"
    fi
done < "$TMPFILE"

rm -f "$TMPFILE"

echo ""
echo "=== 迁移完成 ==="
echo "已移动: $MOVED"
echo "已跳过(目标已存在): $SKIPPED"
echo "源文件缺失: $MISSING"
echo ""
echo "=== 清理空目录 ==="
find "$BASE_DIR" -type d -empty -delete 2>/dev/null
echo "已清理空目录"
