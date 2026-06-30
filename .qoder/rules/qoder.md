---
trigger: always_on
---
# 全局规则

- 所有对话和输出必须使用中文
- 每次收到用户消息后，在做出任何响应或执行任何操作之前，必须先调用 Skill 工具加载 `using-superpowers` 技能，检查是否有可用的技能
- 任何涉及数据库数据操作（包括但不限于增删改）前，必须向用户明确确认，列明即将执行的具体操作、涉及的表名、记录ID、关键字段值，未经用户明确确认不得执行
- 仅涉及数据表结构变更的操作，才需要在工程目录下新建专门目录（如 `db_changes/`）存储对应的变更脚本文件，脚本文件名必须以对应的研发版本号标识开头（如 `V1.0.4_add_image_width_height.sql`），确保与 VERSION.md 中的版本号对应
- 在分析和调试问题时，必须将思考过程和分析结论展示给用户，不要只在后台思考而不输出
- 每次修改代码前，必须使用CodeGraph了解清楚代码的整体结构及影响后再进行修正。

<!-- CODEGRAPH_START -->
## CodeGraph

In repositories indexed by CodeGraph (a `.codegraph/` directory exists at the repo root), reach for it BEFORE grep/find or reading files when you need to understand or locate code:

- **MCP tools** (when available): `codegraph_explore` answers most code questions in one call — the relevant symbols' verbatim source plus the call paths between them. `codegraph_node` returns one symbol's source + callers, or reads a whole file with line numbers. If the tools are listed but deferred, load them by name via tool search.
- **Shell** (always works): `codegraph explore "<symbol names or question>"` and `codegraph node <symbol-or-file>` print the same output.

If there is no `.codegraph/` directory, skip CodeGraph entirely — indexing is the user's decision.
<!-- CODEGRAPH_END -->
