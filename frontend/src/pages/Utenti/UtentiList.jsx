import { useEffect, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import useAuthStore from '../../store/authStore'
import { listUtenti, patchUtente, deleteUtente } from '../../services/utentiService'
import PageBar from '../../components/PageBar'

export default function UtentiList() {
    const role = useAuthStore(s => s.role)
    const isAdmin = role === 'ADMIN'
    const location = useLocation()
    const navigate = useNavigate()

    const [users, setUsers] = useState([])
    const [original, setOriginal] = useState([])
    const [loading, setLoading] = useState(true)
    const [editingId, setEditingId] = useState(null)
    const [savingId, setSavingId] = useState(null)
    const [deletingId, setDeletingId] = useState(null)
    const [error, setError] = useState(null)
    const [info, setInfo] = useState(null)

    if (!isAdmin) return <Navigate to="/" replace />

    // carica elenco
    useEffect(() => {
        (async () => {
            try {
                const data = await listUtenti()
                setUsers(data)
                setOriginal(JSON.parse(JSON.stringify(data)))
            } catch {
                setError('Errore nel caricamento utenti')
            } finally {
                setLoading(false)
            }
        })()
    }, [])

    // flash dopo creazione
    useEffect(() => {
        if (location.state?.flash === 'CREATED') {
            setInfo('Utente creato con successo ✅')
            navigate(location.pathname, { replace: true, state: {} })
            const t = setTimeout(() => setInfo(null), 2000)
            return () => clearTimeout(t)
        }
    }, [location.state, location.pathname, navigate])

    const onChangeField = (id, field, value) =>
        setUsers(prev => prev.map(u => (u.id === id ? { ...u, [field]: value } : u)))

    const buildPatch = (u) => {
        const o = original.find(x => x.id === u.id)
        if (!o) return {}
        const patch = {}
        for (const k of ['nome', 'cognome', 'email', 'codiceFiscale', 'ruolo']) {
            if (u[k] !== o[k]) patch[k] = u[k]
        }
        return patch
    }

    const onEdit = (id) => { setError(null); setInfo(null); setEditingId(id) }

    const onCancel = (id) => {
        const snapshot = original.find(x => x.id === id)
        setUsers(prev => prev.map(u => (u.id === id ? { ...snapshot } : u)))
        setEditingId(null)
    }

    const onSave = async (u) => {
        setSavingId(u.id); setError(null); setInfo(null)
        try {
            const patch = buildPatch(u)
            if (Object.keys(patch).length === 0) {
                setInfo('Nessuna modifica da salvare')
                return
            }
            await patchUtente(u.id, patch)
            setOriginal(prev => prev.map(x => (x.id === u.id ? { ...u } : x)))
            setInfo('Modifiche salvate')
            setEditingId(null)
        } catch {
            setError('Errore nel salvataggio utente')
        } finally {
            setSavingId(null)
            setTimeout(() => setInfo(null), 1500)
        }
    }

    const onDelete = async (id) => {
        if (!confirm('Eliminare questo utente?')) return
        setDeletingId(id); setError(null); setInfo(null)
        try {
            await deleteUtente(id)
            setUsers(prev => prev.filter(u => u.id !== id))
            setOriginal(prev => prev.filter(u => u.id !== id))
            setInfo('Utente eliminato')
        } catch {
            setError('Errore nella cancellazione utente')
        } finally {
            setDeletingId(null)
            setTimeout(() => setInfo(null), 1500)
        }
    }

    if (loading) return <div className="p-6">Caricamento…</div>
    if (error) return <div className="p-6 text-red-600">{error}</div>

    return (
        <div className="p-6">
            <PageBar title="Utenti" />
            {info && <div className="text-green-700 mb-3">{info}</div>}

            <div className="grid gap-4">
                {users.map(u => {
                    const editable = editingId === u.id
                    const inputCls = `w-full border rounded px-3 py-2 ${editable ? '' : 'bg-gray-100 cursor-not-allowed'}`
                    return (
                        <form key={u.id} className="bg-white border rounded-xl p-4 shadow-sm">
                            <div className="grid grid-cols-2 gap-3">
                                <label className="block">
                                    <span className="text-sm text-gray-600">Nome</span>
                                    <input disabled={!editable} className={inputCls}
                                        value={u.nome ?? ''} onChange={e => onChangeField(u.id, 'nome', e.target.value)} />
                                </label>
                                <label className="block">
                                    <span className="text-sm text-gray-600">Cognome</span>
                                    <input disabled={!editable} className={inputCls}
                                        value={u.cognome ?? ''} onChange={e => onChangeField(u.id, 'cognome', e.target.value)} />
                                </label>
                                <label className="block">
                                    <span className="text-sm text-gray-600">Email</span>
                                    <input disabled={!editable} className={inputCls}
                                        value={u.email ?? ''} onChange={e => onChangeField(u.id, 'email', e.target.value)} />
                                </label>
                                <label className="block">
                                    <span className="text-sm text-gray-600">Codice Fiscale</span>
                                    <input disabled={!editable} className={inputCls}
                                        value={u.codiceFiscale ?? ''} onChange={e => onChangeField(u.id, 'codiceFiscale', e.target.value)} />
                                </label>
                            </div>

                            <div className="mt-4 flex gap-2">
                                {!editable ? (
                                    <button type="button" onClick={() => onEdit(u.id)}
                                        className="px-3 py-1 rounded border border-blue-600 text-blue-600 hover:bg-blue-50">Modifica</button>
                                ) : (
                                    <>
                                        <button type="button" onClick={() => onSave(u)}
                                            disabled={savingId === u.id}
                                            className="px-3 py-1 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50">
                                            {savingId === u.id ? 'Salvataggio…' : 'Salva'}
                                        </button>
                                        <button type="button" onClick={() => onCancel(u.id)}
                                            className="px-3 py-1 rounded border text-gray-700 hover:bg-gray-50">Annulla</button>
                                    </>
                                )}
                                <button type="button" onClick={() => onDelete(u.id)}
                                    disabled={deletingId === u.id || editable}
                                    className="px-3 py-1 rounded border border-red-600 text-red-600 hover:bg-red-50 disabled:opacity-50"
                                    title={editable ? 'Chiudi modifica per eliminare' : 'Elimina utente'}>
                                    {deletingId === u.id ? 'Eliminazione…' : 'Elimina'}
                                </button>
                            </div>
                        </form>
                    )
                })}
            </div>
        </div>
    )
}
