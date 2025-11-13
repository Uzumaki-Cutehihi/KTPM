const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export const API = {
  BASE_URL: API_BASE_URL,
  IAM: `${API_BASE_URL}/api/iam/v1`,
  CATALOG: `${API_BASE_URL}/api/catalog/v1`,
  BORROWING: `${API_BASE_URL}/api/borrowing/v1`,
  SEARCH: `${API_BASE_URL}/api/search/v1`,
  ADMIN: `${API_BASE_URL}/api/admin/v1`,
};

