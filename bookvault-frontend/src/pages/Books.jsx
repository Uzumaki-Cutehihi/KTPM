import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import bookService from '../services/bookService'

export default function Books() {
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = async () => {
    setLoading(true)
    try {
      const data = await bookService.getBooks()
      setBooks(data || [])
      setError('')
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to load books')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  return (
    <section className="max-w-6xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-semibold">Danh sách sách</h2>
        <button onClick={load} disabled={loading} className="px-3 py-2 rounded-md border border-gray-300 hover:bg-gray-100">
          {loading ? '...' : 'Refresh'}
        </button>
      </div>
      {error && <div className="mt-3 text-sm text-red-600">{error}</div>}
      {books.length === 0 ? (
        <p className="mt-6 text-gray-600">Không có sách</p>
      ) : (
        <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
          {books.map(b => (
            <div key={b.id} className="group rounded-xl border border-gray-200 bg-white p-4 shadow-sm hover:shadow-md transition">
              <h4 className="text-lg font-medium text-gray-900 group-hover:text-primary-600">{b.title}</h4>
              <div className="text-sm text-gray-600">{b.author}</div>
              <div className="mt-2 text-xs text-gray-500">ISBN: {b.isbn}</div>
              <Link to={`/books/${b.id}`} className="mt-4 inline-block px-3 py-1.5 rounded-md bg-primary-600 text-white text-sm hover:bg-primary-700">Chi tiết</Link>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
