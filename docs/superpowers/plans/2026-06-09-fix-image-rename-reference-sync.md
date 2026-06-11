# 修复：图床改名后自定义编辑器图片丢失

## 问题
图床中对图片改名后，自定义编辑器中的图片丢失（无法显示）。

## 根因
DataEntry 中图片卡片引用的 URL（`data-url`、`img src`）与 image_resource 表中的 URL 格式不一致：
- image_resource: `/api/images/file/1/1. 数智底座-数据/.../数仓规划1.png`
- DataEntry 引用: `/api/images/file/1/1/1. 数智底座-数据/.../数仓规划1_1.png`（多了 `/1/`，文件名不同）
- 还有外部 URL: `http://cloudimgs.jscloud.vip:16666/api/images/...`

`syncImageNameInReferences` 用 `desc.replace(oldUrl, newUrl)` 替换 URL，但由于 URL 不一致，替换不生效。物理文件已改名，旧 URL 返回 404。

## 修复方案
**改用 `data-id` 精确匹配**：DataEntry 中的 image-card 都有 `data-id="<imageId>"` 属性，imageId 改名时不变。

### 核心变更

#### 1. `ImageResourceService.syncImageNameInReferences` 重写
- 入参改为 `(Long imageId, String newUrl, String newName)`
- 定向查询：`findByColFeatureDescContaining("data-id=\"" + imageId + "\"")`
- 找到后用正则匹配整个 `image-card` 块，替换其中的 `data-url`、`img src`、`data-filename`、`alt`、`title`、`image-name`

#### 2. 调用处 `update()` 方法
- Phase 3 调用改为 `syncImageNameInReferences(image.getId(), image.getUrl(), image.getFilename())`

### 涉及文件
- `src/main/java/com/superpower/modules/image/service/ImageResourceService.java`

### 详细步骤

1. 修改 `syncImageNameInReferences` 方法签名和实现
2. 修改 `update()` 中调用 `syncImageNameInReferences` 的传参
3. 编译验证 → 重启服务 → 测试

### 替换策略
用正则匹配 image-card 标签中包含 `data-id="<imageId>"` 的块，然后：
- 替换 `data-url="旧值"` → `data-url="新URL"`
- 替换 `img src="旧值"` → `img src="新URL"` （注意 src 在 image-thumb 子元素中）
- 替换 `data-filename="旧值"` → `data-filename="新名"`
- 替换 `alt="旧值"` → `alt="新名"`
- 替换 `title="旧值"` → `title="新名"`
- 替换 `<span class="image-name">旧名</span>` → 新名

由于这些属性都在同一个 image-card 块内，直接在整个 desc 上按属性名替换即可（无需先提取 card 块），但要确保只替换 `data-id` 匹配的 card 内的内容。

更精确的做法：先正则提取包含 `data-id="<imageId>"` 的 image-card 块，在块内做替换，再整体 replace回去。
