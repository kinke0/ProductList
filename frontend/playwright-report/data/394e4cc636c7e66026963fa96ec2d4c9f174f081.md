# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: product/data-list-crud.spec.ts >> 产品清单-CRUD闭环 >> 新建条目-填写名称保存成功
- Location: e2e/specs/product/data-list-crud.spec.ts:17:3

# Error details

```
TimeoutError: locator.click: Timeout 5000ms exceeded.
Call log:
  - waiting for getByRole('button', { name: '新建' })

```

# Page snapshot

```yaml
- generic [ref=e4]:
  - generic [ref=e5]:
    - generic [ref=e6]:
      - generic [ref=e7]: 添翼
      - generic [ref=e8]: PRO
      - img [ref=e11] [cursor=pointer]
    - menubar [ref=e13]:
      - menuitem "产品清单" [ref=e14] [cursor=pointer]:
        - img [ref=e16]
        - generic [ref=e18]: 产品清单
      - menuitem "需求管理" [ref=e19]:
        - generic [ref=e20] [cursor=pointer]:
          - img [ref=e22]
          - generic [ref=e24]: 需求管理
          - img [ref=e26]
      - menuitem "图床管理" [ref=e28] [cursor=pointer]:
        - img [ref=e30]
        - generic [ref=e33]: 图床管理
      - menuitem "版本管理" [ref=e34] [cursor=pointer]:
        - img [ref=e36]
        - generic [ref=e38]: 版本管理
      - menuitem "系统管理" [ref=e40]:
        - generic [ref=e41] [cursor=pointer]:
          - img [ref=e43]
          - generic [ref=e45]: 系统管理
          - img [ref=e47]
  - generic [ref=e49]:
    - generic [ref=e50]:
      - generic [ref=e51]: 产品清单
      - generic [ref=e52]:
        - button "需求录入" [ref=e53] [cursor=pointer]:
          - generic [ref=e54]: 需求录入
        - button "金鹤" [ref=e56] [cursor=pointer]:
          - text: 金鹤
          - img [ref=e58]
    - generic [ref=e61]:
      - generic [ref=e62]:
        - generic [ref=e63]:
          - generic [ref=e64]: v1.3
          - generic [ref=e66]: 编辑中
        - button "切换版本" [ref=e67] [cursor=pointer]:
          - generic [ref=e68]: 切换版本
      - generic [ref=e71]:
        - tablist [ref=e75]:
          - tab "产品全景图" [ref=e77]
          - tab "统计视图" [ref=e78]
          - tab "数据清单" [active] [selected] [ref=e79] [cursor=pointer]
          - tab "曜系列" [ref=e80]:
            - generic [ref=e81]: 曜系列
            - img [ref=e83]
          - tab "内部可交付清单" [ref=e85]:
            - generic [ref=e86]: 内部可交付清单
            - img [ref=e88]
          - tab "方案全量清单" [ref=e90]:
            - generic [ref=e91]: 方案全量清单
            - img [ref=e93]
          - tab "添翼外采系统清单" [ref=e95]:
            - generic [ref=e96]: 添翼外采系统清单
            - img [ref=e98]
          - tab "添加清单" [ref=e100] [cursor=pointer]:
            - generic [ref=e101]:
              - img [ref=e103]
              - text: 添加清单
        - tabpanel "数据清单" [ref=e106]:
          - generic [ref=e107]:
            - generic [ref=e108]:
              - generic "收起导航" [ref=e109] [cursor=pointer]:
                - img [ref=e111]
              - generic [ref=e114]:
                - generic [ref=e115]: 层级导航
                - generic [ref=e118]:
                  - img [ref=e121]
                  - textbox "搜索分类/领域" [ref=e123]
                - generic [ref=e125] [cursor=pointer]: 全部
                - tree [ref=e126]:
                  - treeitem "1. 数智底座-数据" [expanded] [ref=e127]:
                    - generic [ref=e128] [cursor=pointer]:
                      - img [ref=e130]
                      - generic [ref=e132]: 1. 数智底座-数据
                    - group [ref=e133]:
                      - treeitem "1.1 大数据平台" [expanded] [ref=e134]:
                        - generic [ref=e136] [cursor=pointer]: 1.1 大数据平台
                        - group
                      - treeitem "1.2 数据空间" [expanded] [ref=e137]:
                        - generic [ref=e139] [cursor=pointer]: 1.2 数据空间
                        - group
                      - treeitem "1.3 数据应用" [expanded] [ref=e140]:
                        - generic [ref=e142] [cursor=pointer]: 1.3 数据应用
                        - group
                      - treeitem "1.4 数据产品" [expanded] [ref=e143]:
                        - generic [ref=e145] [cursor=pointer]: 1.4 数据产品
                        - group
                  - treeitem "3. 数智底座-人工智能" [expanded] [ref=e146]:
                    - generic [ref=e147] [cursor=pointer]:
                      - img [ref=e149]
                      - generic [ref=e151]: 3. 数智底座-人工智能
                    - group [ref=e152]:
                      - treeitem "3.1 AI智能平台" [expanded] [ref=e153]:
                        - generic [ref=e155] [cursor=pointer]: 3.1 AI智能平台
                        - group
                  - treeitem "4. 数智底座-技术" [expanded] [ref=e156]:
                    - generic [ref=e157] [cursor=pointer]:
                      - img [ref=e159]
                      - generic [ref=e161]: 4. 数智底座-技术
                    - group [ref=e162]:
                      - treeitem "4.1 技术开发" [expanded] [ref=e163]:
                        - generic [ref=e165] [cursor=pointer]: 4.1 技术开发
                        - group
                      - treeitem "4.2 系统管理" [expanded] [ref=e166]:
                        - generic [ref=e168] [cursor=pointer]: 4.2 系统管理
                        - group
                      - treeitem "4.3 信息集成" [expanded] [ref=e169]:
                        - generic [ref=e171] [cursor=pointer]: 4.3 信息集成
                        - group
                  - treeitem "5. 智慧医疗" [expanded] [ref=e172]:
                    - generic [ref=e173] [cursor=pointer]:
                      - img [ref=e175]
                      - generic [ref=e177]: 5. 智慧医疗
                    - group [ref=e178]:
                      - treeitem "5.1 门诊诊疗业务" [expanded] [ref=e179]:
                        - generic [ref=e181] [cursor=pointer]: 5.1 门诊诊疗业务
                        - group
                      - treeitem "5.2 急诊诊疗业务" [expanded] [ref=e182]:
                        - generic [ref=e184] [cursor=pointer]: 5.2 急诊诊疗业务
                        - group
                      - treeitem "5.3 住院诊疗业务" [expanded] [ref=e185]:
                        - generic [ref=e187] [cursor=pointer]: 5.3 住院诊疗业务
                        - group
                      - treeitem "5.4 辅助诊断业务" [expanded] [ref=e188]:
                        - generic [ref=e190] [cursor=pointer]: 5.4 辅助诊断业务
                        - group
                      - treeitem "5.5 治疗业务" [expanded] [ref=e191]:
                        - generic [ref=e193] [cursor=pointer]: 5.5 治疗业务
                        - group
                      - treeitem "5.6 医疗保障业务" [expanded] [ref=e194]:
                        - generic [ref=e196] [cursor=pointer]: 5.6 医疗保障业务
                        - group
                      - treeitem "5.7 病案管理业务" [expanded] [ref=e197]:
                        - generic [ref=e199] [cursor=pointer]: 5.7 病案管理业务
                        - group
                      - treeitem "5.8 护理管理业务" [expanded] [ref=e200]:
                        - generic [ref=e202] [cursor=pointer]: 5.8 护理管理业务
                        - group
                      - treeitem "5.9 医疗质量业务" [expanded] [ref=e203]:
                        - generic [ref=e205] [cursor=pointer]: 5.9 医疗质量业务
                        - group
                      - treeitem "5.11 专科专病业务" [expanded] [ref=e206]:
                        - generic [ref=e208] [cursor=pointer]: 5.11 专科专病业务
                        - group
                      - treeitem "5.10 医保管理业务" [expanded] [ref=e209]:
                        - generic [ref=e211] [cursor=pointer]: 5.10 医保管理业务
                        - group
                  - treeitem "6. 智慧服务" [expanded] [ref=e212]:
                    - generic [ref=e213] [cursor=pointer]:
                      - img [ref=e215]
                      - generic [ref=e217]: 6. 智慧服务
                    - group [ref=e218]:
                      - treeitem "6.1 服务资源优化" [expanded] [ref=e219]:
                        - generic [ref=e221] [cursor=pointer]: 6.1 服务资源优化
                        - group
                      - treeitem "6.2 互联网服务" [expanded] [ref=e222]:
                        - generic [ref=e224] [cursor=pointer]: 6.2 互联网服务
                        - group
                      - treeitem "6.3 医疗智能终端" [expanded] [ref=e225]:
                        - generic [ref=e227] [cursor=pointer]: 6.3 医疗智能终端
                        - group
                      - treeitem "6.4 基于病种的患者全病程管理" [expanded] [ref=e228]:
                        - generic [ref=e230] [cursor=pointer]: 6.4 基于病种的患者全病程管理
                        - group
                      - treeitem "6.5 健康促进支持" [expanded] [ref=e231]:
                        - generic [ref=e233] [cursor=pointer]: 6.5 健康促进支持
                        - group
                  - treeitem "7. 智慧管理" [expanded] [ref=e234]:
                    - generic [ref=e235] [cursor=pointer]:
                      - img [ref=e237]
                      - generic [ref=e239]: 7. 智慧管理
                    - group [ref=e240]:
                      - treeitem "7.1 人力资源管理" [expanded] [ref=e241]:
                        - generic [ref=e243] [cursor=pointer]: 7.1 人力资源管理
                        - group
                      - treeitem "7.2 财务资产管理" [expanded] [ref=e244]:
                        - generic [ref=e246] [cursor=pointer]: 7.2 财务资产管理
                        - group
                      - treeitem "7.3 设备设施管理" [expanded] [ref=e247]:
                        - generic [ref=e249] [cursor=pointer]: 7.3 设备设施管理
                        - group
                      - treeitem "7.4 药品耗材管理" [expanded] [ref=e250]:
                        - generic [ref=e252] [cursor=pointer]: 7.4 药品耗材管理
                        - group
                      - treeitem "7.5 运营管理" [expanded] [ref=e253]:
                        - generic [ref=e255] [cursor=pointer]: 7.5 运营管理
                        - group
                      - treeitem "7.6 后勤保障" [expanded] [ref=e256]:
                        - generic [ref=e258] [cursor=pointer]: 7.6 后勤保障
                        - group
                      - treeitem "7.7 办公管理" [expanded] [ref=e259]:
                        - generic [ref=e261] [cursor=pointer]: 7.7 办公管理
                        - group
                      - treeitem "7.8 科教管理" [expanded] [ref=e262]:
                        - generic [ref=e264] [cursor=pointer]: 7.8 科教管理
                        - group
                  - treeitem "8. 智慧科研" [expanded] [ref=e265]:
                    - generic [ref=e266] [cursor=pointer]:
                      - img [ref=e268]
                      - generic [ref=e270]: 8. 智慧科研
                    - group [ref=e271]:
                      - treeitem "8.1 临床科研平台" [expanded] [ref=e272]:
                        - generic [ref=e274] [cursor=pointer]: 8.1 临床科研平台
                        - group
                      - treeitem "8.2 专病数据库" [expanded] [ref=e275]:
                        - generic [ref=e277] [cursor=pointer]: 8.2 专病数据库
                        - group
                      - treeitem "8.3 科研数据应用" [expanded] [ref=e278]:
                        - generic [ref=e280] [cursor=pointer]: 8.3 科研数据应用
                        - group
                  - treeitem "9. 智慧医联" [expanded] [ref=e281]:
                    - generic [ref=e282] [cursor=pointer]:
                      - img [ref=e284]
                      - generic [ref=e286]: 9. 智慧医联
                    - group [ref=e287]:
                      - treeitem "9.1 区域医疗服务协同" [expanded] [ref=e288]:
                        - generic [ref=e290] [cursor=pointer]: 9.1 区域医疗服务协同
                        - group
                      - treeitem "9.2 便民惠民服务协同" [expanded] [ref=e291]:
                        - generic [ref=e293] [cursor=pointer]: 9.2 便民惠民服务协同
                        - group
                      - treeitem "9.3 基层医疗卫生综合管理" [expanded] [ref=e294]:
                        - generic [ref=e296] [cursor=pointer]: 9.3 基层医疗卫生综合管理
                        - group
                  - treeitem "10. 评级服务包-智慧医院5级" [expanded] [ref=e297]:
                    - generic [ref=e298] [cursor=pointer]:
                      - img [ref=e300]
                      - generic [ref=e302]: 10. 评级服务包-智慧医院5级
                    - group [ref=e303]:
                      - treeitem "10.1 大数据平台" [expanded] [ref=e304]:
                        - generic [ref=e306] [cursor=pointer]: 10.1 大数据平台
                        - group
                      - treeitem "10.2 住院业务" [expanded] [ref=e307]:
                        - generic [ref=e309] [cursor=pointer]: 10.2 住院业务
                        - group
                      - treeitem "10.3 辅助诊断业务" [expanded] [ref=e310]:
                        - generic [ref=e312] [cursor=pointer]: 10.3 辅助诊断业务
                        - group
                      - treeitem "10.4 治疗业务" [expanded] [ref=e313]:
                        - generic [ref=e315] [cursor=pointer]: 10.4 治疗业务
                        - group
                      - treeitem "10.5 医疗保障业务" [expanded] [ref=e316]:
                        - generic [ref=e318] [cursor=pointer]: 10.5 医疗保障业务
                        - group
                      - treeitem "10.6 病案管理业务" [expanded] [ref=e319]:
                        - generic [ref=e321] [cursor=pointer]: 10.6 病案管理业务
                        - group
                      - treeitem "10.7 护理管理业务" [expanded] [ref=e322]:
                        - generic [ref=e324] [cursor=pointer]: 10.7 护理管理业务
                        - group
                      - treeitem "10.8 医疗质量业务" [expanded] [ref=e325]:
                        - generic [ref=e327] [cursor=pointer]: 10.8 医疗质量业务
                        - group
                      - treeitem "10.9 区域医疗服务协调" [expanded] [ref=e328]:
                        - generic [ref=e330] [cursor=pointer]: 10.9 区域医疗服务协调
                        - group
                      - treeitem "10.10 评级指导服务" [expanded] [ref=e331]:
                        - generic [ref=e333] [cursor=pointer]: 10.10 评级指导服务
                        - group
                  - treeitem "11. 评级服务包-智慧医院6级" [expanded] [ref=e334]:
                    - generic [ref=e335] [cursor=pointer]:
                      - img [ref=e337]
                      - generic [ref=e339]: 11. 评级服务包-智慧医院6级
                    - group [ref=e340]:
                      - treeitem "11.1 住院业务" [expanded] [ref=e341]:
                        - generic [ref=e343] [cursor=pointer]: 11.1 住院业务
                        - group
                      - treeitem "11.2 护理管理业务" [expanded] [ref=e344]:
                        - generic [ref=e346] [cursor=pointer]: 11.2 护理管理业务
                        - group
                      - treeitem "11.3 门诊诊疗业务" [expanded] [ref=e347]:
                        - generic [ref=e349] [cursor=pointer]: 11.3 门诊诊疗业务
                        - group
                      - treeitem "11.4 急诊诊疗业务" [expanded] [ref=e350]:
                        - generic [ref=e352] [cursor=pointer]: 11.4 急诊诊疗业务
                        - group
                      - treeitem "11.5 医疗质量业务" [expanded] [ref=e353]:
                        - generic [ref=e355] [cursor=pointer]: 11.5 医疗质量业务
                        - group
                      - treeitem "11.6 辅助诊断业务" [expanded] [ref=e356]:
                        - generic [ref=e358] [cursor=pointer]: 11.6 辅助诊断业务
                        - group
                      - treeitem "11.7 治疗业务" [expanded] [ref=e359]:
                        - generic [ref=e361] [cursor=pointer]: 11.7 治疗业务
                        - group
                      - treeitem "11.8 服务资源优化" [expanded] [ref=e362]:
                        - generic [ref=e364] [cursor=pointer]: 11.8 服务资源优化
                        - group
                      - treeitem "11.9 医疗保障业务" [expanded] [ref=e365]:
                        - generic [ref=e367] [cursor=pointer]: 11.9 医疗保障业务
                        - group
                      - treeitem "11.10 病案管理业务" [expanded] [ref=e368]:
                        - generic [ref=e370] [cursor=pointer]: 11.10 病案管理业务
                        - group
                      - treeitem "11.11 临床数据应用" [expanded] [ref=e371]:
                        - generic [ref=e373] [cursor=pointer]: 11.11 临床数据应用
                        - group
                      - treeitem "11.12 大数据平台" [expanded] [ref=e374]:
                        - generic [ref=e376] [cursor=pointer]: 11.12 大数据平台
                        - group
                      - treeitem "11.13 AI智能平台" [expanded] [ref=e377]:
                        - generic [ref=e379] [cursor=pointer]: 11.13 AI智能平台
                        - group
                      - treeitem "11.14 区域医疗服务协同" [expanded] [ref=e380]:
                        - generic [ref=e382] [cursor=pointer]: 11.14 区域医疗服务协同
                        - group
                      - treeitem "11.15 健康促进支撑" [expanded] [ref=e383]:
                        - generic [ref=e385] [cursor=pointer]: 11.15 健康促进支撑
                        - group
                      - treeitem "11.16 报到机" [expanded] [ref=e386]:
                        - generic [ref=e388] [cursor=pointer]: 11.16 报到机
                        - group
                      - treeitem "11.17 短信平台" [expanded] [ref=e389]:
                        - generic [ref=e391] [cursor=pointer]: 11.17 短信平台
                        - group
                      - treeitem "11.18 智慧护理系统" [expanded] [ref=e392]:
                        - generic [ref=e394] [cursor=pointer]: 11.18 智慧护理系统
                        - group
                      - treeitem "11.19 评级指导服务" [expanded] [ref=e395]:
                        - generic [ref=e397] [cursor=pointer]: 11.19 评级指导服务
                        - group
                      - treeitem "11.20 闭环管理" [expanded] [ref=e398]:
                        - generic [ref=e400] [cursor=pointer]: 11.20 闭环管理
                        - group
                  - treeitem "12. 评级服务包-智慧医院7级" [expanded] [ref=e401]:
                    - generic [ref=e402] [cursor=pointer]:
                      - img [ref=e404]
                      - generic [ref=e406]: 12. 评级服务包-智慧医院7级
                    - group [ref=e407]:
                      - treeitem "12.1 住院业务" [expanded] [ref=e408]:
                        - generic [ref=e410] [cursor=pointer]: 12.1 住院业务
                        - group
                      - treeitem "12.2 护理管理业务" [expanded] [ref=e411]:
                        - generic [ref=e413] [cursor=pointer]: 12.2 护理管理业务
                        - group
                      - treeitem "12.3 急诊诊疗业务" [expanded] [ref=e414]:
                        - generic [ref=e416] [cursor=pointer]: 12.3 急诊诊疗业务
                        - group
                      - treeitem "12.4 医疗质量业务" [expanded] [ref=e417]:
                        - generic [ref=e419] [cursor=pointer]: 12.4 医疗质量业务
                        - group
                      - treeitem "12.5 辅助诊断业务" [expanded] [ref=e420]:
                        - generic [ref=e422] [cursor=pointer]: 12.5 辅助诊断业务
                        - group
                      - treeitem "12.6 治疗业务" [expanded] [ref=e423]:
                        - generic [ref=e425] [cursor=pointer]: 12.6 治疗业务
                        - group
                      - treeitem "12.7 服务资源优化" [expanded] [ref=e426]:
                        - generic [ref=e428] [cursor=pointer]: 12.7 服务资源优化
                        - group
                      - treeitem "12.8 医疗保障业务" [expanded] [ref=e429]:
                        - generic [ref=e431] [cursor=pointer]: 12.8 医疗保障业务
                        - group
                      - treeitem "12.9 病案管理业务" [expanded] [ref=e432]:
                        - generic [ref=e434] [cursor=pointer]: 12.9 病案管理业务
                        - group
                      - treeitem "12.10 临床数据应用" [expanded] [ref=e435]:
                        - generic [ref=e437] [cursor=pointer]: 12.10 临床数据应用
                        - group
                      - treeitem "12.11 大数据平台" [expanded] [ref=e438]:
                        - generic [ref=e440] [cursor=pointer]: 12.11 大数据平台
                        - group
                      - treeitem "12.12 区域医疗服务协同" [expanded] [ref=e441]:
                        - generic [ref=e443] [cursor=pointer]: 12.12 区域医疗服务协同
                        - group
                      - treeitem "12.13 健康促进支撑" [expanded] [ref=e444]:
                        - generic [ref=e446] [cursor=pointer]: 12.13 健康促进支撑
                        - group
                      - treeitem "12.14 评级指导服务" [expanded] [ref=e447]:
                        - generic [ref=e449] [cursor=pointer]: 12.14 评级指导服务
                        - group
                  - treeitem "13. 评级服务包-智慧服务3级" [expanded] [ref=e450]:
                    - generic [ref=e451] [cursor=pointer]:
                      - img [ref=e453]
                      - generic [ref=e455]: 13. 评级服务包-智慧服务3级
                    - group [ref=e456]:
                      - treeitem "13.1 详细功能清单" [expanded] [ref=e457]:
                        - generic [ref=e459] [cursor=pointer]: 13.1 详细功能清单
                        - group
                  - treeitem "14. 评级服务包-智慧服务4级" [expanded] [ref=e460]:
                    - generic [ref=e461] [cursor=pointer]:
                      - img [ref=e463]
                      - generic [ref=e465]: 14. 评级服务包-智慧服务4级
                    - group [ref=e466]:
                      - treeitem "14.1 详细功能清单" [expanded] [ref=e467]:
                        - generic [ref=e469] [cursor=pointer]: 14.1 详细功能清单
                        - group
            - generic [ref=e471]:
              - generic [ref=e473]:
                - generic [ref=e474]:
                  - generic [ref=e475]: 名称
                  - textbox "名称" [ref=e479]:
                    - /placeholder: 产品/系统名称
                - generic [ref=e481]:
                  - generic [ref=e482]: 状态
                  - generic [ref=e485] [cursor=pointer]:
                    - generic:
                      - combobox "状态" [ref=e487]
                      - generic [ref=e488]: 全部
                    - img [ref=e491]
                - generic [ref=e493]:
                  - generic [ref=e494]: 产品经理
                  - textbox "产品经理" [ref=e498]
                - generic [ref=e500]:
                  - generic [ref=e501]: 解决方案
                  - generic [ref=e504] [cursor=pointer]:
                    - generic:
                      - combobox "解决方案" [ref=e506]
                      - generic [ref=e507]: 全部
                    - img [ref=e510]
                - generic [ref=e512]:
                  - generic [ref=e513]: 版本
                  - generic [ref=e516] [cursor=pointer]:
                    - generic:
                      - combobox "版本" [ref=e518]
                      - generic [ref=e519]: 全部
                    - img [ref=e522]
                - generic [ref=e526] [cursor=pointer]:
                  - generic [ref=e527]:
                    - checkbox "智能化"
                  - generic [ref=e529]: 智能化
                - generic [ref=e531]:
                  - button "查询" [ref=e532] [cursor=pointer]:
                    - generic [ref=e533]: 查询
                  - button "重置" [ref=e534] [cursor=pointer]:
                    - generic [ref=e535]: 重置
              - generic [ref=e536]:
                - generic [ref=e537]:
                  - generic [ref=e538]: 查询结果
                  - generic [ref=e539] [cursor=pointer]:
                    - img [ref=e541]
                    - text: 全部展开
                  - generic [ref=e543]: "|"
                  - generic [ref=e544] [cursor=pointer]:
                    - img [ref=e546]
                    - text: 全部折叠
                - generic [ref=e548]:
                  - button "插入待生成清单" [ref=e549] [cursor=pointer]:
                    - generic [ref=e550]:
                      - img [ref=e552]
                      - text: 插入待生成清单
                  - button "批量提交" [ref=e554] [cursor=pointer]:
                    - generic [ref=e555]:
                      - img [ref=e557]
                      - text: 批量提交
                  - button "批量通过" [ref=e559] [cursor=pointer]:
                    - generic [ref=e560]:
                      - img [ref=e562]
                      - text: 批量通过
                  - button "批量驳回" [ref=e565] [cursor=pointer]:
                    - generic [ref=e566]:
                      - img [ref=e568]
                      - text: 批量驳回
                  - button "其他批量操作" [ref=e572] [cursor=pointer]:
                    - generic [ref=e573]:
                      - img [ref=e575]
                      - text: 其他批量操作
                      - img [ref=e579]
                  - button "编码重排序" [disabled] [ref=e581]:
                    - generic [ref=e582]:
                      - img [ref=e584]
                      - text: 编码重排序
              - generic [ref=e586]:
                - generic [ref=e587]:
                  - generic [ref=e591] [cursor=pointer]:
                    - checkbox
                  - generic [ref=e593]:
                    - text: 名称
                    - generic [ref=e594]: 303个产品 / 3614条记录
                  - generic [ref=e595]: 审批
                  - generic [ref=e596]: 状态
                  - generic [ref=e597]: 产品经理
                  - generic [ref=e598]:
                    - text: 版本划分
                    - generic [ref=e599]: 最小集
                  - generic [ref=e600]: 操作
                - generic [ref=e602]:
                  - generic [ref=e606] [cursor=pointer]:
                    - generic [ref=e607]: ▼
                    - generic [ref=e608]: 1. 数智底座-数据 - 1.1 大数据平台
                    - button "+ 添加产品/系统" [ref=e609]:
                      - generic [ref=e610]: + 添加产品/系统
                  - generic [ref=e612]:
                    - generic [ref=e614]:
                      - generic [ref=e615]: ⠿
                      - generic [ref=e617] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e620]:
                      - img [ref=e622] [cursor=pointer]
                      - generic [ref=e625]: 产品
                      - generic: 1.1.1 数据资源管理平台-编辑-编辑-编辑-编辑-编辑-编辑
                      - generic [ref=e626] [cursor=pointer]: AI
                      - generic [ref=e627]: 71条记录
                    - generic [ref=e630] [cursor=pointer]: 驳回
                    - generic [ref=e633]: 可交付
                    - generic [ref=e635]: 胡其涛
                    - generic [ref=e637]:
                      - generic [ref=e639]:
                        - generic [ref=e640] [cursor=pointer]:
                          - checkbox "曜" [disabled]
                        - generic [ref=e642]: 曜
                      - generic [ref=e644]:
                        - generic [ref=e645] [cursor=pointer]:
                          - checkbox "远" [disabled]
                        - generic [ref=e647]: 远
                      - generic [ref=e649]:
                        - generic [ref=e650] [cursor=pointer]:
                          - checkbox "驰" [disabled]
                        - generic [ref=e652]: 驰
                      - generic [ref=e653] [cursor=pointer]:
                        - generic [ref=e654]:
                          - checkbox "非标配" [checked]
                        - generic [ref=e656]: 非标配
                    - generic [ref=e657]:
                      - generic [ref=e658] [cursor=pointer]: 提交
                      - generic [ref=e660] [cursor=pointer]: 预览
                      - generic [ref=e661] [cursor=pointer]: 编辑
                      - generic [ref=e662] [cursor=pointer]: 添加
                      - generic [ref=e663] [cursor=pointer]: 删除
                  - generic [ref=e665]:
                    - generic [ref=e667]:
                      - generic [ref=e668]: ⠿
                      - generic [ref=e670] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e673]:
                      - generic [ref=e675]: 产品
                      - generic [ref=e676]: 1.1.2 数据资产管理平台-编辑-编辑
                    - generic [ref=e680]: 缺失
                    - generic [ref=e682]: 胡其涛
                    - generic [ref=e684]:
                      - generic [ref=e686] [cursor=pointer]:
                        - generic [ref=e687]:
                          - checkbox "曜"
                        - generic [ref=e689]: 曜
                      - generic [ref=e691] [cursor=pointer]:
                        - generic [ref=e692]:
                          - checkbox "远"
                        - generic [ref=e694]: 远
                      - generic [ref=e696] [cursor=pointer]:
                        - generic [ref=e697]:
                          - checkbox "驰"
                        - generic [ref=e699]: 驰
                      - generic [ref=e700] [cursor=pointer]:
                        - generic [ref=e701]:
                          - checkbox "非标配"
                        - generic [ref=e703]: 非标配
                    - generic [ref=e704]:
                      - generic [ref=e706] [cursor=pointer]: 预览
                      - generic [ref=e707] [cursor=pointer]: 编辑
                      - generic [ref=e708] [cursor=pointer]: 添加
                      - generic [ref=e709] [cursor=pointer]: 删除
                  - generic [ref=e711]:
                    - generic [ref=e713]:
                      - generic [ref=e714]: ⠿
                      - generic [ref=e716] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e719]:
                      - img [ref=e721] [cursor=pointer]
                      - generic [ref=e724]: 产品
                      - generic: 1.1.1 数据资源管理平台-编辑-编辑-编辑-编辑-编辑-编辑
                      - generic [ref=e725] [cursor=pointer]: AI
                      - generic [ref=e726]: 71条记录
                    - generic [ref=e729] [cursor=pointer]: 待提交
                    - generic [ref=e732]: 可交付
                    - generic [ref=e734]: 胡其涛
                    - generic [ref=e736]:
                      - generic [ref=e738]:
                        - generic [ref=e739] [cursor=pointer]:
                          - checkbox "曜" [disabled]
                        - generic [ref=e741]: 曜
                      - generic [ref=e743]:
                        - generic [ref=e744] [cursor=pointer]:
                          - checkbox "远" [disabled]
                        - generic [ref=e746]: 远
                      - generic [ref=e748]:
                        - generic [ref=e749] [cursor=pointer]:
                          - checkbox "驰" [disabled]
                        - generic [ref=e751]: 驰
                      - generic [ref=e752] [cursor=pointer]:
                        - generic [ref=e753]:
                          - checkbox "非标配" [checked]
                        - generic [ref=e755]: 非标配
                    - generic [ref=e756]:
                      - generic [ref=e757] [cursor=pointer]: 提交
                      - generic [ref=e759] [cursor=pointer]: 预览
                      - generic [ref=e760] [cursor=pointer]: 编辑
                      - generic [ref=e761] [cursor=pointer]: 添加
                      - generic [ref=e762] [cursor=pointer]: 删除
                  - generic [ref=e764]:
                    - generic [ref=e766]:
                      - generic [ref=e767]: ⠿
                      - generic [ref=e769] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e772]:
                      - img [ref=e774] [cursor=pointer]
                      - generic [ref=e777]: 产品
                      - generic: 1.1.1 数据资源管理平台-编辑-编辑-编辑-编辑-编辑-编辑
                      - generic [ref=e778] [cursor=pointer]: AI
                      - generic [ref=e779]: 70条记录
                    - generic [ref=e782] [cursor=pointer]: 待提交
                    - generic [ref=e785]: 可交付
                    - generic [ref=e787]: 胡其涛
                    - generic [ref=e789]:
                      - generic [ref=e791]:
                        - generic [ref=e792] [cursor=pointer]:
                          - checkbox "曜" [disabled]
                        - generic [ref=e794]: 曜
                      - generic [ref=e796]:
                        - generic [ref=e797] [cursor=pointer]:
                          - checkbox "远" [disabled]
                        - generic [ref=e799]: 远
                      - generic [ref=e801]:
                        - generic [ref=e802] [cursor=pointer]:
                          - checkbox "驰" [disabled]
                        - generic [ref=e804]: 驰
                      - generic [ref=e805] [cursor=pointer]:
                        - generic [ref=e806]:
                          - checkbox "非标配" [checked]
                        - generic [ref=e808]: 非标配
                    - generic [ref=e809]:
                      - generic [ref=e810] [cursor=pointer]: 提交
                      - generic [ref=e812] [cursor=pointer]: 预览
                      - generic [ref=e813] [cursor=pointer]: 编辑
                      - generic [ref=e814] [cursor=pointer]: 添加
                      - generic [ref=e815] [cursor=pointer]: 删除
                  - generic [ref=e817]:
                    - generic [ref=e819]:
                      - generic [ref=e820]: ⠿
                      - generic [ref=e822] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e825]:
                      - img [ref=e827] [cursor=pointer]
                      - generic [ref=e830]: 产品
                      - generic: 1.1.1 数据资源管理平台-编辑-编辑-编辑-编辑-编辑-编辑
                      - generic [ref=e831] [cursor=pointer]: AI
                      - generic [ref=e832]: 69条记录
                    - generic [ref=e835] [cursor=pointer]: 待审核
                    - generic [ref=e838]: 可交付
                    - generic [ref=e840]: 胡其涛
                    - generic [ref=e842]:
                      - generic [ref=e844]:
                        - generic [ref=e845] [cursor=pointer]:
                          - checkbox "曜" [disabled]
                        - generic [ref=e847]: 曜
                      - generic [ref=e849]:
                        - generic [ref=e850] [cursor=pointer]:
                          - checkbox "远" [disabled]
                        - generic [ref=e852]: 远
                      - generic [ref=e854]:
                        - generic [ref=e855] [cursor=pointer]:
                          - checkbox "驰" [disabled]
                        - generic [ref=e857]: 驰
                      - generic [ref=e858] [cursor=pointer]:
                        - generic [ref=e859]:
                          - checkbox "非标配" [checked]
                        - generic [ref=e861]: 非标配
                    - generic [ref=e862]:
                      - generic [ref=e863] [cursor=pointer]: 撤销
                      - generic [ref=e864] [cursor=pointer]: 通过
                      - generic [ref=e865] [cursor=pointer]: 驳回
                      - generic [ref=e867] [cursor=pointer]: 预览
                      - generic [ref=e868] [cursor=pointer]: 编辑
                      - generic [ref=e869] [cursor=pointer]: 添加
                      - generic [ref=e870] [cursor=pointer]: 删除
                  - generic [ref=e872]:
                    - generic [ref=e874]:
                      - generic [ref=e875]: ⠿
                      - generic [ref=e877] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e880]:
                      - img [ref=e882] [cursor=pointer]
                      - generic [ref=e885]: 产品
                      - generic: 1.1.1 数据资源管理平台-编辑-编辑-编辑-编辑-编辑
                      - generic [ref=e886] [cursor=pointer]: AI
                      - generic [ref=e887]: 68条记录
                    - generic [ref=e890] [cursor=pointer]: 待提交
                    - generic [ref=e893]: 可交付
                    - generic [ref=e895]: 胡其涛
                    - generic [ref=e897]:
                      - generic [ref=e899]:
                        - generic [ref=e900] [cursor=pointer]:
                          - checkbox "曜" [disabled]
                        - generic [ref=e902]: 曜
                      - generic [ref=e904]:
                        - generic [ref=e905] [cursor=pointer]:
                          - checkbox "远" [disabled]
                        - generic [ref=e907]: 远
                      - generic [ref=e909]:
                        - generic [ref=e910] [cursor=pointer]:
                          - checkbox "驰" [disabled]
                        - generic [ref=e912]: 驰
                      - generic [ref=e913] [cursor=pointer]:
                        - generic [ref=e914]:
                          - checkbox "非标配" [checked]
                        - generic [ref=e916]: 非标配
                    - generic [ref=e917]:
                      - generic [ref=e918] [cursor=pointer]: 提交
                      - generic [ref=e920] [cursor=pointer]: 预览
                      - generic [ref=e921] [cursor=pointer]: 编辑
                      - generic [ref=e922] [cursor=pointer]: 添加
                      - generic [ref=e923] [cursor=pointer]: 删除
                  - generic [ref=e925]:
                    - generic [ref=e927]:
                      - generic [ref=e928]: ⠿
                      - generic [ref=e930] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e933]:
                      - img [ref=e935] [cursor=pointer]
                      - generic [ref=e938]: 产品
                      - generic: 1.1.1 数据资源管理平台-编辑-编辑
                      - generic [ref=e939] [cursor=pointer]: AI
                      - generic [ref=e940]: 66条记录
                    - generic [ref=e943] [cursor=pointer]: 待提交
                    - generic [ref=e946]: 可交付
                    - generic [ref=e948]: 胡其涛
                    - generic [ref=e950]:
                      - generic [ref=e952]:
                        - generic [ref=e953] [cursor=pointer]:
                          - checkbox "曜" [disabled]
                        - generic [ref=e955]: 曜
                      - generic [ref=e957]:
                        - generic [ref=e958] [cursor=pointer]:
                          - checkbox "远" [disabled]
                        - generic [ref=e960]: 远
                      - generic [ref=e962]:
                        - generic [ref=e963] [cursor=pointer]:
                          - checkbox "驰" [disabled]
                        - generic [ref=e965]: 驰
                      - generic [ref=e966] [cursor=pointer]:
                        - generic [ref=e967]:
                          - checkbox "非标配" [checked]
                        - generic [ref=e969]: 非标配
                    - generic [ref=e970]:
                      - generic [ref=e971] [cursor=pointer]: 提交
                      - generic [ref=e973] [cursor=pointer]: 预览
                      - generic [ref=e974] [cursor=pointer]: 编辑
                      - generic [ref=e975] [cursor=pointer]: 添加
                      - generic [ref=e976] [cursor=pointer]: 删除
                  - generic [ref=e978]:
                    - generic [ref=e980]:
                      - generic [ref=e981]: ⠿
                      - generic [ref=e983] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e986]:
                      - img [ref=e988] [cursor=pointer]
                      - generic [ref=e991]: 产品
                      - generic: 1.1.1 数据资源管理平台-编辑
                      - generic [ref=e992] [cursor=pointer]: AI
                      - generic [ref=e993]: 65条记录
                    - generic [ref=e996] [cursor=pointer]: 待提交
                    - generic [ref=e999]: 可交付
                    - generic [ref=e1001]: 胡其涛
                    - generic [ref=e1003]:
                      - generic [ref=e1005]:
                        - generic [ref=e1006] [cursor=pointer]:
                          - checkbox "曜" [disabled]
                        - generic [ref=e1008]: 曜
                      - generic [ref=e1010]:
                        - generic [ref=e1011] [cursor=pointer]:
                          - checkbox "远" [disabled]
                        - generic [ref=e1013]: 远
                      - generic [ref=e1015]:
                        - generic [ref=e1016] [cursor=pointer]:
                          - checkbox "驰" [disabled]
                        - generic [ref=e1018]: 驰
                      - generic [ref=e1019] [cursor=pointer]:
                        - generic [ref=e1020]:
                          - checkbox "非标配" [checked]
                        - generic [ref=e1022]: 非标配
                    - generic [ref=e1023]:
                      - generic [ref=e1024] [cursor=pointer]: 提交
                      - generic [ref=e1026] [cursor=pointer]: 预览
                      - generic [ref=e1027] [cursor=pointer]: 编辑
                      - generic [ref=e1028] [cursor=pointer]: 添加
                      - generic [ref=e1029] [cursor=pointer]: 删除
                  - generic [ref=e1031]:
                    - generic [ref=e1033]:
                      - generic [ref=e1034]: ⠿
                      - generic [ref=e1036] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e1039]:
                      - img [ref=e1041] [cursor=pointer]
                      - generic [ref=e1044]: 产品
                      - generic: 1.1.1 数据资源管理平台
                      - generic [ref=e1045] [cursor=pointer]: AI
                      - generic [ref=e1046]: 64条记录
                    - generic [ref=e1049] [cursor=pointer]: 待提交
                    - generic [ref=e1052]: 可交付
                    - generic [ref=e1054]: 胡其涛
                    - generic [ref=e1056]:
                      - generic [ref=e1058] [cursor=pointer]:
                        - generic [ref=e1059]:
                          - checkbox "曜"
                        - generic [ref=e1061]: 曜
                      - generic [ref=e1063] [cursor=pointer]:
                        - generic [ref=e1064]:
                          - checkbox "远" [checked]
                        - generic [ref=e1066]: 远
                      - generic [ref=e1068] [cursor=pointer]:
                        - generic [ref=e1069]:
                          - checkbox "驰" [checked]
                        - generic [ref=e1071]: 驰
                      - generic [ref=e1072]:
                        - generic [ref=e1073] [cursor=pointer]:
                          - checkbox "非标配" [disabled]
                        - generic [ref=e1075]: 非标配
                    - generic [ref=e1076]:
                      - generic [ref=e1077] [cursor=pointer]: 提交
                      - generic [ref=e1079] [cursor=pointer]: 预览
                      - generic [ref=e1080] [cursor=pointer]: 编辑
                      - generic [ref=e1081] [cursor=pointer]: 添加
                      - generic [ref=e1082] [cursor=pointer]: 删除
                  - generic [ref=e1084]:
                    - generic [ref=e1086]:
                      - generic [ref=e1087]: ⠿
                      - generic [ref=e1089] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e1092]:
                      - generic [ref=e1094]: 产品
                      - generic [ref=e1095]: E2E测试-待删
                    - generic [ref=e1100]:
                      - generic [ref=e1102] [cursor=pointer]:
                        - generic [ref=e1103]:
                          - checkbox "曜"
                        - generic [ref=e1105]: 曜
                      - generic [ref=e1107] [cursor=pointer]:
                        - generic [ref=e1108]:
                          - checkbox "远"
                        - generic [ref=e1110]: 远
                      - generic [ref=e1112] [cursor=pointer]:
                        - generic [ref=e1113]:
                          - checkbox "驰"
                        - generic [ref=e1115]: 驰
                      - generic [ref=e1116] [cursor=pointer]:
                        - generic [ref=e1117]:
                          - checkbox "非标配"
                        - generic [ref=e1119]: 非标配
                    - generic [ref=e1120]:
                      - generic [ref=e1122] [cursor=pointer]: 预览
                      - generic [ref=e1123] [cursor=pointer]: 编辑
                      - generic [ref=e1124] [cursor=pointer]: 添加
                      - generic [ref=e1125] [cursor=pointer]: 删除
                  - generic [ref=e1127]:
                    - generic [ref=e1129]:
                      - generic [ref=e1130]: ⠿
                      - generic [ref=e1132] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e1135]:
                      - generic [ref=e1137]: 产品
                      - generic [ref=e1138]: E2E测试-新建
                    - generic [ref=e1143]:
                      - generic [ref=e1145] [cursor=pointer]:
                        - generic [ref=e1146]:
                          - checkbox "曜"
                        - generic [ref=e1148]: 曜
                      - generic [ref=e1150] [cursor=pointer]:
                        - generic [ref=e1151]:
                          - checkbox "远"
                        - generic [ref=e1153]: 远
                      - generic [ref=e1155] [cursor=pointer]:
                        - generic [ref=e1156]:
                          - checkbox "驰"
                        - generic [ref=e1158]: 驰
                      - generic [ref=e1159] [cursor=pointer]:
                        - generic [ref=e1160]:
                          - checkbox "非标配"
                        - generic [ref=e1162]: 非标配
                    - generic [ref=e1163]:
                      - generic [ref=e1165] [cursor=pointer]: 预览
                      - generic [ref=e1166] [cursor=pointer]: 编辑
                      - generic [ref=e1167] [cursor=pointer]: 添加
                      - generic [ref=e1168] [cursor=pointer]: 删除
                  - generic [ref=e1170]:
                    - generic [ref=e1172]:
                      - generic [ref=e1173]: ⠿
                      - generic [ref=e1175] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e1178]:
                      - generic [ref=e1180]: 产品
                      - generic [ref=e1181]: E2E测试-新建
                    - generic [ref=e1186]:
                      - generic [ref=e1188] [cursor=pointer]:
                        - generic [ref=e1189]:
                          - checkbox "曜"
                        - generic [ref=e1191]: 曜
                      - generic [ref=e1193] [cursor=pointer]:
                        - generic [ref=e1194]:
                          - checkbox "远"
                        - generic [ref=e1196]: 远
                      - generic [ref=e1198] [cursor=pointer]:
                        - generic [ref=e1199]:
                          - checkbox "驰"
                        - generic [ref=e1201]: 驰
                      - generic [ref=e1202] [cursor=pointer]:
                        - generic [ref=e1203]:
                          - checkbox "非标配"
                        - generic [ref=e1205]: 非标配
                    - generic [ref=e1206]:
                      - generic [ref=e1208] [cursor=pointer]: 预览
                      - generic [ref=e1209] [cursor=pointer]: 编辑
                      - generic [ref=e1210] [cursor=pointer]: 添加
                      - generic [ref=e1211] [cursor=pointer]: 删除
                  - generic [ref=e1213]:
                    - generic [ref=e1215]:
                      - generic [ref=e1216]: ⠿
                      - generic [ref=e1218] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e1221]:
                      - generic [ref=e1223]: 产品
                      - generic [ref=e1224]: E2E测试-待删
                    - generic [ref=e1229]:
                      - generic [ref=e1231] [cursor=pointer]:
                        - generic [ref=e1232]:
                          - checkbox "曜"
                        - generic [ref=e1234]: 曜
                      - generic [ref=e1236] [cursor=pointer]:
                        - generic [ref=e1237]:
                          - checkbox "远"
                        - generic [ref=e1239]: 远
                      - generic [ref=e1241] [cursor=pointer]:
                        - generic [ref=e1242]:
                          - checkbox "驰"
                        - generic [ref=e1244]: 驰
                      - generic [ref=e1245] [cursor=pointer]:
                        - generic [ref=e1246]:
                          - checkbox "非标配"
                        - generic [ref=e1248]: 非标配
                    - generic [ref=e1249]:
                      - generic [ref=e1251] [cursor=pointer]: 预览
                      - generic [ref=e1252] [cursor=pointer]: 编辑
                      - generic [ref=e1253] [cursor=pointer]: 添加
                      - generic [ref=e1254] [cursor=pointer]: 删除
                  - generic [ref=e1256]:
                    - generic [ref=e1258]:
                      - generic [ref=e1259]: ⠿
                      - generic [ref=e1261] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e1264]:
                      - generic [ref=e1266]: 产品
                      - generic [ref=e1267]: E2E测试-待删
                    - generic [ref=e1272]:
                      - generic [ref=e1274] [cursor=pointer]:
                        - generic [ref=e1275]:
                          - checkbox "曜"
                        - generic [ref=e1277]: 曜
                      - generic [ref=e1279] [cursor=pointer]:
                        - generic [ref=e1280]:
                          - checkbox "远"
                        - generic [ref=e1282]: 远
                      - generic [ref=e1284] [cursor=pointer]:
                        - generic [ref=e1285]:
                          - checkbox "驰"
                        - generic [ref=e1287]: 驰
                      - generic [ref=e1288] [cursor=pointer]:
                        - generic [ref=e1289]:
                          - checkbox "非标配"
                        - generic [ref=e1291]: 非标配
                    - generic [ref=e1292]:
                      - generic [ref=e1294] [cursor=pointer]: 预览
                      - generic [ref=e1295] [cursor=pointer]: 编辑
                      - generic [ref=e1296] [cursor=pointer]: 添加
                      - generic [ref=e1297] [cursor=pointer]: 删除
                  - generic [ref=e1299]:
                    - generic [ref=e1301]:
                      - generic [ref=e1302]: ⠿
                      - generic [ref=e1304] [cursor=pointer]:
                        - checkbox
                    - generic [ref=e1307]:
                      - generic [ref=e1309]: 产品
                      - generic [ref=e1310]: E2E测试-新建
                    - generic [ref=e1315]:
                      - generic [ref=e1317] [cursor=pointer]:
                        - generic [ref=e1318]:
                          - checkbox "曜"
                        - generic [ref=e1320]: 曜
                      - generic [ref=e1322] [cursor=pointer]:
                        - generic [ref=e1323]:
                          - checkbox "远"
                        - generic [ref=e1325]: 远
                      - generic [ref=e1327] [cursor=pointer]:
                        - generic [ref=e1328]:
                          - checkbox "驰"
                        - generic [ref=e1330]: 驰
                      - generic [ref=e1331] [cursor=pointer]:
                        - generic [ref=e1332]:
                          - checkbox "非标配"
                        - generic [ref=e1334]: 非标配
                    - generic [ref=e1335]:
                      - generic [ref=e1337] [cursor=pointer]: 预览
                      - generic [ref=e1338] [cursor=pointer]: 编辑
                      - generic [ref=e1339] [cursor=pointer]: 添加
                      - generic [ref=e1340] [cursor=pointer]: 删除
```

