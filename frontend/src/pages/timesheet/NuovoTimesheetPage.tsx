import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { createTimesheetApi } from '../../api/timesheetApi';
import { getMyProfileApi, getUtentiApi } from '../../api/utentiApi';
import { getCurrentUserRole } from '../../utils/auth';
import { excludeAdminUsers } from '../../utils/entityFilters';
import { getErrorMessage } from '../../utils/error';
import type { ProfiloUtenteDto } from '../../types/utente';

type UserOption = {
    id: number;
    label: string;
};

const MONTH_OPTIONS = [
    { value: 1, label: 'Gennaio' },
    { value: 2, label: 'Febbraio' },
    { value: 3, label: 'Marzo' },
    { value: 4, label: 'Aprile' },
    { value: 5, label: 'Maggio' },
    { value: 6, label: 'Giugno' },
    { value: 7, label: 'Luglio' },
    { value: 8, label: 'Agosto' },
    { value: 9, label: 'Settembre' },
    { value: 10, label: 'Ottobre' },
    { value: 11, label: 'Novembre' },
    { value: 12, label: 'Dicembre' },
];

function NuovoTimesheetPage() {
    const navigate = useNavigate();
    const role = getCurrentUserRole();
    const today = new Date();

    const [form, setForm] = useState({
        mese: String(today.getMonth() + 1),
        anno: String(today.getFullYear()),
        utenteId: '',
    });

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [userOptions, setUserOptions] = useState<UserOption[]>([]);
    const [currentUserLabel, setCurrentUserLabel] = useState('');

    useEffect(() => {
        const bootstrap = async () => {
            try {
                setLoading(true);

                if (role === 'ADMIN') {
                    const response = await getUtentiApi();
                    const utenti = excludeAdminUsers(response.data ?? []);

                    setUserOptions(
                        utenti.map((utente) => ({
                            id: utente.id,
                            label: `${utente.nome} ${utente.cognome}`.trim(),
                        }))
                    );
                } else {
                    const response = await getMyProfileApi();
                    const profile = response.data as ProfiloUtenteDto | null;

                    if (!profile?.id) {
                        throw new Error('Impossibile determinare l’utente corrente');
                    }

                    setForm((prev) => ({
                        ...prev,
                        utenteId: String(profile.id),
                    }));

                    setCurrentUserLabel(`${profile.nome} ${profile.cognome}`.trim());
                }
            } catch (error: unknown) {
                toast.error(getErrorMessage(error, 'Errore durante il caricamento dei dati iniziali'));
                navigate('/app/timesheet', { replace: true });
            } finally {
                setLoading(false);
            }
        };

        void bootstrap();
    }, [navigate, role]);

    const handleChange = (
        event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
    ) => {
        const { name, value } = event.target;

        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const validateForm = (): string | null => {
        const mese = Number(form.mese);
        const anno = Number(form.anno);
        const utenteId = Number(form.utenteId);

        if (!mese || !anno || !utenteId) {
            return 'Compila tutti i campi obbligatori';
        }

        if (mese < 1 || mese > 12) {
            return 'Il mese deve essere compreso tra 1 e 12';
        }

        if (anno < 2000) {
            return 'L’anno deve essere maggiore o uguale a 2000';
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
            setSaving(true);

            const response = await createTimesheetApi({
                mese: Number(form.mese),
                anno: Number(form.anno),
                utenteId: Number(form.utenteId),
            });

            toast.success(response.message || 'Timesheet creato con successo');

            const created = response.data;
            if (created?.id) {
                navigate(`/app/timesheet/${created.id}/modifica`, { replace: true });
            } else {
                navigate('/app/timesheet', { replace: true });
            }
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante la creazione del timesheet'));
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div>
                <PageHeader title="Nuovo timesheet" subtitle="Caricamento dati iniziali in corso" />
                <div className="module-placeholder">
                    <h2>Caricamento in corso</h2>
                    <p>Sto preparando i dati necessari per creare un nuovo timesheet.</p>
                </div>
            </div>
        );
    }

    return (
        <div>
            <PageHeader
                title="Nuovo timesheet"
                subtitle="Compila i dati per creare un nuovo timesheet mensile"
            />

            <div className="form-card">
                <form onSubmit={handleSubmit} className="entity-form">
                    <div className="form-grid">
                        <div className="form-group">
                            <label htmlFor="mese">Mese</label>
                            <select
                                id="mese"
                                name="mese"
                                value={form.mese}
                                onChange={handleChange}
                                disabled={saving}
                                className="form-select"
                            >
                                {MONTH_OPTIONS.map((month) => (
                                    <option key={month.value} value={month.value}>
                                        {month.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="form-group">
                            <label htmlFor="anno">Anno</label>
                            <input
                                id="anno"
                                name="anno"
                                type="number"
                                min={2000}
                                value={form.anno}
                                onChange={handleChange}
                                disabled={saving}
                                placeholder="Inserisci l'anno"
                            />
                        </div>

                        {role === 'ADMIN' ? (
                            <div className="form-group">
                                <label htmlFor="utenteId">Utente</label>
                                <select
                                    id="utenteId"
                                    name="utenteId"
                                    value={form.utenteId}
                                    onChange={handleChange}
                                    disabled={saving}
                                    className="form-select"
                                >
                                    <option value="">Seleziona utente</option>
                                    {userOptions.map((user) => (
                                        <option key={user.id} value={user.id}>
                                            {user.label}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        ) : (
                            <div className="form-group">
                                <label htmlFor="utenteDisplay">Utente</label>
                                <input
                                    id="utenteDisplay"
                                    name="utenteDisplay"
                                    type="text"
                                    value={currentUserLabel}
                                    disabled
                                />
                            </div>
                        )}
                    </div>

                    <div className="form-actions">
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={() => navigate('/app/timesheet')}
                            disabled={saving}
                        >
                            Annulla
                        </button>

                        <button
                            type="submit"
                            className="primary-button form-submit-button"
                            disabled={saving}
                        >
                            {saving ? 'Salvataggio...' : 'Crea timesheet'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default NuovoTimesheetPage;
