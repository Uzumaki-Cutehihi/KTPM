import http from './http'
import { API } from '../config/api'

const borrowingAdminService = {
  listLoans: async ({ page = 0, size = 10 }) => {
    const res = await http.get(`${API.BORROWING}/loans`, { params: { page, size } })
    return res.data
  }
}

export default borrowingAdminService

