import axios from 'axios';
import { API } from '../config/api';

const getAuthHeaders = () => {
  const token = localStorage.getItem('token');
  return token ? { 'Authorization': `Bearer ${token}` } : {};
};

const bookService = {
  getBooks: async () => {
    const response = await axios.get(`${API.CATALOG}/books`, {
      headers: getAuthHeaders()
    });
    return response.data;
  },

  getBook: async (id) => {
    const response = await axios.get(`${API.CATALOG}/books/${id}`, {
      headers: getAuthHeaders()
    });
    return response.data;
  },

  createBook: async (book) => {
    const response = await axios.post(`${API.CATALOG}/books`, book, {
      headers: {
        ...getAuthHeaders(),
        'Content-Type': 'application/json'
      }
    });
    return response.data;
  },

  updateBook: async (id, book) => {
    const response = await axios.put(`${API.CATALOG}/books/${id}`, book, {
      headers: {
        ...getAuthHeaders(),
        'Content-Type': 'application/json'
      }
    });
    return response.data;
  },

  deleteBook: async (id) => {
    await axios.delete(`${API.CATALOG}/books/${id}`, {
      headers: getAuthHeaders()
    });
  }
};

export default bookService;

