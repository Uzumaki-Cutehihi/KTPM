import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { isAuthenticated, roles, logout } = useAuth()
  const isAdmin = roles?.includes('ROLE_ADMIN')
  return (
    <header className="sticky top-0 z-50 bg-white/80 backdrop-blur border-b border-gray-200">
      <nav className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <Link to="/" className="text-xl font-bold text-primary-600">BookVault</Link>
          <div className="hidden md:flex items-center gap-4 text-sm">
            <NavLink to="/books" className={({isActive}) => `hover:text-primary-600 ${isActive?'text-primary-600':'text-gray-600'}`}>Books</NavLink>
            <NavLink to="/search" className={({isActive}) => `hover:text-primary-600 ${isActive?'text-primary-600':'text-gray-600'}`}>Search</NavLink>
            {isAuthenticated && <NavLink to="/profile" className={({isActive}) => `hover:text-primary-600 ${isActive?'text-primary-600':'text-gray-600'}`}>Profile</NavLink>}
            {isAuthenticated && <NavLink to="/favourites" className={({isActive}) => `hover:text-primary-600 ${isActive?'text-primary-600':'text-gray-600'}`}>Favourites</NavLink>}
            {isAuthenticated && <NavLink to="/borrowed" className={({isActive}) => `hover:text-primary-600 ${isActive?'text-primary-600':'text-gray-600'}`}>Borrowed</NavLink>}
            {isAuthenticated && <NavLink to="/history" className={({isActive}) => `hover:text-primary-600 ${isActive?'text-primary-600':'text-gray-600'}`}>History</NavLink>}
            {isAuthenticated && <NavLink to="/chat" className={({isActive}) => `hover:text-primary-600 ${isActive?'text-primary-600':'text-gray-600'}`}>Chat</NavLink>}
            {isAdmin && <NavLink to="/admin" className={({isActive}) => `hover:text-primary-600 ${isActive?'text-primary-600':'text-gray-600'}`}>Admin</NavLink>}
          </div>
        </div>
        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <button onClick={logout} className="px-3 py-1.5 rounded-md bg-primary-600 text-white hover:bg-primary-700">Logout</button>
          ) : (
            <div className="flex items-center gap-3">
              <Link to="/login" className="px-3 py-1.5 rounded-md border border-gray-300 hover:bg-gray-100">Login</Link>
              <Link to="/register" className="px-3 py-1.5 rounded-md bg-primary-600 text-white hover:bg-primary-700">Register</Link>
            </div>
          )}
        </div>
      </nav>
    </header>
  )
}
