import { useEffect, useState } from 'react'
import http from '../../services/http'
import { API } from '../../config/api'

export default function AdminNotifications() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [payload, setPayload] = useState({ to: '', subject: '', body: '' })

  const load = async () => { setLoading(true); try { const res = await http.get(`${API.NOTIFICATION}/notifications`); setItems(res.data||[]) } finally { setLoading(false) } }
  useEffect(() => { load() }, [])

  const send = async () => { setLoading(true); try { await http.post(`${API.NOTIFICATION}/email`, payload); await http.post(`${API.NOTIFICATION}/notifications`, { type: 'EMAIL', ...payload }); setPayload({ to:'', subject:'', body:'' }); await load() } finally { setLoading(false) } }
  const remove = async id => { await http.delete(`${API.NOTIFICATION}/notifications/${id}`); await load() }

  return (
    <section className="max-w-6xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-semibold">Thông báo</h2>
        <button onClick={load} className="px-3 py-2 rounded-md border border-gray-300 hover:bg-gray-100">{loading?'...':'Refresh'}</button>
      </div>
      <div className="mt-6 grid md:grid-cols-2 gap-6">
        <div className="rounded-xl border border-gray-200 p-4">
          <h3 className="font-medium">Gửi Email</h3>
          <div className="mt-3 space-y-3">
            <input className="w-full px-3 py-2 rounded-md border" placeholder="To" value={payload.to} onChange={e=>setPayload({...payload,to:e.target.value})} />
            <input className="w-full px-3 py-2 rounded-md border" placeholder="Subject" value={payload.subject} onChange={e=>setPayload({...payload,subject:e.target.value})} />
            <textarea className="w-full px-3 py-2 rounded-md border" placeholder="Body" value={payload.body} onChange={e=>setPayload({...payload,body:e.target.value})} />
            <button onClick={send} className="px-4 py-2 rounded-md bg-primary-600 text-white hover:bg-primary-700">Gửi</button>
          </div>
        </div>
        <div className="rounded-xl border border-gray-200 p-4">
          <h3 className="font-medium">Danh sách thông báo</h3>
          <ul className="mt-3 space-y-2">
            {items.map(it => (
              <li key={it._id} className="flex items-center justify-between rounded-md border p-2">
                <div className="text-sm text-gray-700">{it.subject || it.type}</div>
                <button onClick={()=>remove(it._id)} className="px-3 py-1.5 rounded-md border hover:bg-gray-100">Xóa</button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  )
}
