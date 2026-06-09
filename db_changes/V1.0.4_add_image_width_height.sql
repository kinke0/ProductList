-- ============================================================
-- V1.0.4 数据库表结构变更
-- 日期: 2026-06-08
-- 说明: V1.0.4 为 image_resource 表新增 width 和 height 字段，用于存储图片宽高信息
--       预览时直接从数据库读取宽高，不再逐张 HTTP 下载图片获取尺寸，提升预览性能
--       已有图片的宽高数据通过启动时异步任务自动回填（ImageResourceService.backfillImageDimensions）
-- ============================================================

-- 1. image_resource 表新增 width 字段（图片宽度，单位像素）
ALTER TABLE image_resource ADD COLUMN width INTEGER;

-- 2. image_resource 表新增 height 字段（图片高度，单位像素）
ALTER TABLE image_resource ADD COLUMN height INTEGER;
