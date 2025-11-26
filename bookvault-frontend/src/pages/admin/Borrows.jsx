import { useEffect, useState } from 'react'
import borrowingAdminService from '../../services/borrowingAdminService'

export default function AdminBorrows() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = async (p = page, s = size) => { setLoading(true); try { const res = await borrowingAdminService.listLoans({ page: p, size: s }); setData(res) } finally { setLoading(false) } }
  useEffect(() => { load() }, [])
  const next = () => { const p = page + 1; setPage(p); load(p, size) }
  const prev = () => { const p = Math.max(0, page - 1); setPage(p); load(p, size) }

  return (
    <section className="max-w-6xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-semibold">Quản lý mượn/trả</h2>
        <button onClick={() => load()} className="px-3 py-2 rounded-md border border-gray-300 hover:bg-gray-100">{loading?'...':'Refresh'}</button>
      </div>
      <div className="mt-6 overflow-x-auto rounded-xl border border-gray-200">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-2 text-left">ID</th>
              <th className="px-4 py-2 text-left">User</th>
              <th className="px-4 py-2 text-left">Book</th>
              <th className="px-4 py-2 text-left">Qty</th>
              <th className="px-4 py-2 text-left">Status</th>
              <th className="px-4 py-2 text-left">Due</th>
            </tr>
          </thead>
          <tbody>
            {data?.content?.map(l => (
              <tr key={l.id} className="border-t">
                <td className="px-4 py-2">{l.id}</td>
                <td className="px-4 py-2">{l.userId}</td>
                <td className="px-4 py-2">{l.bookId}</td>
                <td className="px-4 py-2">{l.quantity}</td>
                <td className="px-4 py-2">{l.status}</td>
                <td className="px-4 py-2">{l.dueAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-4 flex items-center gap-3">
        <button onClick={prev} className="px-3 py-1.5 rounded-md border">Prev</button>
        <button onClick={next} className="px-3 py-1.5 rounded-md border">Next</button>
        <span className="text-sm text-gray-600">Page {page+1}</span>
      </div>
    </section>
  )
}
