import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { loginRequest } from '../services/authService'
import useAuthStore from '../store/authStore'

export default function Login() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(null)
    const navigate = useNavigate()
    const location = useLocation()
    const from = location.state?.from?.pathname || '/'

    useEffect(() => {
        useAuthStore.getState().logout()  // evita token sporchi
        localStorage.clear()              // ulteriore sicurezza
    }, [])

    const onSubmit = async (e) => {
        e.preventDefault()
        setLoading(true); setError(null)
        try {
            const { jwt } = await loginRequest(email, password)
            // console.debug('JWT ricevuto:', jwt)   // <-- utile se vuoi vedere in console
            useAuthStore.getState().login(jwt)
            navigate(from, { replace: true })
        } catch (err) {
            setError('Credenziali non valide')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
            <form onSubmit={onSubmit} className="bg-white p-6 rounded-2xl shadow w-[360px] border">
                <h1 className="text-xl font-semibold mb-4">Accedi</h1>

                <label className="block text-sm mb-1">Email</label>
                <input className="w-full border rounded-xl px-3 py-2 mb-3"
                    value={email} onChange={e => setEmail(e.target.value)} />

                <label className="block text-sm mb-1">Password</label>
                <input type="password" className="w-full border rounded-xl px-3 py-2 mb-4"
                    value={password} onChange={e => setPassword(e.target.value)} />

                {error && <div className="text-red-600 text-sm mb-3">{error}</div>}

                <button type="submit" disabled={loading} className="w-full border rounded-xl py-2">
                    {loading ? '...' : 'Entra'}
                </button>
            </form>
        </div>
    )
}
