import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import PageBar from '../../components/PageBar'
import { createCliente } from '../../services/clientiService'

const vuoto = { nome: '', tariffaOraria: '' }

export default function ClienteCreate() {
    const navigate = useNavigate()
    const [form, setForm] = useState(vuoto)
    const [saving, setSaving] = useState(false)
    const [submitted, setSubmitted] = useState(false)
    const [formError, setFormError] = useState(null)
    const [fieldErrors, setFieldErrors] = useState({})

    const setField = (k, v) => {
        setForm(prev => ({ ...prev, [k]: v }))
        setFieldErrors(prev => {
            if (!(k in prev)) return prev
            const copy = { ...prev }
            delete copy[k]
            return copy
        })
    }

    const validate = (f) => {
        const errors = {}
        const nome = (f.nome ?? '').trim()
        if (!nome) errors.nome = 'Il nome del cliente è obbligatorio'
        else if (nome.length > 100) errors.nome = 'Il nome del cliente non può superare 100 caratteri'

        const t = Number(f.tariffaOraria)
        if (!f.tariffaOraria?.toString().trim()) errors.tariffaOraria = 'La tariffa oraria è obbligatoria'
        else if (!(t > 0)) errors.tariffaOraria = 'La tariffa oraria deve essere maggiore di 0'

        return errors
    }

    function parseValidationErrors(err) {
        const data = err?.response?.data

        if (Array.isArray(data?.errors)) {
            const map = {}
            for (const e of data.errors) {
                const field = (e.field || e.property || '').toString()
                if (field) map[field] = e.message || e.defaultMessage || 'Valore non valido'
            }
            return { fieldErrors: map }
        }

        if (Array.isArray(data?.fieldErrors)) {
            const map = {}
            for (const e of data.fieldErrors) map[e.field] = e.defaultMessage || 'Valore non valido'
            return { fieldErrors: map }
        }

        if (Array.isArray(data?.violations)) {
            const map = {}
            for (const v of data.violations) map[v.fieldName || v.field] = v.message
            return { fieldErrors: map }
        }

        if (typeof data?.message === 'string') return { formError: data.message }
        return { formError: 'Errore nella creazione cliente' }
    }

    const onSubmit = async (e) => {
        e.preventDefault()
        setSubmitted(true)
        setFormError(null)

        const localErrors = validate(form)
        if (Object.keys(localErrors).length > 0) {
            setFieldErrors(localErrors)
            const firstKey = Object.keys(localErrors)[0]
            const el = document.querySelector(`[name="${firstKey}"]`)
            if (el) el.focus()
            return
        }

        setSaving(true)
        try {
            await createCliente({
                nome: form.nome.trim(),
                tariffaOraria: Number(form.tariffaOraria)
            })
            navigate('/clienti', { replace: true, state: { flash: 'CREATED' } })
        } catch (err) {
            if (err?.response?.status === 409) {
                setFieldErrors({ nome: 'Esiste già un cliente con questo nome' })
            } else {
                const { formError, fieldErrors } = parseValidationErrors(err)
                if (fieldErrors && Object.keys(fieldErrors).length) {
                    setFieldErrors(fieldErrors)
                    const firstKey = Object.keys(fieldErrors)[0]
                    const el = document.querySelector(`[name="${firstKey}"]`)
                    if (el) el.focus()
                } else if (formError) {
                    setFormError(formError)
                }
            }
        } finally {
            setSaving(false)
        }
    }

    const inputCls = 'border rounded w-full px-3 py-2'
    const helpErr = (k) =>
        (submitted && fieldErrors[k]) ? <p className="text-red-600 text-sm mt-1">{fieldErrors[k]}</p> : null

    return (
        <div className="p-6">
            <PageBar title="Crea cliente" />

            <div className="flex justify-center">
                <div className="w-full max-w-2xl bg-white shadow rounded-2xl p-6 border">
                    <h1 className="text-xl font-semibold mb-4">Nuovo cliente</h1>

                    {formError && <div className="text-red-600 mb-3">{formError}</div>}

                    <form onSubmit={onSubmit} noValidate className="grid gap-4">
                        <div className="grid grid-cols-2 gap-4">
                            <div className="col-span-2">
                                <label className="block text-sm mb-1">Nome</label>
                                <input
                                    name="nome"
                                    className={inputCls}
                                    value={form.nome}
                                    onChange={e => setField('nome', e.target.value)}
                                />
                                {helpErr('nome')}
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm mb-1">Tariffa oraria</label>
                                <input
                                    name="tariffaOraria"
                                    type="number"
                                    step="0.01"
                                    className={inputCls}
                                    value={form.tariffaOraria}
                                    onChange={e => setField('tariffaOraria', e.target.value)}
                                />
                                {helpErr('tariffaOraria')}
                            </div>
                        </div>

                        <div className="flex gap-2 pt-2">
                            <button
                                type="submit"
                                disabled={saving}
                                className="px-4 py-2 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
                            >
                                {saving ? 'Creazione…' : 'Crea cliente'}
                            </button>

                            <Link to="/clienti" className="px-4 py-2 rounded border hover:bg-gray-50">
                                Annulla
                            </Link>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    )
}
