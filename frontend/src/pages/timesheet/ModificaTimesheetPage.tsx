import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { getTimesheetByIdApi, updateTimesheetApi } from '../../api/timesheetApi';
import {
    createTimesheetRigaApi,
    deleteTimesheetRigaApi,
    getTimesheetRigheByTimesheetApi,
    updateTimesheetRigaApi,
} from '../../api/timesheetRigheApi';
import { getUtentiApi } from '../../api/utentiApi';
import { getClientiApi } from '../../api/clientiApi';
import type { ClienteDto } from '../../types/cliente';
import type { TimesheetStato } from '../../types/timesheet';
import type { TimesheetRigaDto } from '../../types/timesheetRiga';
import { getCurrentUserRole } from '../../utils/auth';
import { excludeAdminUsers, NON_LAVORATO_CLIENT_NAME } from '../../utils/entityFilters';
import { getErrorMessage } from '../../utils/error';
import BackButton from '../../components/common/BackButton';
import { isFestivoItaliano } from '../../utils/calendar';

type UserOption = {
    id: number;
    label: string;
};

type RigaFormState = {
    clienteId: string;
    data: string;
    dataFine: string;
    ore: string;
    minuti: string;
};

type RigaFormErrors = Partial<Record<keyof RigaFormState, string>>;

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

function getMonthLabel(month: number): string {
    return MONTH_OPTIONS.find((item) => item.value === month)?.label ?? String(month);
}

function buildDefaultRigaDate(mese: number, anno: number): string {
    return `${anno}-${String(mese).padStart(2, '0')}-01`;
}

function formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    if (!year || !month || !day) {
        return value;
    }

    return `${day}/${month}/${year}`;
}

function formatCurrency(value: number): string {
    return `€ ${value.toFixed(2)}`;
}

function getMonthDates(mese: number, anno: number): string[] {
    const daysInMonth = new Date(anno, mese, 0).getDate();

    return Array.from({ length: daysInMonth }, (_, index) => {
        const day = String(index + 1).padStart(2, '0');
        return `${anno}-${String(mese).padStart(2, '0')}-${day}`;
    });
}

function getDatesInRange(start: string, end: string): string[] {
    const startDate = new Date(`${start}T00:00:00`);
    const endDate = new Date(`${end}T00:00:00`);

    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime()) || startDate > endDate) {
        return [];
    }

    const dates: string[] = [];
    const current = new Date(startDate);

    while (current <= endDate) {
        const year = current.getFullYear();
        const month = String(current.getMonth() + 1).padStart(2, '0');
        const day = String(current.getDate()).padStart(2, '0');
        dates.push(`${year}-${month}-${day}`);
        current.setDate(current.getDate() + 1);
    }

    return dates;
}

