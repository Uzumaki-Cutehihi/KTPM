import { useAuth } from '../context/AuthContext'
import { jwtDecode } from 'jwt-decode'
import mediaService from '../services/mediaService'
import iamService from '../services/iamService'

export default function Profile() {
  const { isAuthenticated, roles, logout } = useAuth()
  const token = localStorage.getItem('token')
  const payload = token ? (() => { try { return jwtDecode(token) } catch { return null } })() : null
  const username = payload?.sub || payload?.username
  const [me, setMe] = useState(null)
  useEffect(() => { (async () => { if (username) { try { const data = await iamService.me(username); setMe(data) } catch {} } })() }, [username])
  if (!isAuthenticated) return null
  const [preview, setPreview] = useState(null)
  const upload = async e => {
    const file = e.target.files?.[0]
    if (!file) return
    setPreview(URL.createObjectURL(file))
    try { await mediaService.upload(file) } catch {}
  }
  return (
    <div style={{ padding: 20 }}>
      <h2>Hồ sơ</h2>
      <div>Tên người dùng: {username || 'N/A'}</div>
      <div>Roles: {roles.join(', ')}</div>
      {me && <div>Email: {me.email}</div>}
      <div style={{ marginTop: 12 }}>
        <input type="file" onChange={upload} />
        {preview && <div style={{ marginTop: 8 }}><img src={preview} alt="preview" style={{ width: 120, height: 120, objectFit: 'cover', borderRadius: 8 }} /></div>}
      </div>
      <button onClick={logout} style={{ marginTop: 12 }}>Logout</button>
    </div>
  )
}
