import { useEffect, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import useAuthStore from '../../store/authStore'
import { listClienti, patchCliente, deleteCliente } from '../../services/clientiService'
import PageBar from '../../components/PageBar'

export default function ClientiList() {
    const role = useAuthStore(s => s.role)
    const isAdmin = role === 'ADMIN'
    const location = useLocation()
    const navigate = useNavigate()

    const [clienti, setClienti] = useState([])
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
                const data = await listClienti()
                setClienti(data)
                setOriginal(JSON.parse(JSON.stringify(data)))
            } catch {
                setError('Errore nel caricamento clienti')
            } finally {
                setLoading(false)
            }
        })()
    }, [])

    // flash dopo creazione (mostra + pulisce history state)
    useEffect(() => {
        if (location.state?.flash === 'CREATED') {
            setInfo('Cliente creato con successo ✅')
            navigate(location.pathname, { replace: true, state: {} })
        }
    }, [location.state, location.pathname, navigate])

    // auto-dismiss per qualunque info
    useEffect(() => {
        if (!info) return
        const t = setTimeout(() => setInfo(null), 2500)
        return () => clearTimeout(t)
    }, [info])

    const onChangeField = (id, field, value) =>
        setClienti(prev => prev.map(c => (c.id === id ? { ...c, [field]: value } : c)))

    const buildPatch = (c) => {
        const o = original.find(x => x.id === c.id)
        if (!o) return {}
        const patch = {}
        for (const k of ['nome', 'tariffaOraria']) {
            if (c[k] !== o[k]) patch[k] = c[k]
        }
        return patch
    }

    const onEdit = (id) => { setError(null); setInfo(null); setEditingId(id) }

    const onCancel = (id) => {
        const snapshot = original.find(x => x.id === id)
        setClienti(prev => prev.map(c => (c.id === id ? { ...snapshot } : c)))
        setEditingId(null)
    }

    const onSave = async (c) => {
        setSavingId(c.id); setError(null); setInfo(null)
        try {
            const patch = buildPatch(c)
            if (Object.keys(patch).length === 0) {
                setInfo('Nessuna modifica da salvare')
                return
            }
            // validazione locale coerente con DTO
            if ('nome' in patch) {
                const nome = (patch.nome ?? '').trim()
                if (!nome) throw { _local: 'Il nome del cliente è obbligatorio' }
                if (nome.length > 100) throw { _local: 'Il nome del cliente non può superare 100 caratteri' }
                patch.nome = nome
            }
            if ('tariffaOraria' in patch) {
                const t = Number(patch.tariffaOraria)
                if (!(t > 0)) throw { _local: 'La tariffa oraria deve essere maggiore di 0' }
                patch.tariffaOraria = t
            }

            await patchCliente(c.id, patch)
            setOriginal(prev => prev.map(x => (x.id === c.id ? { ...c, ...patch } : x)))
            setInfo('Modifiche salvate')
            setEditingId(null)
        } catch (e) {
            if (e?._local) setError(e._local)
            else if (e?.response?.status === 409) setError('Esiste già un cliente con questo nome')
            else setError('Errore nel salvataggio cliente')
        } finally {
            setSavingId(null)
            setTimeout(() => setInfo(null), 1500)
        }
    }

    const onDelete = async (id) => {
        if (!confirm('Eliminare questo cliente?')) return
        setDeletingId(id); setError(null); setInfo(null)
        try {
            await deleteCliente(id)
            setClienti(prev => prev.filter(c => c.id !== id))
            setOriginal(prev => prev.filter(c => c.id !== id))
            setInfo('Cliente eliminato')
        } catch {
            setError('Errore nella cancellazione cliente')
        } finally {
            setDeletingId(null)
            setTimeout(() => setInfo(null), 1500)
        }
    }

    if (loading) return <div className="p-6">Caricamento…</div>
    if (error) return <div className="p-6 text-red-600">{error}</div>

    return (
        <div className="p-6">
            <PageBar title="Clienti" />
            {info && <div className="text-green-700 mb-3">{info}</div>}

            <div className="grid gap-4">
                {clienti.map(c => {
                    const editable = editingId === c.id
                    const inputCls = `w-full border rounded px-3 py-2 ${editable ? '' : 'bg-gray-100 cursor-not-allowed'}`
                    return (
                        <form key={c.id} className="bg-white border rounded-xl p-4 shadow-sm">
                            <div className="grid grid-cols-2 gap-3">
                                <label className="block">
                                    <span className="text-sm text-gray-600">Nome</span>
                                    <input
                                        disabled={!editable}
                                        className={inputCls}
                                        value={c.nome ?? ''}
                                        onChange={e => onChangeField(c.id, 'nome', e.target.value)}
                                    />
                                </label>
                                <label className="block">
                                    <span className="text-sm text-gray-600">Tariffa oraria</span>
                                    <input
                                        disabled={!editable}
                                        className={inputCls}
                                        type="number"
                                        step="0.01"
                                        value={c.tariffaOraria ?? ''}
                                        onChange={e => onChangeField(c.id, 'tariffaOraria', e.target.value)}
                                    />
                                </label>
                            </div>

                            <div className="mt-4 flex gap-2">
                                {!editable ? (
                                    <button
                                        type="button"
                                        onClick={() => onEdit(c.id)}
                                        className="px-3 py-1 rounded border border-blue-600 text-blue-600 hover:bg-blue-50"
                                    >
                                        Modifica
                                    </button>
                                ) : (
                                    <>
                                        <button
                                            type="button"
                                            onClick={() => onSave(c)}
                                            disabled={savingId === c.id}
                                            className="px-3 py-1 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
                                        >
                                            {savingId === c.id ? 'Salvataggio…' : 'Salva'}
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => onCancel(c.id)}
                                            className="px-3 py-1 rounded border text-gray-700 hover:bg-gray-50"
                                        >
                                            Annulla
                                        </button>
                                    </>
                                )}
                                <button
                                    type="button"
                                    onClick={() => onDelete(c.id)}
                                    disabled={deletingId === c.id || editable}
                                    className="px-3 py-1 rounded border border-red-600 text-red-600 hover:bg-red-50 disabled:opacity-50"
                                    title={editable ? 'Chiudi modifica per eliminare' : 'Elimina cliente'}
                                >
                                    {deletingId === c.id ? 'Eliminazione…' : 'Elimina'}
                                </button>
                            </div>
                        </form>
                    )
                })}
            </div>
        </div>
    )
}
