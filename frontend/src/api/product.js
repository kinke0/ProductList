import request from '../utils/request'

// L1 统计分类
export function getProductL1List(versionId) {
  return request.get('/product/l1/list', { params: { versionId } })
}

export function createProductL1(versionId, name) {
  return request.post('/product/l1', { name }, { params: { versionId } })
}

export function updateProductL1(id, name) {
  return request.put(`/product/l1/${id}`, { name })
}

export function deleteProductL1(id) {
  return request.delete(`/product/l1/${id}`)
}

export function updateProductL1Sort(versionId, sortList) {
  return request.put('/product/l1/sort', sortList, { params: { versionId } })
}

// L2 核心业务方向
export function getProductL2List(versionId, l1Id) {
  return request.get('/product/l2/list', { params: { versionId, l1Id } })
}

export function createProductL2(versionId, l1Id, name) {
  return request.post('/product/l2', { name }, { params: { versionId, l1Id } })
}

export function updateProductL2(id, name) {
  return request.put(`/product/l2/${id}`, { name })
}

export function deleteProductL2(id) {
  return request.delete(`/product/l2/${id}`)
}

export function updateProductL2Sort(versionId, sortList) {
  return request.put('/product/l2/sort', sortList, { params: { versionId } })
}

// L3 核心业务产品
export function getProductL3List(versionId, l2Id) {
  return request.get('/product/l3/list', { params: { versionId, l2Id } })
}

export function createProductL3(versionId, l2Id, name) {
  return request.post('/product/l3', { name }, { params: { versionId, l2Id } })
}

export function updateProductL3(id, name) {
  return request.put(`/product/l3/${id}`, { name })
}

export function deleteProductL3(id) {
  return request.delete(`/product/l3/${id}`)
}

export function updateProductL3Sort(versionId, sortList) {
  return request.put('/product/l3/sort', sortList, { params: { versionId } })
}
