import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { getTimesheetByIdApi, updateTimesheetApi } from '../../api/timesheetApi';
import { getMyProfileApi, getUtentiApi } from '../../api/utentiApi';
import type { TimesheetStato } from '../../types/timesheet';
import type { ProfiloUtenteDto } from '../../types/utente';
import { getCurrentUserRole } from '../../utils/auth';
import { getErrorMessage } from '../../utils/error';

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

function ModificaTimesheetPage() {
    const navigate = useNavigate();
    const { id } = useParams();
    const role = getCurrentUserRole();

    const [form, setForm] = useState({
        mese: '',
        anno: '',
        utenteId: '',
        stato: 'APERTO' as TimesheetStato,
    });

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [userOptions, setUserOptions] = useState<UserOption[]>([]);
    const [currentUserLabel, setCurrentUserLabel] = useState('');

    const isReadOnly = useMemo(() => {
        if (form.stato === 'CHIUSO') {
            return true;
        }

        if (form.stato === 'CONFERMATO' && role !== 'ADMIN') {
            return true;
        }

        return false;
    }, [form.stato, role]);

    useEffect(() => {
        const loadData = async () => {
            if (!id || Number.isNaN(Number(id))) {
                toast.error('ID timesheet non valido');
                navigate('/app/timesheet', { replace: true });
                return;
            }

            try {
                setLoading(true);

                const timesheetResponse = await getTimesheetByIdApi(Number(id));
                const timesheet = timesheetResponse.data;

                if (!timesheet) {
                    toast.error('Timesheet non trovato');
                    navigate('/app/timesheet', { replace: true });
                    return;
                }

                setForm({
                    mese: String(timesheet.mese),
                    anno: String(timesheet.anno),
                    utenteId: String(timesheet.utenteId),
                    stato: timesheet.stato,
                });

                if (role === 'ADMIN') {
                    const utentiResponse = await getUtentiApi();
                    const utenti = utentiResponse.data ?? [];

                    setUserOptions(
                        utenti.map((utente) => ({
                            id: utente.id,
                            label: `${utente.nome} ${utente.cognome}`.trim(),
                        }))
                    );
                } else {
                    const profileResponse = await getMyProfileApi();
                    const profile = profileResponse.data as ProfiloUtenteDto | null;

                    if (profile?.id) {
                        setCurrentUserLabel(`${profile.nome} ${profile.cognome}`.trim());
                    }
                }
            } catch (error: unknown) {
                toast.error(getErrorMessage(error, 'Errore durante il caricamento del timesheet'));
                navigate('/app/timesheet', { replace: true });
            } finally {
                setLoading(false);
            }
        };

        void loadData();
    }, [id, navigate, role]);

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

        if (!id || Number.isNaN(Number(id))) {
            toast.error('ID timesheet non valido');
            return;
        }

        const validationError = validateForm();
        if (validationError) {
            toast.error(validationError);
            return;
        }

        try {
            setSaving(true);

            const response = await updateTimesheetApi(Number(id), {
                mese: Number(form.mese),
                anno: Number(form.anno),
                utenteId: Number(form.utenteId),
            });

            toast.success(response.message || 'Timesheet aggiornato con successo');
            navigate('/app/timesheet', { replace: true });
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’aggiornamento del timesheet'));
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div>
                <PageHeader title="Modifica timesheet" subtitle="Caricamento dati timesheet in corso" />
                <div className="module-placeholder">
                    <h2>Caricamento in corso</h2>
                    <p>Sto recuperando i dati del timesheet selezionato.</p>
                </div>
            </div>
        );
    }

    return (
        <div>
            <PageHeader title="Modifica timesheet" subtitle={`Stato corrente: ${form.stato}`} />

            <div className="form-card">
                {isReadOnly ? (
                    <div style={{ marginBottom: '1rem' }}>
                        <p className="page-subtitle">
                            Questo timesheet non è modificabile nello stato attuale.
                        </p>
                    </div>
                ) : null}

                <form onSubmit={handleSubmit} className="entity-form">
                    <div className="form-grid">
                        <div className="form-group">
                            <label htmlFor="mese">Mese</label>
                            <select
                                id="mese"
                                name="mese"
                                value={form.mese}
                                onChange={handleChange}
                                disabled={saving || isReadOnly}
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
                                disabled={saving || isReadOnly}
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
                                    disabled={saving || isReadOnly}
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
                            Torna alla lista
                        </button>

                        <button
                            type="submit"
                            className="primary-button form-submit-button"
                            disabled={saving || isReadOnly}
                        >
                            {saving ? 'Salvataggio...' : 'Salva modifiche'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default ModificaTimesheetPage;