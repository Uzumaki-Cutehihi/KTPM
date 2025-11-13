import { useState, useEffect } from 'react';
import './App.css';
import authService from './services/authService';
import bookService from './services/bookService';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    loginUsername: '',
    loginPassword: ''
  });
  const [bookForm, setBookForm] = useState({
    title: '',
    author: '',
    isbn: '',
    quantity: 0
  });

  useEffect(() => {
    setIsAuthenticated(authService.isAuthenticated());
    if (authService.isAuthenticated()) {
      loadBooks();
    }
  }, []);

  const loadBooks = async () => {
    try {
      setLoading(true);
      const data = await bookService.getBooks();
      setBooks(data);
      setError('');
    } catch (err) {
      setError('Failed to load books: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      await authService.register(formData.username, formData.email, formData.password);
      setIsAuthenticated(true);
      setError('');
      await loadBooks();
    } catch (err) {
      setError('Registration failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      await authService.login(formData.loginUsername, formData.loginPassword);
      setIsAuthenticated(true);
      setError('');
      await loadBooks();
    } catch (err) {
      setError('Login failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    authService.logout();
    setIsAuthenticated(false);
    setBooks([]);
  };

  const handleCreateBook = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      await bookService.createBook(bookForm);
      setBookForm({ title: '', author: '', isbn: '', quantity: 0 });
      setError('');
      await loadBooks();
    } catch (err) {
      setError('Failed to create book: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="App" style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
      <header style={{ marginBottom: '30px', borderBottom: '2px solid #ccc', paddingBottom: '20px' }}>
        <h1>📚 BookVault - Library Management System</h1>
        {isAuthenticated && (
          <button onClick={handleLogout} style={{ padding: '10px 20px', marginTop: '10px' }}>
            Logout
          </button>
        )}
      </header>

      {error && (
        <div style={{ padding: '10px', background: '#ffebee', color: '#c62828', marginBottom: '20px', borderRadius: '4px' }}>
          {error}
        </div>
      )}

      {!isAuthenticated ? (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          <div style={{ border: '1px solid #ccc', padding: '20px', borderRadius: '8px' }}>
            <h2>Register</h2>
            <form onSubmit={handleRegister}>
              <input
                type="text"
                placeholder="Username"
                value={formData.username}
                onChange={(e) => setFormData({...formData, username: e.target.value})}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
              />
              <input
                type="email"
                placeholder="Email"
                value={formData.email}
                onChange={(e) => setFormData({...formData, email: e.target.value})}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
              />
              <input
                type="password"
                placeholder="Password"
                value={formData.password}
                onChange={(e) => setFormData({...formData, password: e.target.value})}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
              />
              <button type="submit" disabled={loading} style={{ width: '100%', padding: '10px' }}>
                {loading ? 'Registering...' : 'Register'}
              </button>
            </form>
          </div>

          <div style={{ border: '1px solid #ccc', padding: '20px', borderRadius: '8px' }}>
            <h2>Login</h2>
            <form onSubmit={handleLogin}>
              <input
                type="text"
                placeholder="Username"
                value={formData.loginUsername}
                onChange={(e) => setFormData({...formData, loginUsername: e.target.value})}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
              />
              <input
                type="password"
                placeholder="Password"
                value={formData.loginPassword}
                onChange={(e) => setFormData({...formData, loginPassword: e.target.value})}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
              />
              <button type="submit" disabled={loading} style={{ width: '100%', padding: '10px' }}>
                {loading ? 'Logging in...' : 'Login'}
              </button>
            </form>
          </div>
        </div>
      ) : (
        <div>
          <div style={{ border: '1px solid #ccc', padding: '20px', borderRadius: '8px', marginBottom: '20px' }}>
            <h2>Create New Book</h2>
            <form onSubmit={handleCreateBook}>
              <input
                type="text"
                placeholder="Title"
                value={bookForm.title}
                onChange={(e) => setBookForm({...bookForm, title: e.target.value})}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
              />
              <input
                type="text"
                placeholder="Author"
                value={bookForm.author}
                onChange={(e) => setBookForm({...bookForm, author: e.target.value})}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
              />
              <input
                type="text"
                placeholder="ISBN"
                value={bookForm.isbn}
                onChange={(e) => setBookForm({...bookForm, isbn: e.target.value})}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
              />
              <input
                type="number"
                placeholder="Quantity"
                value={bookForm.quantity}
                onChange={(e) => setBookForm({...bookForm, quantity: parseInt(e.target.value)})}
                required
                min="0"
                style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
              />
              <button type="submit" disabled={loading} style={{ width: '100%', padding: '10px' }}>
                {loading ? 'Creating...' : 'Create Book'}
              </button>
            </form>
          </div>

          <div>
            <h2>Books ({books.length})</h2>
            <button onClick={loadBooks} disabled={loading} style={{ padding: '10px 20px', marginBottom: '20px' }}>
              {loading ? 'Loading...' : 'Refresh'}
            </button>
            {books.length === 0 ? (
              <p>No books found. Create one above!</p>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '20px' }}>
                {books.map(book => (
                  <div key={book.id} style={{ border: '1px solid #ccc', padding: '15px', borderRadius: '8px' }}>
                    <h3>{book.title}</h3>
                    <p><strong>Author:</strong> {book.author}</p>
                    <p><strong>ISBN:</strong> {book.isbn}</p>
                    <p><strong>Quantity:</strong> {book.quantity}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
