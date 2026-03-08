import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { createUtenteApi } from '../../api/utentiApi';
import { getErrorMessage } from '../../utils/error';
import type { Ruolo } from '../../types/utente';

function NuovoUtentePage() {
    const navigate = useNavigate();

    const [form, setForm] = useState<{
        codiceFiscale: string;
        nome: string;
        cognome: string;
        email: string;
        password: string;
        ruolo: Ruolo;
    }>({
        codiceFiscale: '',
        nome: '',
        cognome: '',
        email: '',
        password: '',
        ruolo: 'DIPENDENTE',
    });

    const [loading, setLoading] = useState(false);

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
            password: pwd,
            ruolo,
        } = form;

        const cf = codiceFiscale.trim().toUpperCase();
        const firstName = nome.trim();
        const lastName = cognome.trim();
        const emailValue = email.trim();

        if (!cf || !firstName || !lastName || !emailValue || !pwd || !ruolo) {
            return 'Compila tutti i campi obbligatori';
        }

        if (cf.length !== 16) {
            return 'Il codice fiscale deve contenere esattamente 16 caratteri';
        }

        if (!/^[A-Z0-9]{16}$/.test(cf)) {
            return 'Il codice fiscale deve contenere solo lettere maiuscole e numeri';
        }

        if (pwd.length < 6) {
            return 'La password deve contenere almeno 6 caratteri';
        }

        return null;
    };
    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        const validationError = validateForm();
        if (validationError) {
            toast.error(validationError);
            return;
        }

        try {
            setLoading(true);

            const response = await createUtenteApi({
                codiceFiscale: form.codiceFiscale.trim().toUpperCase(),
                nome: form.nome.trim(),
                cognome: form.cognome.trim(),
                email: form.email.trim(),
                password: form.password,
                ruolo: form.ruolo,
            });

            toast.success(response.message || 'Utente creato con successo');
            navigate('/app/utenti', { replace: true });
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante la creazione dell’utente'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <PageHeader
                title="Nuovo utente"
                subtitle="Compila i dati per creare una nuova anagrafica utente"
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
                                disabled={loading}
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
                                disabled={loading}
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
                                disabled={loading}
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
                                disabled={loading}
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
                                disabled={loading}
                                placeholder="Inserisci la password"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="ruolo">Ruolo</label>
                            <select
                                id="ruolo"
                                name="ruolo"
                                value={form.ruolo}
                                onChange={handleChange}
                                disabled={loading}
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
                            disabled={loading}
                        >
                            Annulla
                        </button>

                        <button type="submit" className="primary-button form-submit-button" disabled={loading}>
                            {loading ? 'Salvataggio...' : 'Crea utente'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default NuovoUtentePage;