function ModificaTimesheetPage() {
    const navigate = useNavigate();
    const { id } = useParams();
    const role = getCurrentUserRole();
    const isAdmin = role === 'ADMIN';

    const numericTimesheetId = Number(id);

    const [form, setForm] = useState({
        mese: '',
        anno: '',
        utenteId: '',
        stato: 'APERTO' as TimesheetStato,
    });

    const [timesheetInfo, setTimesheetInfo] = useState<{
        mese: number;
        anno: number;
    } | null>(null);

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    const [userOptions, setUserOptions] = useState<UserOption[]>([]);

    const [clienti, setClienti] = useState<ClienteDto[]>([]);
    const [clientiLoading, setClientiLoading] = useState(false);

    const [righe, setRighe] = useState<TimesheetRigaDto[]>([]);
    const [righeLoading, setRigheLoading] = useState(false);
    const [righeSaving, setRigheSaving] = useState(false);
    const [editingRigaId, setEditingRigaId] = useState<number | null>(null);
    const [deletingRigaId, setDeletingRigaId] = useState<number | null>(null);
    const [rigaDaEliminare, setRigaDaEliminare] = useState<TimesheetRigaDto | null>(null);

    const [rigaForm, setRigaForm] = useState<RigaFormState>({
        clienteId: '',
        data: '',
        dataFine: '',
        ore: '',
        minuti: '0',
    });
    const [rigaFormErrors, setRigaFormErrors] = useState<RigaFormErrors>({});

    const isReadOnly = useMemo(() => {
        return form.stato === 'CONFERMATO' || form.stato === 'CHIUSO';
    }, [form.stato]);

    const pageTitle = isReadOnly ? 'Visualizza timesheet' : 'Modifica timesheet';

    const selectedCliente = useMemo(() => {
        const selectedId = Number(rigaForm.clienteId);
        if (!selectedId) {
            return null;
        }

        return clienti.find((cliente) => cliente.id === selectedId) ?? null;
    }, [clienti, rigaForm.clienteId]);

    const isNonLavoratoSelected = selectedCliente?.nome?.toUpperCase() === NON_LAVORATO_CLIENT_NAME;

    const missingRequiredDates = useMemo(() => {
        if (!timesheetInfo) {
            return [];
        }

        const compiledDates = new Set(righe.map((riga) => riga.data));

        return getMonthDates(timesheetInfo.mese, timesheetInfo.anno)
            .filter((date) => !isFestivoItaliano(date))
            .filter((date) => !compiledDates.has(date));
    }, [righe, timesheetInfo]);

    const resetRigaForm = useCallback((mese?: number, anno?: number) => {
        setRigaForm({
            clienteId: '',
            data: mese && anno ? buildDefaultRigaDate(mese, anno) : '',
            dataFine: mese && anno ? buildDefaultRigaDate(mese, anno) : '',
            ore: '',
            minuti: '0',
        });
        setRigaFormErrors({});
        setEditingRigaId(null);
    }, []);

    const loadClienti = useCallback(async () => {
        try {
            setClientiLoading(true);
            const response = await getClientiApi();
            setClienti(response.data ?? []);
        } catch (error: unknown) {
            setClienti([]);
            toast.error(getErrorMessage(error, 'Errore durante il caricamento dei clienti'));
        } finally {
            setClientiLoading(false);
        }
    }, []);

    const loadRighe = useCallback(async (timesheetId: number) => {
        try {
            setRigheLoading(true);
            const response = await getTimesheetRigheByTimesheetApi(timesheetId);
            const righeOrdinate = [...(response.data ?? [])].sort((a, b) => {
                const byDate = a.data.localeCompare(b.data);
                if (byDate !== 0) {
                    return byDate;
                }

                const byCliente = a.clienteNome.localeCompare(b.clienteNome);
                if (byCliente !== 0) {
                    return byCliente;
                }

                return a.id - b.id;
            });

            setRighe(righeOrdinate);
        } catch (error: unknown) {
            setRighe([]);
            toast.error(getErrorMessage(error, 'Errore durante il caricamento delle righe timesheet'));
        } finally {
            setRigheLoading(false);
        }
    }, []);

    useEffect(() => {
        const loadData = async () => {
            if (!id || Number.isNaN(numericTimesheetId)) {
                toast.error('ID timesheet non valido');
                navigate('/app/timesheet', { replace: true });
                return;
            }

            try {
                setLoading(true);

                const timesheetResponse = await getTimesheetByIdApi(numericTimesheetId);
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

                setTimesheetInfo({
                    mese: timesheet.mese,
                    anno: timesheet.anno,
                });

                resetRigaForm(timesheet.mese, timesheet.anno);

                const asyncTasks: Promise<unknown>[] = [
                    loadClienti(),
                    loadRighe(timesheet.id),
                ];

                if (role === 'ADMIN') {
                    asyncTasks.push(
                        getUtentiApi().then((utentiResponse) => {
                            const utenti = excludeAdminUsers(utentiResponse.data ?? []);
                            setUserOptions(
                                utenti.map((utente) => ({
                                    id: utente.id,
                                    label: `${utente.nome} ${utente.cognome}`.trim(),
                                }))
                            );
                        })
                    );
                }

                await Promise.all(asyncTasks);
            } catch (error: unknown) {
                toast.error(getErrorMessage(error, 'Errore durante il caricamento del timesheet'));
                navigate('/app/timesheet', { replace: true });
            } finally {
                setLoading(false);
            }
        };

        void loadData();
    }, [id, loadClienti, loadRighe, navigate, numericTimesheetId, resetRigaForm, role]);

    const handleChange = (
        event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
    ) => {
        const { name, value } = event.target;

        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const handleRigaChange = (
        event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
    ) => {
        const { name, value } = event.target;

        const selectedClienteName = name === 'clienteId'
            ? clienti.find((cliente) => String(cliente.id) === value)?.nome?.toUpperCase()
            : null;

        setRigaForm((prev) => ({
            ...prev,
            [name]: value,
            ...(selectedClienteName === NON_LAVORATO_CLIENT_NAME
                ? { ore: '0', minuti: '0' }
                : {}),
        }));
        setRigaFormErrors((prev) => ({
            ...prev,
            [name]: undefined,
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

    const validateRigaForm = (): { formError: string | null; fieldErrors: RigaFormErrors } => {
        const fieldErrors: RigaFormErrors = {};

        if (!timesheetInfo) {
            return {
                formError: 'Informazioni del timesheet non disponibili',
                fieldErrors,
            };
        }

        if (!rigaForm.clienteId) {
            fieldErrors.clienteId = 'Seleziona un cliente';
        }
        if (!rigaForm.data) {
            fieldErrors.data = editingRigaId !== null ? 'Seleziona una data' : 'Seleziona una data iniziale';
        }
        if (!editingRigaId && !rigaForm.dataFine) {
            fieldErrors.dataFine = 'Seleziona una data finale';
        }
        if (rigaForm.ore === '') {
            fieldErrors.ore = 'Inserisci le ore';
        }
        if (rigaForm.minuti === '') {
            fieldErrors.minuti = 'Inserisci i minuti';
        }

        if (Object.keys(fieldErrors).length > 0) {
            return {
                formError: 'Controlla i campi evidenziati della riga',
                fieldErrors,
            };
        }

        const ore = Number(rigaForm.ore);
        const minuti = Number(rigaForm.minuti);

        if (Number.isNaN(ore) || !Number.isInteger(ore) || ore < 0) {
            fieldErrors.ore = 'Le ore devono essere un numero intero maggiore o uguale a 0';
        }

        if (Number.isNaN(minuti) || !Number.isInteger(minuti) || minuti < 0 || minuti > 59) {
            fieldErrors.minuti = 'I minuti devono essere compresi tra 0 e 59';
        }

        if (ore === 0 && minuti === 0 && !isNonLavoratoSelected) {
            fieldErrors.ore = 'Inserisci una durata maggiore di zero';
        }

        if ((ore > 0 || minuti > 0) && isNonLavoratoSelected) {
            fieldErrors.ore = 'Il cliente NON LAVORATO deve avere 0 ore e 0 minuti';
        }

        const endDate = editingRigaId !== null ? rigaForm.data : rigaForm.dataFine;

        if (!editingRigaId && rigaForm.data && endDate && endDate < rigaForm.data) {
            fieldErrors.dataFine = 'La data finale deve essere uguale o successiva alla data iniziale';
        }

        const intervalDates = getDatesInRange(rigaForm.data, endDate);

        if (intervalDates.length === 0) {
            fieldErrors.data = 'Intervallo date non valido';
        }

        const datesOutsideMonth = intervalDates.filter((date) => {
            const [year, month] = date.split('-').map(Number);
            return year !== timesheetInfo.anno || month !== timesheetInfo.mese;
        });

        if (datesOutsideMonth.length > 0) {
            fieldErrors.data = `Le date devono appartenere a ${getMonthLabel(timesheetInfo.mese)} ${timesheetInfo.anno}`;
            if (!editingRigaId) {
                fieldErrors.dataFine = `Le date devono appartenere a ${getMonthLabel(timesheetInfo.mese)} ${timesheetInfo.anno}`;
            }
        }

        if (Object.keys(fieldErrors).length > 0) {
            return {
                formError: 'Controlla i campi evidenziati della riga',
                fieldErrors,
            };
        }

        return { formError: null, fieldErrors: {} };
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!id || Number.isNaN(numericTimesheetId)) {
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

            const response = await updateTimesheetApi(numericTimesheetId, {
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

    const handleRigaSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!id || Number.isNaN(numericTimesheetId)) {
            toast.error('ID timesheet non valido');
            return;
        }

        const { formError, fieldErrors } = validateRigaForm();
        setRigaFormErrors(fieldErrors);
        if (formError) {
            toast.error(formError);
            return;
        }

        try {
            setRigheSaving(true);

            const targetDates = editingRigaId !== null
                ? [rigaForm.data]
                : getDatesInRange(rigaForm.data, rigaForm.dataFine);

            if (editingRigaId !== null) {
                const payload = {
                    timesheetId: numericTimesheetId,
                    clienteId: Number(rigaForm.clienteId),
                    data: rigaForm.data,
                    ore: Number(rigaForm.ore),
                    minuti: Number(rigaForm.minuti),
                };
                const response = await updateTimesheetRigaApi(editingRigaId, payload);
                toast.success(response.message || 'Riga timesheet aggiornata con successo');
            } else {
                await Promise.all(
                    targetDates.map((date) =>
                        createTimesheetRigaApi({
                            timesheetId: numericTimesheetId,
                            clienteId: Number(rigaForm.clienteId),
                            data: date,
                            ore: Number(rigaForm.ore),
                            minuti: Number(rigaForm.minuti),
                        })
                    )
                );
                toast.success(
                    targetDates.length > 1
                        ? `Create ${targetDates.length} righe timesheet per il cliente selezionato`
                        : 'Riga timesheet creata con successo'
                );
            }

            if (timesheetInfo) {
                resetRigaForm(timesheetInfo.mese, timesheetInfo.anno);
            }

            await loadRighe(numericTimesheetId);
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante il salvataggio della riga timesheet'));
        } finally {
            setRigheSaving(false);
        }
    };

    const startEditRiga = (riga: TimesheetRigaDto) => {
        setEditingRigaId(riga.id);
        setRigaForm({
            clienteId: String(riga.clienteId),
            data: riga.data,
            dataFine: riga.data,
            ore: String(riga.ore),
            minuti: String(riga.minuti),
        });
        setRigaFormErrors({});
    };

    const cancelEditRiga = () => {
        if (timesheetInfo) {
            resetRigaForm(timesheetInfo.mese, timesheetInfo.anno);
        } else {
            resetRigaForm();
        }
    };

    const openDeleteDialog = (riga: TimesheetRigaDto) => {
        setRigaDaEliminare(riga);
    };

    const closeDeleteDialog = () => {
        if (deletingRigaId !== null) {
            return;
        }

        setRigaDaEliminare(null);
    };

    const confirmDeleteRiga = async () => {
        if (!rigaDaEliminare) {
            return;
        }

        try {
            setDeletingRigaId(rigaDaEliminare.id);

            const response = await deleteTimesheetRigaApi(rigaDaEliminare.id);
            toast.success(response.message || 'Riga timesheet eliminata con successo');

            if (editingRigaId === rigaDaEliminare.id) {
                cancelEditRiga();
            }

            setRigaDaEliminare(null);
            await loadRighe(numericTimesheetId);
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’eliminazione della riga timesheet'));
        } finally {
            setDeletingRigaId(null);
        }
    };

    if (loading) {
        return (
            <div>
                <PageHeader title={pageTitle} subtitle="Caricamento dati timesheet in corso" />
                <div className="module-placeholder">
                    <h2>Caricamento in corso</h2>
                    <p>Sto recuperando i dati del timesheet selezionato.</p>
                </div>
            </div>
        );
    }

    return (
        <div>
            <PageHeader title={pageTitle}
                actions={<BackButton fallbackPath="/app/timesheet" />} />

            {isAdmin ? (
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
            ) : null}

            <div className="form-card" style={{ marginTop: '1.5rem' }}>
                {timesheetInfo ? (
                    <div className="timesheet-coverage-card">
                        <div>
                            <h2 style={{ margin: 0 }}>Copertura del mese</h2>
                            <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
                                Per confermare il timesheet, tutti i giorni non festivi di {getMonthLabel(timesheetInfo.mese)} {timesheetInfo.anno} devono avere almeno una riga.
                            </p>
                            <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
                                Se non hai lavorato in un giorno feriale, inserisci una riga con cliente <strong>{NON_LAVORATO_CLIENT_NAME}</strong> e valori <strong>0h 0m</strong>.
                            </p>
                            <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
                                I giorni festivi restano facoltativi: sono evidenziati in tabella ma non bloccano la conferma.
                            </p>
                        </div>

                        <div className={missingRequiredDates.length === 0 ? 'coverage-ok' : 'coverage-missing'}>
                            {missingRequiredDates.length === 0
                                ? 'Copertura completa: il mese è pronto per la conferma.'
                                : `Giorni non festivi ancora da compilare: ${missingRequiredDates.length}`}
                        </div>

                        {missingRequiredDates.length > 0 ? (
                            <div className="coverage-date-list">
                                {missingRequiredDates.map((date) => (
                                    <span key={date} className="coverage-date-pill">
                                        {formatDate(date)}
                                    </span>
                                ))}
                            </div>
                        ) : null}
                    </div>
                ) : null}

                <div
                    style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        gap: '1rem',
                        marginBottom: '1rem',
                        flexWrap: 'wrap',
                    }}
                >
                    <div>
                        <h2 style={{ margin: 0 }}>Righe timesheet</h2>
                        <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
                            Totale righe: {righe.length}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="secondary-button"
                        onClick={() => void loadRighe(numericTimesheetId)}
                        disabled={righeLoading || righeSaving || deletingRigaId !== null}
                    >
                        {righeLoading ? 'Aggiornamento...' : 'Aggiorna righe'}
                    </button>
                </div>

                {isReadOnly ? (
                    <div style={{ marginBottom: '1rem' }}>
                        <p className="page-subtitle">
                            Questo timesheet è in sola lettura nello stato attuale.
                        </p>
                    </div>
                ) : null}

                <form onSubmit={handleRigaSubmit} className="entity-form" style={{ marginBottom: '1.5rem' }}>
                    <div className="form-grid">
                        <div className="form-group">
                            <label htmlFor="clienteId">Cliente</label>
                            <select
                                id="clienteId"
                                name="clienteId"
                                value={rigaForm.clienteId}
                                onChange={handleRigaChange}
                                disabled={isReadOnly || righeSaving || clientiLoading}
                                className={`form-select ${rigaFormErrors.clienteId ? 'input-error' : ''}`}
                            >
                                <option value="">Seleziona cliente</option>
                                {clienti.map((cliente) => (
                                    <option key={cliente.id} value={cliente.id}>
                                        {cliente.nome}
                                    </option>
                                ))}
                            </select>
                            {rigaFormErrors.clienteId ? (
                                <small className="field-error">{rigaFormErrors.clienteId}</small>
                            ) : null}
                            {isNonLavoratoSelected ? (
                                <small className="field-hint">
                                    Per il cliente {NON_LAVORATO_CLIENT_NAME} la riga deve restare a 0 ore e 0 minuti.
                                </small>
                            ) : null}
                        </div>

                        <div className="form-group">
                            <label htmlFor="data">Data</label>
                            <input
                                id="data"
                                name="data"
                                type="date"
                                value={rigaForm.data}
                                onChange={handleRigaChange}
                                disabled={isReadOnly || righeSaving}
                                className={rigaFormErrors.data ? 'input-error' : ''}
                            />
                            {rigaFormErrors.data ? (
                                <small className="field-error">{rigaFormErrors.data}</small>
                            ) : null}
                        </div>

                        {editingRigaId === null ? (
                            <div className="form-group">
                                <label htmlFor="dataFine">Data fine</label>
                                <input
                                    id="dataFine"
                                    name="dataFine"
                                    type="date"
                                    value={rigaForm.dataFine}
                                    onChange={handleRigaChange}
                                    disabled={isReadOnly || righeSaving}
                                    className={rigaFormErrors.dataFine ? 'input-error' : ''}
                                />
                                {rigaFormErrors.dataFine ? (
                                    <small className="field-error">{rigaFormErrors.dataFine}</small>
                                ) : (
                                    <small className="field-hint">
                                        Inserisci un intervallo per creare una riga per ogni giorno con lo stesso cliente.
                                    </small>
                                )}
                            </div>
                        ) : null}

                        <div className="form-group">
                            <label htmlFor="ore">Ore</label>
                            <input
                                id="ore"
                                name="ore"
                                type="number"
                                min={0}
                                step={1}
                                value={rigaForm.ore}
                                onChange={handleRigaChange}
                                disabled={isReadOnly || righeSaving}
                                className={rigaFormErrors.ore ? 'input-error' : ''}
                                placeholder="Es. 8"
                            />
                            {rigaFormErrors.ore ? (
                                <small className="field-error">{rigaFormErrors.ore}</small>
                            ) : null}
                        </div>

                        <div className="form-group">
                            <label htmlFor="minuti">Minuti</label>
                            <input
                                id="minuti"
                                name="minuti"
                                type="number"
                                min={0}
                                max={59}
                                step={1}
                                value={rigaForm.minuti}
                                onChange={handleRigaChange}
                                disabled={isReadOnly || righeSaving}
                                className={rigaFormErrors.minuti ? 'input-error' : ''}
                                placeholder="Es. 30"
                            />
                            {rigaFormErrors.minuti ? (
                                <small className="field-error">{rigaFormErrors.minuti}</small>
                            ) : null}
                        </div>
                    </div>

                    <div className="form-actions">
                        {editingRigaId !== null ? (
                            <button
                                type="button"
                                className="secondary-button"
                                onClick={cancelEditRiga}
                                disabled={righeSaving}
                            >
                                Annulla modifica
                            </button>
                        ) : null}

                        <button
                            type="submit"
                            className="primary-button form-submit-button"
                            disabled={isReadOnly || righeSaving || clientiLoading}
                        >
                            {righeSaving
                                ? 'Salvataggio...'
                                : editingRigaId !== null
                                    ? 'Salva riga'
                                    : 'Aggiungi riga'}
                        </button>
                    </div>
                </form>

                {righeLoading ? (
                    <div className="module-placeholder">
                        <h2>Caricamento righe in corso</h2>
                        <p>Sto recuperando le righe associate al timesheet selezionato.</p>
                    </div>
                ) : righe.length === 0 ? (
                    <div className="module-placeholder">
                        <h2>Nessuna riga presente</h2>
                        <p>Non ci sono ancora righe associate a questo timesheet.</p>
                    </div>
                ) : (
                    <div className="table-wrapper">
                        <table className="app-table">
                            <thead>
                                <tr>
                                    <th>Data</th>
                                    <th>Cliente</th>
                                    <th>Ore</th>
                                    <th>Minuti</th>
                                    <th>Orario</th>
                                    <th>Costo</th>
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                {righe.map((riga) => {
                                    const isFestivo = isFestivoItaliano(riga.data);

                                    return (
                                        <tr key={riga.id} className={isFestivo ? 'festivo-row' : ''}>
                                            <td className={isFestivo ? 'festivo-date-cell' : ''}>
                                                {formatDate(riga.data)}
                                            </td>
                                            <td>{riga.clienteNome}</td>
                                            <td>{riga.ore}</td>
                                            <td>{riga.minuti}</td>
                                            <td><span className="riga-metrica">{riga.orario.toFixed(2)} ore</span></td>
                                            <td><span className="riga-metrica">{formatCurrency(riga.costoOrario)}</span></td>
                                            <td>
                                                <div className="table-actions">
                                                    <button
                                                        type="button"
                                                        className="table-action-button edit"
                                                        onClick={() => startEditRiga(riga)}
                                                        disabled={isReadOnly || righeSaving || deletingRigaId !== null}
                                                    >
                                                        Modifica
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="table-action-button delete"
                                                        onClick={() => openDeleteDialog(riga)}
                                                        disabled={isReadOnly || righeSaving || deletingRigaId === riga.id}
                                                    >
                                                        {deletingRigaId === riga.id ? 'Eliminazione...' : 'Elimina'}
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            <ConfirmDialog
                open={!!rigaDaEliminare}
                title="Conferma eliminazione"
                message={
                    rigaDaEliminare
                        ? `Vuoi eliminare la riga del ${formatDate(rigaDaEliminare.data)} per il cliente ${rigaDaEliminare.clienteNome}?`
                        : ''
                }
                confirmText="Elimina riga"
                cancelText="Annulla"
                loading={deletingRigaId !== null}
                onConfirm={confirmDeleteRiga}
                onCancel={closeDeleteDialog}
            />
        </div>
    );
}

export default ModificaTimesheetPage;
