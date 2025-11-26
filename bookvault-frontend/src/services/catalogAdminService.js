import http from './http'
import { API } from '../config/api'

const catalogAdminService = {
  listBooksPaged: async ({ page = 0, size = 10 }) => {
    const res = await http.get(`${API.CATALOG}/books/paged`, { params: { page, size } })
    return res.data
  }
}

export default catalogAdminService

