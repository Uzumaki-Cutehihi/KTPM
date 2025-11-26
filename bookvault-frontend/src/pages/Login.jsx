import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const onSubmit = async e => {
    e.preventDefault()
    setLoading(true)
    try {
      await login(username, password)
      navigate('/books')
    } catch (err) {
      setError(err?.response?.data?.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="max-w-md mx-auto px-4 py-10">
      <h2 className="text-2xl font-semibold">Đăng nhập</h2>
      {error && <div className="mt-2 text-sm text-red-600">{error}</div>}
      <form onSubmit={onSubmit} className="mt-6 space-y-4">
        <input className="w-full px-3 py-2 rounded-md border border-gray-300" value={username} onChange={e => setUsername(e.target.value)} placeholder="Username" />
        <input className="w-full px-3 py-2 rounded-md border border-gray-300" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Password" />
        <button type="submit" disabled={loading} className="w-full px-4 py-2 rounded-md bg-primary-600 text-white hover:bg-primary-700">{loading ? '...' : 'Login'}</button>
      </form>
    </section>
  )
}
