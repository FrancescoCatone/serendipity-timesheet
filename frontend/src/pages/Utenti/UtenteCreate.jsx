import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { createUtente } from '../../services/utentiService'
import PageBar from '../../components/PageBar'

const vuoto = {
    nome: '', cognome: '', email: '',
    codiceFiscale: '', password: '', ruolo: 'DIPENDENTE'
}

// regex come nel backend
const CF_REGEX = /^[A-Z0-9]{16}$/;
const NOME_REGEX = /^[A-Za-zÀ-ÿ\s'-]+$/;

export default function UtenteCreate() {
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

    // validazione locale 1:1 con i messaggi del backend
    const validate = (f) => {
        const errors = {}

        if (!f.codiceFiscale?.trim()) {
            errors.codiceFiscale = "Il codice fiscale è obbligatorio"
        } else if (f.codiceFiscale.trim().length !== 16) {
            errors.codiceFiscale = "Il codice fiscale deve contenere esattamente 16 caratteri"
        } else if (!CF_REGEX.test(f.codiceFiscale.trim().toUpperCase())) {
            errors.codiceFiscale = "Il codice fiscale deve contenere solo lettere maiuscole e numeri, 16 caratteri"
        }

        if (!f.nome?.trim()) {
            errors.nome = "Il nome è obbligatorio"
        } else if (f.nome.trim().length > 50) {
            errors.nome = "Il nome non può superare i 50 caratteri"
        } else if (!NOME_REGEX.test(f.nome.trim())) {
            errors.nome = "Il nome può contenere solo lettere e spazi"
        }

        if (!f.cognome?.trim()) {
            errors.cognome = "Il cognome è obbligatorio"
        } else if (f.cognome.trim().length > 50) {
            errors.cognome = "Il cognome non può superare i 50 caratteri"
        } else if (!NOME_REGEX.test(f.cognome.trim())) {
            errors.cognome = "Il cognome può contenere solo lettere e spazi"
        }

        if (!f.email?.trim()) {
            errors.email = "L'email è obbligatoria"
        } else {
            const SIMPLE_MAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!SIMPLE_MAIL.test(f.email.trim())) {
                errors.email = "Email non valida"
            }
        }

        if (!f.password) {
            errors.password = "La password è obbligatoria"
        } else if (f.password.length < 6 || f.password.length > 50) {
            errors.password = "La password deve contenere almeno 6 caratteri"
        }

        if (!f.ruolo) {
            errors.ruolo = "Il ruolo è obbligatorio"
        }

        return errors
    }

    // parser di errori che arrivano dal backend
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
        return { formError: 'Errore nella creazione utente' }
    }

    const onSubmit = async (e) => {
        e.preventDefault()
        setSubmitted(true)
        setFormError(null)

        // 1) validazione locale
        const localErrors = validate({
            ...form,
            codiceFiscale: form.codiceFiscale?.toUpperCase()
        })

        // 👉 Focus automatico sul primo campo non valido
        if (Object.keys(localErrors).length > 0) {
            setFieldErrors(localErrors)
            const firstKey = Object.keys(localErrors)[0]
            const el = document.querySelector(`[name="${firstKey}"]`)
            if (el) el.focus()
            return
        }

        // 2) chiamata API
        setSaving(true)
        try {
            await createUtente({
                nome: form.nome.trim(),
                cognome: form.cognome.trim(),
                email: form.email.trim(),
                codiceFiscale: form.codiceFiscale.trim().toUpperCase(),
                password: form.password,
                ruolo: form.ruolo,
            })
            navigate('/utenti', { replace: true, state: { flash: 'CREATED' } })
        } catch (err) {
            const { formError, fieldErrors } = parseValidationErrors(err)
            if (fieldErrors && Object.keys(fieldErrors).length) {
                setFieldErrors(fieldErrors)
                // focus anche sul primo errore proveniente dal backend
                const firstKey = Object.keys(fieldErrors)[0]
                const el = document.querySelector(`[name="${firstKey}"]`)
                if (el) el.focus()
            }
            if (!fieldErrors || Object.keys(fieldErrors).length === 0) {
                if (formError) setFormError(formError)
            }
        } finally {
            setSaving(false)
        }
    }

    const inputCls = "border rounded w-full px-3 py-2"
    const helpErr = (k) =>
        (submitted && fieldErrors[k]) ? <p className="text-red-600 text-sm mt-1">{fieldErrors[k]}</p> : null

    return (
        <div className="p-6">
            <PageBar title="Crea utente" />

            <div className="flex justify-center">
                <div className="w-full max-w-2xl bg-white shadow rounded-2xl p-6 border">
                    <h1 className="text-xl font-semibold mb-4">Nuovo utente</h1>

                    {formError && <div className="text-red-600 mb-3">{formError}</div>}

                    {/* noValidate elimina i popup nativi (email, required, …) */}
                    <form onSubmit={onSubmit} noValidate className="grid gap-4">
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm mb-1">Nome</label>
                                <input
                                    name="nome"
                                    className={inputCls}
                                    value={form.nome}
                                    onChange={(e) => setField('nome', e.target.value)}
                                />
                                {helpErr('nome')}
                            </div>
                            <div>
                                <label className="block text-sm mb-1">Cognome</label>
                                <input
                                    name="cognome"
                                    className={inputCls}
                                    value={form.cognome}
                                    onChange={(e) => setField('cognome', e.target.value)}
                                />
                                {helpErr('cognome')}
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm mb-1">Email</label>
                            <input
                                name="email"
                                className={inputCls}   // type="text" + noValidate → nessun popup nativo
                                value={form.email}
                                onChange={(e) => setField('email', e.target.value)}
                            />
                            {helpErr('email')}
                        </div>

                        <div>
                            <label className="block text-sm mb-1">Codice Fiscale</label>
                            <input
                                name="codiceFiscale"
                                className={inputCls}
                                value={form.codiceFiscale}
                                onChange={(e) => setField('codiceFiscale', e.target.value.toUpperCase())}
                            />
                            {helpErr('codiceFiscale')}
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm mb-1">Password</label>
                                <input
                                    name="password"
                                    type="password"
                                    className={inputCls}
                                    value={form.password}
                                    onChange={(e) => setField('password', e.target.value)}
                                />
                                {helpErr('password')}
                            </div>
                            <div>
                                <label className="block text-sm mb-1">Ruolo</label>
                                <select
                                    name="ruolo"
                                    className={inputCls}
                                    value={form.ruolo}
                                    onChange={(e) => setField('ruolo', e.target.value)}
                                >
                                    <option value="DIPENDENTE">DIPENDENTE</option>
                                    <option value="ADMIN">ADMIN</option>
                                </select>
                                {helpErr('ruolo')}
                            </div>
                        </div>

                        <div className="flex gap-2 pt-2">
                            <button
                                type="submit"
                                disabled={saving}
                                className="px-4 py-2 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
                            >
                                {saving ? 'Creazione…' : 'Crea utente'}
                            </button>

                            <Link to="/utenti" className="px-4 py-2 rounded border hover:bg-gray-50">
                                Annulla
                            </Link>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    )
}
