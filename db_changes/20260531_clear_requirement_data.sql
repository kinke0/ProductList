-- 清空需求清单数据库，仅清除需求相关数据
-- 执行时间: 2026-05-31
-- 涉及表: req_item, req_log, image_resource(仅需求)
-- 文件删除: uploads/requirements/ 目录

DELETE FROM req_log;
DELETE FROM req_item;
DELETE FROM image_resource WHERE category = '需求管理';

-- 同时需要手动删除 uploads/requirements/ 目录下的图片文件:
-- rm -rf uploads/requirements/*
