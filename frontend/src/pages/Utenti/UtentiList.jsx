import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import useAuthStore from '../../store/authStore'
import { listUtenti, patchUtente, deleteUtente } from '../../services/utentiService'

export default function UtentiList() {
    const role = useAuthStore(s => s.role)
    const isAdmin = role === 'ADMIN'

    const [users, setUsers] = useState([])
    const [original, setOriginal] = useState([]) // snapshot per diff
    const [loading, setLoading] = useState(true)
    const [savingId, setSavingId] = useState(null)
    const [deletingId, setDeletingId] = useState(null)
    const [error, setError] = useState(null)
    const [info, setInfo] = useState(null)

    if (!isAdmin) return <Navigate to="/" replace />

    useEffect(() => {
        (async () => {
            try {
                const data = await listUtenti()
                setUsers(data)
                // snapshot profondo per diff
                setOriginal(JSON.parse(JSON.stringify(data)))
                console.debug('[UtentiList] caricati', data.length, 'utenti')
            } catch {
                setError('Errore nel caricamento utenti')
            } finally {
                setLoading(false)
            }
        })()
    }, [])

    const onChangeField = (id, field, value) => {
        setUsers(prev => prev.map(u => (u.id === id ? { ...u, [field]: value } : u)))
    }

    const buildPatch = (u) => {
        const o = original.find(x => x.id === u.id)
        if (!o) return {}
        const patch = {}
        for (const k of ['nome', 'cognome', 'email', 'codiceFiscale', 'ruolo']) {
            if (u[k] !== o[k]) patch[k] = u[k]
        }
        return patch
    }

    const onSave = async (u) => {
        setSavingId(u.id)
        setError(null)
        setInfo(null)
        try {
            const patch = buildPatch(u)
            console.debug('[UtentiList] onSave patch →', patch)
            if (Object.keys(patch).length === 0) {
                setInfo('Nessuna modifica da salvare')
                return
            }
            await patchUtente(u.id, patch)
            // aggiorna snapshot
            setOriginal(prev => prev.map(x => (x.id === u.id ? { ...u } : x)))
            setInfo('Modifiche salvate')
        } catch (e) {
            console.error(e)
            setError('Errore nel salvataggio utente')
        } finally {
            setSavingId(null)
            setTimeout(() => setInfo(null), 1500)
        }
    }

    const onDelete = async (id) => {
        if (!confirm('Eliminare questo utente?')) return
        setDeletingId(id)
        setError(null)
        setInfo(null)
        try {
            await deleteUtente(id)
            setUsers(prev => prev.filter(u => u.id !== id))
            setOriginal(prev => prev.filter(u => u.id !== id))
            setInfo('Utente eliminato')
        } catch (e) {
            console.error(e)
            setError('Errore nella cancellazione utente')
        } finally {
            setDeletingId(null)
            setTimeout(() => setInfo(null), 1500)
        }
    }

    if (loading) return <div className="p-6">Caricamento…</div>

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-4">Utenti</h1>
            {error && <div className="text-red-600 mb-3">{error}</div>}
            {info && <div className="text-green-700 mb-3">{info}</div>}

            <div className="grid gap-4">
                {users.map(u => (
                    <form key={u.id} className="bg-white border rounded-xl p-4 shadow-sm">
                        <div className="grid grid-cols-2 gap-3">
                            <label className="block">
                                <span className="text-sm text-gray-600">Nome</span>
                                <input
                                    className="w-full border rounded px-3 py-2"
                                    value={u.nome ?? ''}
                                    onChange={e => onChangeField(u.id, 'nome', e.target.value)}
                                />
                            </label>

                            <label className="block">
                                <span className="text-sm text-gray-600">Cognome</span>
                                <input
                                    className="w-full border rounded px-3 py-2"
                                    value={u.cognome ?? ''}
                                    onChange={e => onChangeField(u.id, 'cognome', e.target.value)}
                                />
                            </label>

                            <label className="block">
                                <span className="text-sm text-gray-600">Email</span>
                                <input
                                    className="w-full border rounded px-3 py-2"
                                    value={u.email ?? ''}
                                    onChange={e => onChangeField(u.id, 'email', e.target.value)}
                                />
                            </label>

                            <label className="block">
                                <span className="text-sm text-gray-600">Codice Fiscale</span>
                                <input
                                    className="w-full border rounded px-3 py-2"
                                    value={u.codiceFiscale ?? ''}
                                    onChange={e => onChangeField(u.id, 'codiceFiscale', e.target.value)}
                                />
                            </label>
                        </div>

                        <div className="mt-4 flex gap-2">
                            <button
                                type="button"
                                onClick={() => onSave(u)}
                                disabled={savingId === u.id}
                                className="border rounded px-3 py-1"
                            >
                                {savingId === u.id ? 'Salvataggio…' : 'Modifica'}
                            </button>

                            <button
                                type="button"
                                onClick={() => onDelete(u.id)}
                                disabled={deletingId === u.id}
                                className="border rounded px-3 py-1"
                            >
                                {deletingId === u.id ? 'Eliminazione…' : 'Elimina'}
                            </button>
                        </div>
                    </form>
                ))}
            </div>
        </div>
    )
}
