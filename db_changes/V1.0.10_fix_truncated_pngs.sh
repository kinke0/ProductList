#!/bin/bash
# V1.0.10 批量检测并修复截断的PNG文件
# 用法: bash V1.0.10_fix_truncated_pngs.sh [图片目录]
# 默认扫描 ./uploads/images 目录
# 服务器用法: bash V1.0.10_fix_truncated_pngs.sh /opt/productlist/data/uploads/images

DIR=${1:-./uploads/images}
FIXED=0
TOTAL=0
FAILED=0

if [ ! -d "$DIR" ]; then
    echo "错误: 目录不存在 - $DIR"
    exit 1
fi

echo "扫描目录: $DIR"
echo "---"

while IFS= read -r f; do
    TOTAL=$((TOTAL + 1))
    SIZE=$(wc -c < "$f" 2>/dev/null | tr -d ' ')
    if [ "$SIZE" -lt 24 ]; then
        echo "跳过(文件过小): $f ($SIZE bytes)"
        FAILED=$((FAILED + 1))
        continue
    fi
    TAIL=$(xxd -p -l 12 -s -12 "$f" 2>/dev/null)
    if [ "$TAIL" != "0000000049454e44ae426082" ]; then
        echo "修复: $f ($SIZE bytes)"
        printf '\x00\x00\x00\x00\x49\x45\x4e\x44\xae\x42\x60\x82' >> "$f"
        VERIFY=$(xxd -p -l 12 -s -12 "$f" 2>/dev/null)
        if [ "$VERIFY" = "0000000049454e44ae426082" ]; then
            NEW_SIZE=$(wc -c < "$f" 2>/dev/null | tr -d ' ')
            echo "  -> 成功 ($NEW_SIZE bytes)"
            FIXED=$((FIXED + 1))
        else
            echo "  -> 失败!"
            FAILED=$((FAILED + 1))
        fi
    fi
done < <(find "$DIR" -iname '*.png' -type f 2>/dev/null)

echo "---"
echo "扫描完成: 共 $TOTAL 个PNG文件, 修复 $FIXED 个, 失败 $FAILED 个"
