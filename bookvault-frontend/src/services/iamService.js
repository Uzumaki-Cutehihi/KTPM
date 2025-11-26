import http from './http'
import { API } from '../config/api'

const iamService = {
  me: async (username) => {
    const res = await http.get(`${API.IAM}/users/me`, { params: { username } })
    return res.data
  },
  getMe: async () => {
    const res = await http.get(`${API.IAM}/users/me`)
    return res.data
  },
  updateMe: async payload => {
    const res = await http.put(`${API.IAM}/users/me`, payload)
    return res.data
  }
}

export default iamService
