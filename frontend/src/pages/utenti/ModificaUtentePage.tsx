import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { getErrorMessage } from '../../utils/error';
import { getUtenteByIdApi, updateUtenteApi } from '../../api/utentiApi';
import { isValidEmail } from '../../utils/validation';

function ModificaUtentePage() {
    const navigate = useNavigate();
    const { id } = useParams();

    const [form, setForm] = useState({
        codiceFiscale: '',
        nome: '',
        cognome: '',
        email: '',
        password: '',
        ruolo: 'DIPENDENTE' as 'ADMIN' | 'DIPENDENTE',
    });

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const loadUtente = async () => {
            if (!id || Number.isNaN(Number(id))) {
                toast.error('ID utente non valido');
                navigate('/app/utenti', { replace: true });
                return;
            }

            try {
                setLoading(true);

                const response = await getUtenteByIdApi(Number(id));
                const utente = response.data;

                if (!utente) {
                    toast.error('Utente non trovato');
                    navigate('/app/utenti', { replace: true });
                    return;
                }

                setForm({
                    codiceFiscale: utente.codiceFiscale ?? '',
                    nome: utente.nome ?? '',
                    cognome: utente.cognome ?? '',
                    email: utente.email ?? '',
                    password: '',
                    ruolo: utente.ruolo === 'ADMIN' ? 'ADMIN' : 'DIPENDENTE',
                });
            } catch (error: unknown) {
                toast.error(getErrorMessage(error, 'Errore durante il caricamento dell’utente'));
                navigate('/app/utenti', { replace: true });
            } finally {
                setLoading(false);
            }
        };

        loadUtente();
    }, [id, navigate]);

    const handleChange = (
        event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
    ) => {
        const { name, value } = event.target;

        setForm((prev) => ({
            ...prev,
            [name]: name === 'codiceFiscale' ? value.toUpperCase() : value,
        }));
    };

    const validateForm = (): string | null => {
        const {
            codiceFiscale,
            nome,
            cognome,
            email,
            password: plainTextValue,
            ruolo,
        } = form;

        const cf = codiceFiscale.trim().toUpperCase();
        const firstName = nome.trim();
        const lastName = cognome.trim();
        const emailValue = email.trim();

        if (!cf || !firstName || !lastName || !emailValue || !plainTextValue || !ruolo) {
            return 'Compila tutti i campi obbligatori';
        }

        if (cf.length !== 16) {
            return 'Il codice fiscale deve contenere esattamente 16 caratteri';
        }

        if (!/^[A-Z0-9]{16}$/.test(cf)) {
            return 'Il codice fiscale deve contenere solo lettere maiuscole e numeri';
        }

        if (plainTextValue.length < 6) {
            return 'La password deve contenere almeno 6 caratteri';
        }

        if (!isValidEmail(emailValue)) {
            return "Inserisci un'email valida completa di dominio finale, ad esempio nome@dominio.it";
        }

        return null;
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!id || Number.isNaN(Number(id))) {
            toast.error('ID utente non valido');
            return;
        }

        const validationError = validateForm();
        if (validationError) {
            toast.error(validationError);
            return;
        }

        try {
            setSaving(true);

            const response = await updateUtenteApi(Number(id), {
                codiceFiscale: form.codiceFiscale.trim().toUpperCase(),
                nome: form.nome.trim(),
                cognome: form.cognome.trim(),
                email: form.email.trim(),
                password: form.password,
                ruolo: form.ruolo,
            });

            toast.success(response.message || 'Utente aggiornato con successo');
            navigate('/app/utenti', { replace: true });
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’aggiornamento dell’utente'));
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div>
                <PageHeader
                    title="Modifica utente"
                    subtitle="Caricamento dati utente in corso"
                />
                <div className="module-placeholder">
                    <h2>Caricamento in corso</h2>
                    <p>Sto recuperando i dati dell’utente dal backend.</p>
                </div>
            </div>
        );
    }

    return (
        <div>
            <PageHeader
                title="Modifica utente"
                subtitle="Aggiorna i dati dell’anagrafica utente"
            />

            <div className="form-card">
                <form onSubmit={handleSubmit} className="entity-form">
                    <div className="form-grid">
                        <div className="form-group">
                            <label htmlFor="codiceFiscale">Codice fiscale</label>
                            <input
                                id="codiceFiscale"
                                name="codiceFiscale"
                                type="text"
                                maxLength={16}
                                value={form.codiceFiscale}
                                onChange={handleChange}
                                disabled={saving}
                                placeholder="Inserisci il codice fiscale"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="nome">Nome</label>
                            <input
                                id="nome"
                                name="nome"
                                type="text"
                                value={form.nome}
                                onChange={handleChange}
                                disabled={saving}
                                placeholder="Inserisci il nome"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="cognome">Cognome</label>
                            <input
                                id="cognome"
                                name="cognome"
                                type="text"
                                value={form.cognome}
                                onChange={handleChange}
                                disabled={saving}
                                placeholder="Inserisci il cognome"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="email">Email</label>
                            <input
                                id="email"
                                name="email"
                                type="email"
                                value={form.email}
                                onChange={handleChange}
                                disabled={saving}
                                placeholder="Inserisci l'email"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="password">Password</label>
                            <input
                                id="password"
                                name="password"
                                type="password"
                                value={form.password}
                                onChange={handleChange}
                                disabled={saving}
                                placeholder="Reinserisci la password"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="ruolo">Ruolo</label>
                            <select
                                id="ruolo"
                                name="ruolo"
                                value={form.ruolo}
                                onChange={handleChange}
                                disabled={saving}
                                className="form-select"
                            >
                                <option value="ADMIN">ADMIN</option>
                                <option value="DIPENDENTE">DIPENDENTE</option>
                            </select>
                        </div>
                    </div>

                    <div className="form-actions">
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={() => navigate('/app/utenti')}
                            disabled={saving}
                        >
                            Annulla
                        </button>

                        <button
                            type="submit"
                            className="primary-button form-submit-button"
                            disabled={saving}
                        >
                            {saving ? 'Salvataggio...' : 'Salva modifiche'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default ModificaUtentePage;
