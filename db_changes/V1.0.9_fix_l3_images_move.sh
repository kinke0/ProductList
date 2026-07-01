#!/bin/bash
# V1.0.9 修复版本3图片物理文件目录
# 将图片从错误的深层目录移动到正确的 L3 目录
#
# 用法:
#   在映射目录下执行:
#   bash V1.0.9_fix_l3_images_move.sh
#
#   或指定映射目录路径:
#   bash V1.0.9_fix_l3_images_move.sh /data/app

set -e

BASE_DIR="${1:-.}"
IMG_DIR="$BASE_DIR/uploads/images/3"

echo "=== 移动版本3图片文件到正确L3目录 ==="
echo "图片目录: $IMG_DIR"
echo ""

MOVED=0
SKIPPED=0
TOTAL=200

# [1/200] 接入各类业务数据.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.7 汇聚层数据资源/接入各类业务数据.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/接入各类业务数据.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [2/200] 离线采集与实时采集.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.7 汇聚层数据资源/离线采集与实时采集.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/离线采集与实时采集.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [3/200] 配置抽取策略与执行周期.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.7 汇聚层数据资源/配置抽取策略与执行周期.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/配置抽取策略与执行周期.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [4/200] 配置数据质量规则.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.8 治理层数据资源/配置数据质量规则.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/配置数据质量规则.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [5/200] 统一数据标准.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.8 治理层数据资源/统一数据标准.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/统一数据标准.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [6/200] 根据不同业务域构建分类.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.8 治理层数据资源/根据不同业务域构建分类.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/根据不同业务域构建分类.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [7/200] 覆盖各类临床数据.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.2数据空间/1.2.1 可信主题数据空间/覆盖各类临床数据.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.2 数据空间/1.2.1 可信主题数据空间/覆盖各类临床数据.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [8/200] 面向服务架构（SOA）进行整体设计.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.2数据空间/1.2.1 可信主题数据空间/面向服务架构（SOA）进行整体设计.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.2 数据空间/1.2.1 可信主题数据空间/面向服务架构（SOA）进行整体设计.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [9/200] 在院患者列表.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1.1 在院患者列表/在院患者列表.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/在院患者列表.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [10/200] 出院患者列表.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1.2 出院患者列表/出院患者列表.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/出院患者列表.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [11/200] 转出患者列表.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1.3 转出患者列表/转出患者列表.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/转出患者列表.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [12/200] 全院患者列表.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1.4 全院患者列表/全院患者列表.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/全院患者列表.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [13/200] 组合条件查询.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1.5 组合条件检索/组合条件查询.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/组合条件查询.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [14/200] 全院运营.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/全院运营.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/全院运营.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [15/200] 实时监控.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/实时监控.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/实时监控.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [16/200] 运营监控.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/运营监控.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/运营监控.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [17/200] 门诊运营.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/门诊运营.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/门诊运营.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [18/200] 门诊运营-指标筛选.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/门诊运营-指标筛选.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/门诊运营-指标筛选.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [19/200] 门急诊收入.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/门急诊收入.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/门急诊收入.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [20/200] 门急诊次均费用.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/门急诊次均费用.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/门急诊次均费用.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [21/200] 预约分析.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/预约分析.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/预约分析.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [22/200] 住院运营.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/住院运营.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/住院运营.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [23/200] 住院运营-出院人数下钻.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/住院运营-出院人数下钻.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/住院运营-出院人数下钻.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [24/200] 住院工作量.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/住院工作量.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/住院工作量.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [25/200] 住院费用分析.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/住院费用分析.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/住院费用分析.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [26/200] 人力效率.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/人力效率.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/人力效率.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [27/200] 手术分析.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/手术分析.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/手术分析.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [28/200] 住院收入.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/住院收入.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/住院收入.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [29/200] 医保收入.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/医保收入.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/医保收入.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [30/200] 药占比.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/药占比.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/药占比.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [31/200] 耗材占比.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/耗材占比.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/耗材占比.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [32/200] 检验占比.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/检验占比.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/检验占比.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [33/200] 合理用药.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/合理用药.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/合理用药.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [34/200] 抗菌药使用情况统计.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/抗菌药使用情况统计.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/抗菌药使用情况统计.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [35/200] 检查占比.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/检查占比.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/检查占比.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [36/200] 检查工作量.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 管理决策支持系统/检查工作量.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.4 医院管理决策支持系统/检查工作量.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [37/200] 门诊直接发药.png
SRC="$IMG_DIR/5. 智慧医疗/5.6 医疗保障业务/5.1.3.1 发药管理/门诊直接发药.png"
DST="$IMG_DIR/5. 智慧医疗/5.1 门诊诊疗业务/5.1.3 门诊药房管理系统/门诊直接发药.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [38/200] 发药追溯码录入.png
SRC="$IMG_DIR/5. 智慧医疗/5.6 医疗保障业务/5.1.3.1 发药管理/发药追溯码录入.png"
DST="$IMG_DIR/5. 智慧医疗/5.1 门诊诊疗业务/5.1.3 门诊药房管理系统/发药追溯码录入.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [39/200] 门诊退药.png
SRC="$IMG_DIR/5. 智慧医疗/5.6 医疗保障业务/5.1.3.2 退药管理/门诊退药.png"
DST="$IMG_DIR/5. 智慧医疗/5.1 门诊诊疗业务/5.1.3 门诊药房管理系统/门诊退药.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [40/200] 库存管理.png
SRC="$IMG_DIR/5. 智慧医疗/5.6 医疗保障业务/5.1.3.3 药房管理/库存管理.png"
DST="$IMG_DIR/5. 智慧医疗/5.1 门诊诊疗业务/5.1.3 门诊药房管理系统/库存管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [41/200] 门诊直接发药-草药.png
SRC="$IMG_DIR/5. 智慧医疗/5.6 医疗保障业务/5.6.2 中药房管理系统/门诊直接发药-草药.png"
DST="$IMG_DIR/5. 智慧医疗/5.6 医疗保障业务/5.6.2 中药房管理系统---补全完整的中药房管理功能说明/门诊直接发药-草药.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [42/200] 库存管理.png
SRC="$IMG_DIR/5.6.2 中药房管理系统/库存管理.png"
DST="$IMG_DIR/5. 智慧医疗/5.6 医疗保障业务/5.6.2 中药房管理系统---补全完整的中药房管理功能说明/库存管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [43/200] 数仓规划.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.1 数仓规划/数仓规划.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数仓规划.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [44/200] 基于数仓规划的数据分类.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.2 数据分类/基于数仓规划的数据分类.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/基于数仓规划的数据分类.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [45/200] 以列表形式与层级结构展示.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.2 数据分类/以列表形式与层级结构展示.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/以列表形式与层级结构展示.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [46/200] 分类信息展示.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.2 数据分类/分类信息展示.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/分类信息展示.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [47/200] 分类信息使用情况.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.2 数据分类/分类信息使用情况.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/分类信息使用情况.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [48/200] 分层规划模型.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.4 数据模型管理/分层规划模型.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/分层规划模型.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [49/200] 模型管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.4 数据模型管理/模型管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/模型管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [50/200] 导入生成模型.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.4 数据模型管理/导入生成模型.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/导入生成模型.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [51/200] ER图展现模型关联关系.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.4 数据模型管理/ER图展现模型关联关系.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/ER图展现模型关联关系.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [52/200] 结构化数据库数据源.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1 离线采集/结构化数据库数据源.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/结构化数据库数据源.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [53/200] 多模态数据源.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1 离线采集/多模态数据源.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/多模态数据源.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [54/200] ETL图形化界面.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1 离线采集/ETL图形化界面.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/ETL图形化界面.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [55/200] 质量规则.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.1 数据质量/质量规则.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/质量规则.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [56/200] 规则配置.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.1 数据质量/规则配置.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/规则配置.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [57/200] 质量监控.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.1 数据质量/质量监控.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/质量监控.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [58/200] 质量报告.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.1 数据质量/质量报告.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/质量报告.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [59/200] 比对任务定义.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.2 数据比对/比对任务定义.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/比对任务定义.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [60/200] 比对表结构查看.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.2 数据比对/比对表结构查看.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/比对表结构查看.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [61/200] 比对任务配置.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.2 数据比对/比对任务配置.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/比对任务配置.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [62/200] 比对资源添加.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.2 数据比对/比对资源添加.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/比对资源添加.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [63/200] 比对任务监控.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.2 数据比对/比对任务监控.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/比对任务监控.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [64/200] 比对任务详情.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.2 数据比对/比对任务详情.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/比对任务详情.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [65/200] 比对差异数据明细.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.2 数据比对/比对差异数据明细.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/比对差异数据明细.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [66/200] 比对差异数据找平.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.2 数据比对/比对差异数据找平.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/比对差异数据找平.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [67/200] 服务管理列表.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.1 服务配置管理/服务管理列表.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务管理列表.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [68/200] 服务类型.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.1 服务配置管理/服务类型.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务类型.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [69/200] 服务管控-日志管控、超时管控、限流管控、调用计数管控.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.1 服务配置管理/服务管控-日志管控、超时管控、限流管控、调用计数管控.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务管控-日志管控、超时管控、限流管控、调用计数管控.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [70/200] 服务管控-熔断管控、数据安全管控、加密管控..jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.1 服务配置管理/服务管控-熔断管控、数据安全管控、加密管控..jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务管控-熔断管控、数据安全管控、加密管控..jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [71/200] 服务运维调用日志.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.2 服务运维管理/服务运维调用日志.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务运维调用日志.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [72/200] 服务运维调用日志详情.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.2 服务运维管理/服务运维调用日志详情.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务运维调用日志详情.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [73/200] 服务运维调用监控.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.2 服务运维管理/服务运维调用监控.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务运维调用监控.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [74/200] 服务申请信息.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.3 服务申请管理/服务申请信息.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务申请信息.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [75/200] 服务申请审批.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.3 服务申请管理/服务申请审批.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务申请审批.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [76/200] 服务申请记录.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.3 服务申请管理/服务申请记录.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务申请记录.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [77/200] 服务授权管理.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.4.4 服务授权管理/服务授权管理.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/服务授权管理.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [78/200] 数据资产.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.5.1 共享中心/数据资产.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据资产.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [79/200] 数据资产-资源查看.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.5.1 共享中心/数据资产-资源查看.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据资产-资源查看.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [80/200] 数据资产-批量订阅.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.5.1 共享中心/数据资产-批量订阅.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据资产-批量订阅.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [81/200] 数据资产-资源订阅有效期.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.5.1 共享中心/数据资产-资源订阅有效期.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据资产-资源订阅有效期.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [82/200] 数据资产-资产迁移.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.5.1 共享中心/数据资产-资产迁移.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据资产-资产迁移.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [83/200] 数据资产-多种数据资源.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.5.1 共享中心/数据资产-多种数据资源.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据资产-多种数据资源.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [84/200] 订阅管理.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.5.2 订阅管理/订阅管理.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/订阅管理.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [85/200] 订阅管理-内数外借.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.5.2 订阅管理/订阅管理-内数外借.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/订阅管理-内数外借.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [86/200] 订阅管理-退订&历史记录.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.5.2 订阅管理/订阅管理-退订&历史记录.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/订阅管理-退订&历史记录.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [87/200] 采集监控大屏.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.3 数据采集监控大屏/采集监控大屏.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/采集监控大屏.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [88/200] 资产监控大屏.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.4 数据资产监控大屏/资产监控大屏.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/资产监控大屏.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [89/200] 存储不同类型文档.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.9.1 数据存储/存储不同类型文档.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/存储不同类型文档.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [90/200] 数据存储.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.9.1 数据存储/数据存储.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据存储.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [91/200] 分类管理.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.9.2 分类管理/分类管理.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/分类管理.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [92/200] 文档检索.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.9.3 检索服务/文档检索.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/文档检索.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [93/200] 文档调阅.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.9.4 调阅服务/文档调阅.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/文档调阅.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [94/200] 版本履历查看.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.9.5 版本履历/版本履历查看.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/版本履历查看.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [95/200] 全景视图时间轴.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.1.1 全景视图时间轴展示/全景视图时间轴.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/全景视图时间轴.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [96/200] 全景视图.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.1.2 全景视图展示/全景视图.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/全景视图.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [97/200] 全景视图-病历详细信息.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.1.2 全景视图展示/全景视图-病历详细信息.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/全景视图-病历详细信息.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [98/200] 全景视图-检验报告PDF.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.1.2 全景视图展示/全景视图-检验报告PDF.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/全景视图-检验报告PDF.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [99/200] 全景视图-检查报告详细信息.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.1.2 全景视图展示/全景视图-检查报告详细信息.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/全景视图-检查报告详细信息.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [100/200] 全景视图-检验报告详细信息.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.1.2 全景视图展示/全景视图-检验报告详细信息.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/全景视图-检验报告详细信息.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [101/200] 全景视图-药品医嘱详细信息.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.1.2 全景视图展示/全景视图-药品医嘱详细信息.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/全景视图-药品医嘱详细信息.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [102/200] 全景视图-医嘱手术信息.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.1.2 全景视图展示/全景视图-医嘱手术信息.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/全景视图-医嘱手术信息.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [103/200] 住院视图.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.1.3 住院视图展示/住院视图.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/住院视图.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [104/200] 就诊信息详情.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.1 就诊信息详情展示/就诊信息详情.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/就诊信息详情.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [105/200] 诊断信息.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.2 患者诊断信息展示/诊断信息.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/诊断信息.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [106/200] 主诉信息.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.3 主诉信息展示/主诉信息.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/主诉信息.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [107/200] 现病史.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.4 现病史展示/现病史.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/现病史.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [108/200] 输血记录.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.5 输血记录展示/输血记录.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/输血记录.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [109/200] 手术记录.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.6 手术记录展示/手术记录.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/手术记录.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [110/200] 医嘱信息.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.7 医嘱信息展示/医嘱信息.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/医嘱信息.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [111/200] 检验报告.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.8 检验报告展示/检验报告.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/检验报告.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [112/200] 检验报告趋势.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.8 检验报告展示/检验报告趋势.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/检验报告趋势.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [113/200] 检查报告.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.9 检查报告展示/检查报告.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/检查报告.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [114/200] 过敏信息.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.12 过敏信息展示/过敏信息.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/过敏信息.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [115/200] 病历文书.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.4.2.2.13 病历文书展示/病历文书.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.4 数据产品/1.3.1 患者360诊疗信息查询系统/病历文书.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [116/200] 费用查询与清单.png
SRC="$IMG_DIR/5. 智慧医疗/5.3 住院诊疗业务/5.3.7.2.6 费用查询与清单/费用查询与清单.png"
DST="$IMG_DIR/5. 智慧医疗/5.3 住院诊疗业务/5.3.7 住院护士站/费用查询与清单.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [117/200] 标准检索.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.1 标准检索/标准检索.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/标准检索.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [118/200] 标准检索_卡片或列表形式展现标准内容.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.1 标准检索/标准检索_卡片或列表形式展现标准内容.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/标准检索_卡片或列表形式展现标准内容.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [119/200] 下钻信息展示.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.1 标准检索/下钻信息展示.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/下钻信息展示.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [120/200] 术语新增.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.2 标准管理/术语新增.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/术语新增.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [121/200] 标准文件上传.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.2 标准管理/标准文件上传.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/标准文件上传.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [122/200] 值域字典管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.2 标准管理/值域字典管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/值域字典管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [123/200] 数据元管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.2 标准管理/数据元管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据元管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [124/200] 数据集管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.2 标准管理/数据集管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据集管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [125/200] 落标任务管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.3 落标管理/落标任务管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/落标任务管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [126/200] 落标任务执行情况.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.3 落标管理/落标任务执行情况.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/落标任务执行情况.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [127/200] 手动维护落标.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.3 落标管理/手动维护落标.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/手动维护落标.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [128/200] 落标结果分析展示.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.3 落标管理/落标结果分析展示.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/落标结果分析展示.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [129/200] 值域管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.4 值域对照/值域管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/值域管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [130/200] 模板管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.5 模板管理/模板管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/模板管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [131/200] 模板管理属性管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.1.3.5 模板管理/模板管理属性管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/模板管理属性管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [132/200] 图形化拖拽式流程定义.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1.1 离线任务管理/图形化拖拽式流程定义.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/图形化拖拽式流程定义.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [133/200] 抽取方式可灵活配置.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1.1 离线任务管理/抽取方式可灵活配置.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/抽取方式可灵活配置.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [134/200] 清洗组件.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1.1 离线任务管理/清洗组件.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/清洗组件.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [135/200] 清洗转换工具.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1.1 离线任务管理/清洗转换工具.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/清洗转换工具.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [136/200] SQL前置及后置定义.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1.1 离线任务管理/SQL前置及后置定义.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/SQL前置及后置定义.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [137/200] 离线任务管理_多种匹配方式.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1.1 离线任务管理/离线任务管理_多种匹配方式.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/离线任务管理_多种匹配方式.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [138/200] 离线采集最大并发数设置.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1.1 离线任务管理/离线采集最大并发数设置.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/离线采集最大并发数设置.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [139/200] 任务检索及运行状态查看.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1.2 离线采集运行监控/任务检索及运行状态查看.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/任务检索及运行状态查看.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [140/200] 采集详细日志.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.1.2 离线采集运行监控/采集详细日志.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/采集详细日志.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [141/200] 采集数据范围设置.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.2.1 实时任务管理/采集数据范围设置.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/采集数据范围设置.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [142/200] 前缀后缀设置.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.2.1 实时任务管理/前缀后缀设置.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/前缀后缀设置.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [143/200] 实时任务管理_同步建表.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.2.1 实时任务管理/实时任务管理_同步建表.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/实时任务管理_同步建表.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [144/200] 源端数据源验证.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.2.1 实时任务管理/源端数据源验证.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/源端数据源验证.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [145/200] 实时采集任务管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.2.1 实时任务管理/实时采集任务管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/实时采集任务管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [146/200] 实时采集任务模糊检索.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.2.2 实时任务监控/实时采集任务模糊检索.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/实时采集任务模糊检索.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [147/200] 支持对选中任务执行批量管理操作.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.2.2 实时任务监控/支持对选中任务执行批量管理操作.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/支持对选中任务执行批量管理操作.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [148/200] 支持全程运行监控png.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.2.2 实时任务监控/支持全程运行监控png.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/支持全程运行监控png.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [149/200] 脚本管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.3.1 脚本开发/脚本管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/脚本管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [150/200] 支持SQL脚本开发.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.3.1 脚本开发/支持SQL脚本开发.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/支持SQL脚本开发.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [151/200] 图形化流程设计器.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.3.2 作业开发/图形化流程设计器.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/图形化流程设计器.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [152/200] 丰富的编排组件与控制组件.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.3.2 作业开发/丰富的编排组件与控制组件.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/丰富的编排组件与控制组件.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [153/200] 对编排作业进行管理.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.3.2 作业开发/对编排作业进行管理.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/对编排作业进行管理.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [154/200] 以列表形式清晰展示所有任务的核心信息.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.3.3 离线开发运行监控/以列表形式清晰展示所有任务的核心信息.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/以列表形式清晰展示所有任务的核心信息.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [155/200] 任务运行调度.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.3.3 离线开发运行监控/任务运行调度.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/任务运行调度.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [156/200] 节点执行日志.png
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.2.3.3 离线开发运行监控/节点执行日志.png"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/节点执行日志.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [157/200] 采集监控.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/采集监控.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/采集监控.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [158/200] 采集任务调度.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/采集任务调度.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/采集任务调度.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [159/200] 采集任务日志.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/采集任务日志.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/采集任务日志.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [160/200] 元数据检索主页.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/元数据检索主页.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/元数据检索主页.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [161/200] 元数据检索结果.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/元数据检索结果.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/元数据检索结果.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [162/200] 元数据信息维护.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/元数据信息维护.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/元数据信息维护.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [163/200] 血缘关系.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/血缘关系.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/血缘关系.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [164/200] 血缘关系维护.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/血缘关系维护.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/血缘关系维护.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [165/200] 元数据发布.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/元数据发布.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/元数据发布.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [166/200] 元数据打标.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.1 元数据库/元数据打标.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/元数据打标.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [167/200] 数据图谱.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.2 数据图谱/数据图谱.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据图谱.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [168/200] 数据图谱节点明细.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.2 数据图谱/数据图谱节点明细.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据图谱节点明细.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [169/200] 标签分类管理.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.3 配置管理/标签分类管理.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/标签分类管理.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [170/200] 标签列表.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.3.3 配置管理/标签列表.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/标签列表.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [171/200] 数据分级列表.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.1 数据分级管理/数据分级列表.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据分级列表.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [172/200] 数据分级权限.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.1 数据分级管理/数据分级权限.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据分级权限.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [173/200] 敏感数据分类列表.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.2 敏感数据分类/敏感数据分类列表.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感数据分类列表.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [174/200] 敏感数据规则.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.2 敏感数据分类/敏感数据规则.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感数据规则.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [175/200] 脱敏规则列表.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.3 脱敏规则管理/脱敏规则列表.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/脱敏规则列表.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [176/200] 脱敏测试.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.3 脱敏规则管理/脱敏测试.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/脱敏测试.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [177/200] 脱敏算法.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.3 脱敏规则管理/脱敏算法.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/脱敏算法.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [178/200] 敏感识别规则列表.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.4 敏感识别规则/敏感识别规则列表.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感识别规则列表.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [179/200] 敏感规则与数据密级关联.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.4 敏感识别规则/敏感规则与数据密级关联.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感规则与数据密级关联.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [180/200] 敏感识别规则新增.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.4 敏感识别规则/敏感识别规则新增.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感识别规则新增.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [181/200] 敏感识别任务.jpg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.5 敏感识别管理/敏感识别任务.jpg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感识别任务.jpg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [182/200] 敏感识别任务配置.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.5 敏感识别管理/敏感识别任务配置.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感识别任务配置.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [183/200] 敏感任务监控.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.5 敏感识别管理/敏感任务监控.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感任务监控.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [184/200] 敏感任务详情.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.5 敏感识别管理/敏感任务详情.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感任务详情.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [185/200] 敏感数据管理.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.6 敏感数据管理/敏感数据管理.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/敏感数据管理.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [186/200] 数据加密算法列表.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.7 数据加密管理/数据加密算法列表.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据加密算法列表.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [187/200] 数据安全密钥与算法绑定.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.7 数据加密管理/数据安全密钥与算法绑定.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据安全密钥与算法绑定.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [188/200] 数据加密算法介绍.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.7 数据加密管理/数据加密算法介绍.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据加密算法介绍.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [189/200] 数据加密示例代码.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.3.4.7 数据加密管理/数据加密示例代码.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/数据加密示例代码.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [190/200] 运维大盘.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.1.1 运维大盘/运维大盘.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/运维大盘.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [191/200] 运维监控.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.1.2 运维监控/运维监控.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/运维监控.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [192/200] 调度任务-维度筛选.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.2.1 调度任务/调度任务-维度筛选.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/调度任务-维度筛选.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [193/200] 调度任务-信息展示.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.2.1 调度任务/调度任务-信息展示.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/调度任务-信息展示.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [194/200] 调度配置.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.2.1 调度任务/调度配置.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/调度配置.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [195/200] 调度日志.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.2.1 调度任务/调度日志.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/调度日志.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [196/200] 调度日志详情.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.2.2 调度日志/调度日志详情.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/调度日志详情.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [197/200] 调度周期.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.2.3 调度周期/调度周期.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/调度周期.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [198/200] 修改调度周期.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.2.3 调度周期/修改调度周期.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/修改调度周期.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [199/200] 调度全景.jpeg
SRC="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1.6.2.4 调度全景/调度全景.jpeg"
DST="$IMG_DIR/1. 数智底座-数据/1.1 大数据平台/1.1.1 数据资源管理平台/调度全景.jpeg"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

# [200/200] 双向转诊进度跟踪.png
SRC="$IMG_DIR/9. 智慧医联/9.1 区域医疗服务协同/9.1.3.2.3.4 双向转诊进度跟踪/双向转诊进度跟踪.png"
DST="$IMG_DIR/9. 智慧医联/9.1 区域医疗服务协同/9.1.3 转诊会诊资源共享中心/双向转诊进度跟踪.png"
if [ -f "$SRC" ]; then
  mkdir -p "$(dirname "$DST")"
  if [ -f "$DST" ]; then
    echo "跳过(目标已存在): $DST"
    SKIPPED=$((SKIPPED+1))
  else
    mv "$SRC" "$DST"
    MOVED=$((MOVED+1))
  fi
else
  echo "跳过(源不存在): $SRC"
  SKIPPED=$((SKIPPED+1))
fi

echo ""
echo "=== 移动完成 ==="
echo "已移动: $MOVED 个文件"
echo "跳过: $SKIPPED 个文件"

echo ""
echo "正在清理空目录..."
find "$IMG_DIR" -depth -type d -empty -not -path "$IMG_DIR" | tac | while read dir; do
  rmdir "$dir" 2>/dev/null && echo "  已删除: $dir" || true
done
echo "清理完成"