import axios from 'axios';
import { API } from '../config/api';

const authService = {
  login: async (username, password) => {
    const response = await axios.post(`${API.IAM}/auth/login`, {
      username,
      password
    });
    if (response.data.accessToken) {
      localStorage.setItem('token', response.data.accessToken);
    }
    return response.data;
  },

  register: async (username, email, password) => {
    const response = await axios.post(`${API.IAM}/auth/register`, {
      username,
      email,
      password
    });
    if (response.data.accessToken) {
      localStorage.setItem('token', response.data.accessToken);
    }
    return response.data;
  },

  logout: () => {
    localStorage.removeItem('token');
  },

  getToken: () => {
    return localStorage.getItem('token');
  },

  isAuthenticated: () => {
    return !!localStorage.getItem('token');
  }
};

export default authService;

