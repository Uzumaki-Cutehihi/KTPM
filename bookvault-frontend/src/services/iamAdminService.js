import http from './http'
import { API } from '../config/api'

const iamAdminService = {
  listUsers: async ({ page = 0, size = 10 }) => {
    const res = await http.get(`${API.IAM}/users`, { params: { page, size } })
    return res.data
  }
}

export default iamAdminService

