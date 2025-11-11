import { useState } from 'react'
import { changeMyPassword } from '../../services/utenteMeService'
import PageBar from '../../components/PageBar'

export default function Profilo() {
    const [oldPassword, setOldPassword] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [loading, setLoading] = useState(false)
    const [message, setMessage] = useState(null)
    const [error, setError] = useState(null)

    const onSubmit = async (e) => {
        e.preventDefault()
        setError(null)
        setMessage(null)

        if (newPassword !== confirmPassword) {
            setError('Le nuove password non coincidono')
            return
        }

        setLoading(true)
        try {
            await changeMyPassword(oldPassword, newPassword)
            setMessage('Password modificata con successo ✅')
            setOldPassword(''); setNewPassword(''); setConfirmPassword('')
        } catch {
            setError('Errore durante il cambio password')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="p-6">
            {/* Barra con titolo + tasti Indietro/Home */}
            <PageBar title="Profilo" />

            {/* Contenitore centrale del form */}
            <div className="flex justify-center">
                <div className="w-full max-w-md bg-white shadow-lg rounded-2xl p-6 border">
                    <h1 className="text-2xl font-bold mb-4 text-center">Profilo</h1>

                    <form onSubmit={onSubmit}>
                        <label className="block text-sm mb-2">Password attuale</label>
                        <input
                            type="password"
                            value={oldPassword}
                            onChange={(e) => setOldPassword(e.target.value)}
                            className="border rounded w-full px-3 py-2 mb-3"
                        />

                        <label className="block text-sm mb-2">Nuova password</label>
                        <input
                            type="password"
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            className="border rounded w-full px-3 py-2 mb-3"
                        />

                        <label className="block text-sm mb-2">Conferma nuova password</label>
                        <input
                            type="password"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            className="border rounded w-full px-3 py-2 mb-4"
                        />

                        {error && <div className="text-red-600 mb-2">{error}</div>}
                        {message && <div className="text-green-600 mb-2">{message}</div>}

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full px-3 py-2 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {loading ? 'Aggiornamento…' : 'Aggiorna password'}
                        </button>
                    </form>
                </div>
            </div>
        </div>
    )
}
