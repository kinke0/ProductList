#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
V1.0.9 修复版本3中图片目录超L3层级的问题

问题：版本3中有200条 image_resource 记录的 product 字段不是L3产品名称，
导致物理目录结构为 {category}/{domain}/{L4+名称}/ 而非 {category}/{domain}/{L3名称}/。

修复内容：
1. image_resource.product  → 改为L3祖先名称
2. image_resource.url      → 将错误product路径替换为L3路径
3. image_resource.path     → 将错误product路径替换为L3路径
4. image_resource.category/domain → 修正空值记录
5. data_entry.col_功能说明 → 替换描述中的错误URL为正确URL
6. 物理图片文件移到正确L3目录

使用方法：
  python3 V1.0.9_fix_l3_image_directory.py --db /path/to/superpower.db --uploads /path/to/uploads/images [--dry-run] [--confirm]

参数说明：
  --db       数据库文件路径（默认 ./superpower.db）
  --uploads  图片上传目录路径（默认 ./uploads/images）
  --dry-run  只打印计划，不执行任何修改
  --confirm  确认执行（不加此参数不会实际修改）
"""

import sqlite3
import os
import sys
import argparse
import shutil
from urllib.parse import quote


def find_l3_ancestor(conn, entry_id):
    """通过 entry_id 向上遍历 parent_id 找到 level=3 的祖先，返回 (l3_name, l3_domain, l3_category) 或 None"""
    cur_id = entry_id
    chain = []
    for _ in range(20):
        row = conn.execute(
            'SELECT id, level, parent_id, "col_产品系统", "col_业务域", "col_业务分类" FROM data_entry WHERE id=?',
            (cur_id,)
        ).fetchone()
        if not row:
            return None
        chain.append(row)
        if row[1] == 3:
            # L3 条目：用自身的 col_业务域 和 col_业务分类
            return (row[3], row[4], row[5])
        if row[2] is None:
            return None
        cur_id = row[2]
    return None


def find_entry_by_product(conn, product, version_id=3):
    """根据 product 名称查找 data_entry"""
    return conn.execute(
        'SELECT id, level, parent_id, "col_产品系统" FROM data_entry '
        'WHERE version_id=? AND "col_产品系统"=? LIMIT 1',
        (version_id, product)
    ).fetchone()


# 无法通过 data_entry 找到L3祖先的特殊映射（手动确认过）
MANUAL_L3_MAP = {
    '1.4.4 管理决策支持系统': {
        'l3': '1.4.4 医院管理决策支持系统',
        'category': '1. 数智底座-数据',
        'domain': '1.4 数据产品',
    },
    '5.3.7.2.6 费用查询与清单': {
        'l3': '5.3.7 住院护士站',
        'category': '5. 智慧医疗',
        'domain': '5.3 住院诊疗业务',
    },
    '5.6.2 中药房管理系统': {
        'l3': '5.6.2 中药房管理系统---补全完整的中药房管理功能说明',
        'category': '5. 智慧医疗',
        'domain': '5.6 医疗保障业务',
    },
    '9.1.3.2.3.4 双向转诊进度跟踪': {
        'l3': '9.1.3 转诊会诊资源共享中心',
        'category': '9. 智慧医联',
        'domain': '9.1 区域医疗服务协同',
    },
}


def build_new_url(old_url, new_cat, new_dom, new_product, version_id=3):
    """构建新的 URL：完全重建 cat/domain/product 路径"""
    if not old_url:
        return None
    old_prefix = '/api/images/file/{}/'.format(version_id)
    if not old_url.startswith(old_prefix):
        return old_url
    rest = old_url[len(old_prefix):]
    parts = rest.split('/')
    if len(parts) >= 4:
        filename = '/'.join(parts[3:])
        return '{}{}/{}/{}/{}'.format(old_prefix, new_cat, new_dom, new_product, filename)
    elif len(parts) >= 1:
        filename = parts[-1]
        return '{}{}/{}/{}/{}'.format(old_prefix, new_cat, new_dom, new_product, filename)
    return old_url


def build_new_path(old_path, new_cat, new_dom, new_product, storage_path, version_id=3, db_dir='.'):
    """构建新的物理路径：完全重建 cat/domain/product 路径
    
    返回值格式与 old_path 保持一致（绝对路径返回绝对，相对路径返回相对）
    """
    if not old_path:
        return None

    is_relative = not os.path.isabs(old_path)

    # 相对路径先基于数据库目录解析为绝对路径
    normalized = old_path.replace('\\', '/')
    if is_relative:
        normalized = os.path.normpath(os.path.join(db_dir, normalized)).replace('\\', '/')

    prefix = storage_path.replace('\\', '/')
    if not prefix.endswith('/'):
        prefix += '/'
    prefix += str(version_id) + '/'

    if not normalized.startswith(prefix):
        return old_path

    rest = normalized[len(prefix):]
    parts = rest.split('/')
    if len(parts) >= 4:
        filename = '/'.join(parts[3:])
        result = '{}{}/{}/{}/{}'.format(prefix, new_cat, new_dom, new_product, filename)
    elif len(parts) >= 1:
        filename = parts[-1]
        result = '{}{}/{}/{}/{}'.format(prefix, new_cat, new_dom, new_product, filename)
    else:
        return old_path

    # 如果原始是相对路径，返回相对格式
    if is_relative:
        rel = os.path.relpath(result, db_dir).replace('\\', '/')
        return './' + rel

    return result


def main():
    parser = argparse.ArgumentParser(description='修复版本3图片目录超L3层级问题')
    parser.add_argument('--db', default='./superpower.db', help='数据库文件路径')
    parser.add_argument('--uploads', default='./uploads/images', help='图片上传目录')
    parser.add_argument('--dry-run', action='store_true', help='只打印计划')
    parser.add_argument('--confirm', action='store_true', help='确认执行')
    parser.add_argument('--version-id', type=int, default=3, help='版本ID（默认3）')
    args = parser.parse_args()

    if not args.dry_run and not args.confirm:
        print('错误：请加 --dry-run 预览 或 --confirm 确认执行')
        sys.exit(1)

    if not os.path.exists(args.db):
        print(f'错误：数据库文件不存在: {args.db}')
        sys.exit(1)

    conn = sqlite3.connect(args.db)
    conn.row_factory = sqlite3.Row

    version_id = args.version_id
    storage_path = os.path.abspath(args.uploads)
    db_dir = os.path.dirname(os.path.abspath(args.db))

    # 获取所有L3名称集合
    l3_names = set(r[0] for r in conn.execute(
        'SELECT DISTINCT "col_产品系统" FROM data_entry WHERE version_id=? AND level=3',
        (version_id,)
    ).fetchall())

    # 查询所有需要修复的图片记录
    wrong_images = conn.execute('''
        SELECT id, category, domain, product, filename, url, path
        FROM image_resource
        WHERE version_id = ?
        AND product NOT IN (SELECT "col_产品系统" FROM data_entry WHERE version_id=? AND level=3)
        ORDER BY id
    ''', (version_id, version_id)).fetchall()

    print(f'=== 修复版本{version_id}图片目录超L3层级 ===')
    print(f'数据库: {args.db}')
    print(f'图片目录: {storage_path}')
    print(f'需要修复的图片记录: {len(wrong_images)} 条')
    print()

    # Step 1: 计算每条记录的修复方案
    fixes = []
    errors = []
    url_replacements = {}

    for img in wrong_images:
        img_id = img['id']
        old_product = img['product']
        old_cat = img['category']
        old_dom = img['domain']
        old_url = img['url']
        old_path = img['path']
        filename = img['filename']

        # 确定L3名称
        l3_name = None
        fix_cat = old_cat
        fix_dom = old_dom

        if old_product in MANUAL_L3_MAP:
            manual = MANUAL_L3_MAP[old_product]
            l3_name = manual['l3']
            if not fix_cat:
                fix_cat = manual['category']
            if not fix_dom:
                fix_dom = manual['domain']
        else:
            entry = find_entry_by_product(conn, old_product, version_id)
            if entry:
                result = find_l3_ancestor(conn, entry[0])
                if result:
                    l3_name = result[0]
                    # L3 条目的业务域/分类作为权威来源
                    if result[1]:
                        fix_dom = result[1]
                    if result[2]:
                        fix_cat = result[2]
            else:
                errors.append(f'ID={img_id}: product=[{old_product}] 未找到对应entry且无手动映射')
                continue

        if not l3_name:
            errors.append(f'ID={img_id}: product=[{old_product}] 无法解析L3祖先')
            continue

        # 构建新 URL 和 path（完全重建路径，同时修正 cat/domain/product）
        new_url = build_new_url(old_url, fix_cat, fix_dom, l3_name, version_id)
        new_path = build_new_path(old_path, fix_cat, fix_dom, l3_name, storage_path, version_id, db_dir)

        fix = {
            'id': img_id,
            'old_product': old_product,
            'new_product': l3_name,
            'old_cat': old_cat,
            'new_cat': fix_cat,
            'old_dom': old_dom,
            'new_dom': fix_dom,
            'old_url': old_url,
            'new_url': new_url,
            'old_path': old_path,
            'new_path': new_path,
            'filename': filename,
        }
        fixes.append(fix)

        # 记录 URL 替换映射（用于更新 data_entry 描述）
        if old_url and new_url and old_url != new_url:
            url_replacements[old_url] = new_url

    if errors:
        print('!!! 无法修复的记录:')
        for e in errors:
            print(f'  {e}')
        print()

    print(f'可修复: {len(fixes)} 条')
    print(f'URL替换映射: {len(url_replacements)} 个')
    print()

    # Step 2: 预览修复内容（按 product 分组）
    by_product = {}
    for f in fixes:
        p = f['old_product']
        if p not in by_product:
            by_product[p] = {'l3': f['new_product'], 'count': 0, 'cat': f['new_cat'], 'dom': f['new_dom']}
        by_product[p]['count'] += 1

    print('--- 按 product 分组 ---')
    for p, info in sorted(by_product.items()):
        print(f'  [{p}] ({info["count"]}张) → L3=[{info["l3"]}] cat=[{info["cat"]}] dom=[{info["dom"]}]')
    print()

    # Step 3: 检查 data_entry 中的 URL 引用
    entry_refs = []
    for old_url, new_url in url_replacements.items():
        rows = conn.execute(
            'SELECT id FROM data_entry WHERE version_id=? AND "col_功能说明" LIKE ?',
            (version_id, f'%{old_url}%')
        ).fetchall()
        for r in rows:
            entry_refs.append({'entry_id': r[0], 'old_url': old_url, 'new_url': new_url})

    print(f'--- data_entry 中需要更新URL的引用: {len(entry_refs)} 条 ---')
    for ref in entry_refs[:20]:
        print(f'  entry_id={ref["entry_id"]}: {ref["old_url"][-60:]} → {ref["new_url"][-60:]}')
    if len(entry_refs) > 20:
        print(f'  ... 还有 {len(entry_refs) - 20} 条')
    print()

    if args.dry_run:
        print('=== DRY RUN 模式，未执行任何修改 ===')

        # 在 dry run 模式下检查物理文件
        missing_files = 0
        for f in fixes:
            if f['old_path']:
                fp = f['old_path']
                if not os.path.isabs(fp):
                    fp = os.path.normpath(os.path.join(db_dir, fp))
                if not os.path.exists(fp):
                    missing_files += 1
                    if missing_files <= 5:
                        print(f'  物理文件不存在: {fp}')
        if missing_files:
            print(f'  共 {missing_files} 个物理文件不存在（移动时会跳过）')
        else:
            print('所有物理文件均存在')

        conn.close()
        return

    # Step 4: 执行修复
    print('=== 开始执行修复 ===')
    print()

    # 4.1 更新 image_resource 表
    print('[1/4] 更新 image_resource 表...')
    img_updated = 0
    for f in fixes:
        conn.execute('''
            UPDATE image_resource
            SET product=?, category=?, domain=?, url=?, path=?
            WHERE id=?
        ''', (f['new_product'], f['new_cat'], f['new_dom'], f['new_url'], f['new_path'], f['id']))
        img_updated += 1
    conn.commit()
    print(f'  已更新 {img_updated} 条 image_resource 记录')
    print()

    # 4.2 更新 data_entry 描述中的 URL
    print('[2/4] 更新 data_entry.col_功能说明 中的URL引用...')
    desc_updated = 0
    for ref in entry_refs:
        conn.execute('''
            UPDATE data_entry
            SET "col_功能说明" = REPLACE("col_功能说明", ?, ?)
            WHERE id=?
        ''', (ref['old_url'], ref['new_url'], ref['entry_id']))
        desc_updated += 1
    conn.commit()
    print(f'  已更新 {desc_updated} 条 data_entry 记录')
    print()

    # 4.3 移动物理文件
    print('[3/4] 移动物理文件...')
    moved = 0
    skipped = 0
    moved_dirs = set()

    for f in fixes:
        old_fp = f['old_path']
        new_fp = f['new_path']
        if not old_fp or not new_fp:
            skipped += 1
            continue

        # 处理相对路径
        if not os.path.isabs(old_fp):
            old_fp = os.path.join(os.path.dirname(os.path.abspath(args.db)), old_fp)
        if not os.path.isabs(new_fp):
            new_fp = os.path.join(os.path.dirname(os.path.abspath(args.db)), new_fp)

        if not os.path.exists(old_fp):
            print(f'  跳过（源文件不存在）: {old_fp}')
            skipped += 1
            continue

        if old_fp == new_fp:
            skipped += 1
            continue

        # 确保目标目录存在
        new_dir = os.path.dirname(new_fp)
        os.makedirs(new_dir, exist_ok=True)

        # 如果目标已存在，跳过（避免覆盖）
        if os.path.exists(new_fp):
            print(f'  跳过（目标已存在）: {new_fp}')
            skipped += 1
            continue

        try:
            shutil.move(old_fp, new_fp)
            moved += 1
            moved_dirs.add(os.path.dirname(old_fp))
        except Exception as e:
            print(f'  移动失败: {old_fp} → {new_fp}: {e}')
            skipped += 1

    print(f'  已移动 {moved} 个文件，跳过 {skipped} 个')
    print()

    # 4.4 清理空目录
    print('[4/4] 清理空目录...')
    cleaned = 0
    for d in sorted(moved_dirs, key=len, reverse=True):
        try:
            if os.path.isdir(d) and not os.listdir(d):
                os.rmdir(d)
                cleaned += 1
                print(f'  已删除空目录: {d}')
                # 也尝试删除父目录（如果也空了）
                parent = os.path.dirname(d)
                for _ in range(5):
                    if os.path.isdir(parent) and not os.listdir(parent):
                        os.rmdir(parent)
                        cleaned += 1
                        print(f'  已删除空目录: {parent}')
                        parent = os.path.dirname(parent)
                    else:
                        break
        except Exception as e:
            print(f'  删除目录失败: {d}: {e}')

    print(f'  已清理 {cleaned} 个空目录')
    print()

    conn.close()
    print('=== 修复完成 ===')
    print(f'  image_resource 更新: {img_updated} 条')
    print(f'  data_entry URL替换: {desc_updated} 条')
    print(f'  物理文件移动: {moved} 个')
    print(f'  空目录清理: {cleaned} 个')
    if skipped:
        print(f'  跳过: {skipped} 个')


if __name__ == '__main__':
    main()
