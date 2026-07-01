export const PLATFORM_MODULES = [
  { value: '产品清单', label: '产品清单', children: undefined },
  {
    value: '需求管理', label: '需求管理',
    children: [
      { value: '需求清单', label: '需求清单' },
      { value: '需求图片', label: '需求图片' }
    ]
  },
  { value: '版本管理', label: '版本管理', children: undefined },
  {
    value: '系统管理', label: '系统管理',
    children: [
      { value: '用户管理', label: '用户管理' },
      { value: '权限套餐管理', label: '权限套餐管理' },
      {
        value: '基础数据维护', label: '基础数据维护',
        children: [
          { value: '业务分类维护', label: '业务分类维护' },
          { value: '解决方案维护', label: '解决方案维护' },
          { value: '应用角色维护', label: '应用角色维护' },
          { value: '功能状态维护', label: '功能状态维护' }
        ]
      }
    ]
  },
  { value: '图床管理', label: '图床管理', children: undefined }
]
