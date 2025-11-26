import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import http from '../services/http'
import { API } from '../config/api'

export default function Search() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)

  const search = async (p = page, s = size) => {
    if (!query.trim()) { setData(null); return }
    setLoading(true)
    try { const res = await http.get(`${API.SEARCH}/books/paged`, { params: { query, page: p, size: s } }); setData(res.data) } finally { setLoading(false) }
  }

  useEffect(() => { setPage(0); setData(null) }, [query])

  const next = () => { const p = page + 1; setPage(p); search(p, size) }
  const prev = () => { const p = Math.max(0, page - 1); setPage(p); search(p, size) }

  return (
    <section className="max-w-6xl mx-auto px-4 py-10">
      <h2 className="text-2xl font-semibold">Tìm kiếm sách</h2>
      <div className="mt-4 flex gap-3">
        <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Từ khóa" className="flex-1 px-3 py-2 rounded-md border border-gray-300" />
        <button onClick={() => search()} className="px-4 py-2 rounded-md bg-primary-600 text-white hover:bg-primary-700">Tìm</button>
      </div>
      {loading && <div className="mt-4">...</div>}
      {data && (
        <>
          <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
            {data.content.map(b => (
              <div key={b.id} className="group rounded-xl border border-gray-200 bg-white p-4 shadow-sm hover:shadow-md transition">
                <h4 className="text-lg font-medium text-gray-900 group-hover:text-primary-600">{b.title}</h4>
                <div className="text-sm text-gray-600">{b.author}</div>
                <Link to={`/books/${b.id}`} className="mt-4 inline-block px-3 py-1.5 rounded-md bg-primary-600 text-white text-sm hover:bg-primary-700">Chi tiết</Link>
              </div>
            ))}
          </div>
          <div className="mt-4 flex items-center gap-3">
            <button onClick={prev} className="px-3 py-1.5 rounded-md border">Prev</button>
            <button onClick={next} className="px-3 py-1.5 rounded-md border">Next</button>
            <span className="text-sm text-gray-600">Page {page+1}</span>
          </div>
        </>
      )}
    </section>
  )
}