# Test source

```ts
  1   | import { test, expect } from '@playwright/test';
  2   | import { DataListFunctionalPage } from '../../pages/data-list-functional.page';
  3   | import { DataListPage } from '../../pages/data-list.page';
  4   | 
  5   | const TEST_NAME_PREFIX = 'E2E测试';
  6   | 
  7   | test.describe('产品清单-CRUD闭环', () => {
  8   |   let funcPage: DataListFunctionalPage;
  9   |   let dataListPage: DataListPage;
  10  | 
  11  |   test.beforeEach(async ({ page }) => {
  12  |     funcPage = new DataListFunctionalPage(page);
  13  |     dataListPage = new DataListPage(page);
  14  |     await funcPage.gotoDataList();
  15  |   });
  16  | 
  17  |   test('新建条目-填写名称保存成功', async ({ page }) => {
  18  |     // 需要在L2树节点下才能新建，先展开侧边栏树并选中L2节点
  19  |     // 新建按钮条件: props.isEditing && props.selectedNode?.level === 2
  20  |     const treePanel = page.locator('.tree-panel');
  21  |     // 1. 先展开根节点（增加等待时间确保子节点渲染完成）
  22  |     const rootNodes = treePanel.locator('.el-tree-node');
  23  |     const rootCount = await rootNodes.count();
  24  |     for (let i = 0; i < Math.min(rootCount, 3); i++) {
  25  |       const node = rootNodes.nth(i);
  26  |       const expandIcon = node.locator('.el-tree-node__expand-icon');
  27  |       if (await expandIcon.isVisible().catch(() => false)) {
  28  |         await expandIcon.click();
  29  |         await page.waitForTimeout(1000);
  30  |       }
  31  |     }
  32  |     // 2. 找到并点击一个L2节点（展开后出现的子节点）
  33  |     const childNodes = treePanel.locator('.el-tree-node .el-tree-node');
  34  |     const childCount = await childNodes.count();
  35  |     for (let i = 0; i < Math.min(childCount, 5); i++) {
  36  |       const node = childNodes.nth(i);
  37  |       if (await node.isVisible().catch(() => false)) {
  38  |         await node.locator('.el-tree-node__content').click();
  39  |         await page.waitForTimeout(1000);
  40  |         // 检查新建按钮是否可见
  41  |         if (await funcPage.newButton.isVisible({ timeout: 2000 }).catch(() => false)) {
  42  |           break;
  43  |         }
  44  |       }
  45  |     }
  46  |     // 点击新建按钮
> 47  |     await funcPage.newButton.click({ timeout: 5000 });
      |                              ^ TimeoutError: locator.click: Timeout 5000ms exceeded.
  48  |     // 验证编辑对话框出现
  49  |     await expect(page.locator('.el-dialog').filter({ has: page.locator('.edit-form-compact') })).toBeVisible({ timeout: 5000 });
  50  |     // 填写产品名称
  51  |     await funcPage.fillProductName(TEST_NAME_PREFIX + '-新建');
  52  |     // 保存
  53  |     await funcPage.saveEditForm();
  54  |     // 验证成功消息
  55  |     const msg = await funcPage.getSuccessMessage();
  56  |     expect(msg).toBeTruthy();
  57  |     // 清理：找到新创建的行并删除
  58  |     const newRow = page.locator('.vrow').filter({ hasText: TEST_NAME_PREFIX + '-新建' }).first();
  59  |     if (await newRow.isVisible({ timeout: 3000 }).catch(() => false)) {
  60  |       await newRow.locator('.op-btn.op-del').filter({ hasText: '删除' }).click();
  61  |       await funcPage.confirmMessageBox();
  62  |     }
  63  |   });
  64  | 
  65  |   test('编辑条目-打开编辑对话框并修改名称', async ({ page }) => {
  66  |     // 打开第一行的编辑对话框
  67  |     await funcPage.openEditForFirstRow();
  68  |     // 验证编辑对话框可见
  69  |     await expect(page.locator('.el-dialog').filter({ has: page.locator('.edit-form-compact') })).toBeVisible({ timeout: 5000 });
  70  |     // 修改产品名称
  71  |     const nameInput = page.locator('.edit-form-compact input').first();
  72  |     const currentName = await nameInput.inputValue();
  73  |     await nameInput.clear();
  74  |     await nameInput.fill(currentName + '-编辑');
  75  |     // 保存
  76  |     await funcPage.saveEditForm();
  77  |     // 验证成功消息
  78  |     const msg = await funcPage.getSuccessMessage();
  79  |     expect(msg).toBeTruthy();
  80  |   });
  81  | 
  82  |   test('添加子条目-在L3产品下点击添加按钮', async ({ page }) => {
  83  |     // 找到一条有"添加"按钮的行（L3级别）
  84  |     const addRow = page.locator('.vrow:not(.sep-row)').filter({ has: page.locator('.op-btn.op-add').filter({ hasText: '添加' }) }).first();
  85  |     if (await addRow.isVisible({ timeout: 3000 }).catch(() => false)) {
  86  |       await addRow.locator('.op-btn.op-add').filter({ hasText: '添加' }).click();
  87  |       await page.waitForTimeout(1000);
  88  |       // 验证对话框出现
  89  |       await expect(page.locator('.el-dialog').filter({ has: page.locator('.edit-form-compact') })).toBeVisible({ timeout: 5000 });
  90  |       // 填写子条目名称
  91  |       await funcPage.fillProductName(TEST_NAME_PREFIX + '-子条目');
  92  |       // 保存
  93  |       await funcPage.saveEditForm();
  94  |       const msg = await funcPage.getSuccessMessage();
  95  |       expect(msg).toBeTruthy();
  96  |       // 清理：删除子条目
  97  |       const childRow = page.locator('.vrow').filter({ hasText: TEST_NAME_PREFIX + '-子条目' }).first();
  98  |       if (await childRow.isVisible({ timeout: 3000 }).catch(() => false)) {
  99  |         await childRow.locator('.op-btn.op-del').filter({ hasText: '删除' }).click();
  100 |         await funcPage.confirmMessageBox();
  101 |       }
  102 |     }
  103 |   });
  104 | 
  105 |   test('删除条目-确认删除后条目消失', async ({ page }) => {
  106 |     // 先展开树并找到L2节点以显示新建按钮
  107 |     const treePanel = page.locator('.tree-panel');
  108 |     const rootNodes = treePanel.locator('.el-tree-node');
  109 |     const rootCount = await rootNodes.count();
  110 |     for (let i = 0; i < Math.min(rootCount, 3); i++) {
  111 |       const node = rootNodes.nth(i);
  112 |       const expandIcon = node.locator('.el-tree-node__expand-icon');
  113 |       if (await expandIcon.isVisible().catch(() => false)) {
  114 |         await expandIcon.click();
  115 |         await page.waitForTimeout(500);
  116 |       }
  117 |     }
  118 |     // 找到并点击L2子节点使新建按钮出现
  119 |     const childNodes = treePanel.locator('.el-tree-node .el-tree-node');
  120 |     const childCount = await childNodes.count();
  121 |     for (let i = 0; i < Math.min(childCount, 5); i++) {
  122 |       const node = childNodes.nth(i);
  123 |       if (await node.isVisible().catch(() => false)) {
  124 |         await node.locator('.el-tree-node__content').click();
  125 |         await page.waitForTimeout(1000);
  126 |         if (await funcPage.newButton.isVisible({ timeout: 2000 }).catch(() => false)) {
  127 |           break;
  128 |         }
  129 |       }
  130 |     }
  131 |     const newBtn = funcPage.newButton;
  132 |     if (await newBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
  133 |       await newBtn.click();
  134 |       await page.waitForTimeout(1000);
  135 |       await funcPage.fillProductName(TEST_NAME_PREFIX + '-待删');
  136 |       await funcPage.saveEditForm();
  137 |       await page.waitForTimeout(500);
  138 |       // 找到新建的行并删除
  139 |       const newRow = page.locator('.vrow').filter({ hasText: TEST_NAME_PREFIX + '-待删' }).first();
  140 |       if (await newRow.isVisible({ timeout: 3000 }).catch(() => false)) {
  141 |         await newRow.locator('.op-btn.op-del').filter({ hasText: '删除' }).click();
  142 |         await funcPage.confirmMessageBox();
  143 |         const msg = await funcPage.getSuccessMessage();
  144 |         expect(msg).toBeTruthy();
  145 |       }
  146 |     }
  147 |   });
```