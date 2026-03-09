import axios from 'axios';
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { getUtentiApi } from '../../api/utentiApi';
import {
    chiudiTimesheetApi,
    confermaTimesheetApi,
    deleteTimesheetApi,
    exportTimesheetPdfApi,
    getAnniTimesheetApi,
    getTimesheetApi,
    getTotaliTimesheetApi,
    riapriTimesheetApi,
    searchTimesheetApi,
} from '../../api/timesheetApi';
import type { TimesheetDto, TotaleClienteDto, TotaliDto } from '../../types/timesheet.ts';
import { getCurrentUserRole } from '../../utils/auth';
import { getErrorMessage } from '../../utils/error';

type ConfirmAction = 'delete' | 'conferma' | 'riapri' | 'chiudi';

type ConfirmState = {
    action: ConfirmAction;
    timesheet: TimesheetDto;
} | null;

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

function getMonthLabel(mese: number): string {
    return MONTH_OPTIONS.find((item) => item.value === mese)?.label ?? '';
}

function formatCompilazione(value: string | null): string {
    if (!value) {
        return '—';
    }

    try {
        return new Intl.DateTimeFormat('it-IT').format(new Date(value));
    } catch {
        return value;
    }
}

function TimesheetPage() {
    const role = getCurrentUserRole();

    const [timesheets, setTimesheets] = useState<TimesheetDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

    const [anniDisponibili, setAnniDisponibili] = useState<number[]>([]);
    const [userOptions, setUserOptions] = useState<UserOption[]>([]);

    const [filters, setFilters] = useState({
        mese: '',
        anno: '',
        utenteId: '',
    });

    const [confirmState, setConfirmState] = useState<ConfirmState>(null);

    const [totalsLoading, setTotalsLoading] = useState(false);
    const [selectedTimesheetForTotals, setSelectedTimesheetForTotals] = useState<TimesheetDto | null>(null);
    const [totali, setTotali] = useState<TotaliDto | null>(null);
    const [totaliPerCliente, setTotaliPerCliente] = useState<TotaleClienteDto[]>([]);

    const loadSupportData = useCallback(async () => {
        const anniResponse = await getAnniTimesheetApi();
        setAnniDisponibili(anniResponse.data ?? []);

        if (role === 'ADMIN') {
            const utentiResponse = await getUtentiApi();
            const utenti = utentiResponse.data ?? [];

            setUserOptions(
                utenti.map((utente) => ({
                    id: utente.id,
                    label: `${utente.nome} ${utente.cognome}`.trim(),
                }))
            );
        }
    }, [role]);

    const loadTimesheets = useCallback(async () => {
        try {
            setLoading(true);
            const response = await getTimesheetApi();
            setTimesheets(response.data ?? []);
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante il caricamento dei timesheet'));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        const bootstrap = async () => {
            try {
                await loadSupportData();
                await loadTimesheets();
            } catch (error: unknown) {
                toast.error(getErrorMessage(error, 'Errore durante l’inizializzazione del modulo timesheet'));
                setLoading(false);
            }
        };

        void bootstrap();
    }, [loadSupportData, loadTimesheets]);

    const handleFilterChange = (
        event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
    ) => {
        const { name, value } = event.target;

        setFilters((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const clearTotals = () => {
        setSelectedTimesheetForTotals(null);
        setTotali(null);
        setTotaliPerCliente([]);
    };

    const handleSearch = async () => {
        try {
            setLoading(true);
            clearTotals();

            const params: { mese?: number; anno?: number; utenteId?: number } = {};

            if (filters.mese) {
                params.mese = Number(filters.mese);
            }

            if (filters.anno) {
                params.anno = Number(filters.anno);
            }

            if (role === 'ADMIN' && filters.utenteId) {
                params.utenteId = Number(filters.utenteId);
            }

            if (!params.mese && !params.anno && !params.utenteId) {
                await loadTimesheets();
                return;
            }

            const response = await searchTimesheetApi(params);
            setTimesheets(response.data ?? []);
        } catch (error: unknown) {
            if (axios.isAxiosError(error) && error.response?.status === 404) {
                setTimesheets([]);
                toast.info('Nessun timesheet trovato con i filtri selezionati');
            } else {
                toast.error(getErrorMessage(error, 'Errore durante la ricerca dei timesheet'));
            }
        } finally {
            setLoading(false);
        }
    };

    const handleReset = async () => {
        setFilters({
            mese: '',
            anno: '',
            utenteId: '',
        });
        clearTotals();
        await loadTimesheets();
    };

    const loadTotals = async (timesheet: TimesheetDto) => {
        try {
            setTotalsLoading(true);

            const [totaliResponse, perClienteResponse] = await Promise.all([
                getTotaliTimesheetApi(timesheet.id, false),
                getTotaliTimesheetApi(timesheet.id, true),
            ]);

            const totaliData = totaliResponse.data;
            const perClienteData = perClienteResponse.data;

            setSelectedTimesheetForTotals(timesheet);
            setTotali(Array.isArray(totaliData) ? null : (totaliData ?? null));
            setTotaliPerCliente(Array.isArray(perClienteData) ? perClienteData : []);
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante il caricamento dei totali'));
        } finally {
            setTotalsLoading(false);
        }
    };

    const handleExport = async (timesheet: TimesheetDto) => {
        try {
            setActionLoadingId(timesheet.id);

            const { blob, filename } = await exportTimesheetPdfApi(timesheet.id);
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');

            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            window.setTimeout(() => window.URL.revokeObjectURL(url), 500);
            toast.success('Export PDF completato');
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’export del PDF'));
        } finally {
            setActionLoadingId(null);
        }
    };

    const openConfirmDialog = (action: ConfirmAction, timesheet: TimesheetDto) => {
        setConfirmState({ action, timesheet });
    };

    const closeConfirmDialog = () => {
        if (actionLoadingId !== null) {
            return;
        }

        setConfirmState(null);
    };

    const handleConfirmAction = async () => {
        if (!confirmState) {
            return;
        }

        const { action, timesheet } = confirmState;

        try {
            setActionLoadingId(timesheet.id);

            if (action === 'delete') {
                const response = await deleteTimesheetApi(timesheet.id);
                toast.success(response.message || 'Timesheet eliminato con successo');
            }

            if (action === 'conferma') {
                const response = await confermaTimesheetApi(timesheet.id);
                toast.success(response.message || 'Timesheet confermato con successo');
            }

            if (action === 'riapri') {
                const response = await riapriTimesheetApi(timesheet.id);
                toast.success(response.message || 'Timesheet riaperto con successo');
            }

            if (action === 'chiudi') {
                const response = await chiudiTimesheetApi(timesheet.id);
                toast.success(response.message || 'Timesheet chiuso con successo');
            }

            clearTotals();
            setConfirmState(null);
            await loadTimesheets();
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’operazione sul timesheet'));
        } finally {
            setActionLoadingId(null);
        }
    };

    const canEdit = (timesheet: TimesheetDto): boolean => {
        if (timesheet.stato === 'CHIUSO') {
            return false;
        }

        if (timesheet.stato === 'CONFERMATO' && role !== 'ADMIN') {
            return false;
        }

        return true;
    };

    const canDelete = (): boolean => role === 'ADMIN';
    const canConferma = (timesheet: TimesheetDto): boolean => timesheet.stato === 'APERTO';
    const canRiapri = (timesheet: TimesheetDto): boolean =>
        timesheet.stato === 'CONFERMATO' || (timesheet.stato === 'CHIUSO' && role === 'ADMIN');
    const canChiudi = (timesheet: TimesheetDto): boolean => timesheet.stato === 'CONFERMATO';
    const canExport = (timesheet: TimesheetDto): boolean => timesheet.stato === 'CHIUSO';

    const subtitle =
        role === 'ADMIN'
            ? `Totale timesheet: ${timesheets.length}`
            : `Totale miei timesheet: ${timesheets.length}`;

    const dialogTitleMap: Record<ConfirmAction, string> = {
        delete: 'Conferma eliminazione',
        conferma: 'Conferma timesheet',
        riapri: 'Riapri timesheet',
        chiudi: 'Chiudi timesheet',
    };

    const dialogMessageMap: Record<ConfirmAction, (timesheet: TimesheetDto) => string> = {
        delete: (timesheet) =>
            `Sei sicuro di voler eliminare il timesheet di ${getMonthLabel(timesheet.mese)} ${timesheet.anno}?`,
        conferma: (timesheet) =>
            `Vuoi confermare il timesheet di ${getMonthLabel(timesheet.mese)} ${timesheet.anno}?`,
        riapri: (timesheet) =>
            `Vuoi riaprire il timesheet di ${getMonthLabel(timesheet.mese)} ${timesheet.anno}?`,
        chiudi: (timesheet) =>
            `Vuoi chiudere il timesheet di ${getMonthLabel(timesheet.mese)} ${timesheet.anno}?`,
    };

    const dialogConfirmTextMap: Record<ConfirmAction, string> = {
        delete: 'Elimina timesheet',
        conferma: 'Conferma timesheet',
        riapri: 'Riapri timesheet',
        chiudi: 'Chiudi timesheet',
    };

    return (
        <div>
            <PageHeader
                title="Timesheet"
                subtitle={subtitle}
                actions={
                    <div className="page-actions">
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={loadTimesheets}
                            disabled={loading}
                        >
                            {loading ? 'Aggiornamento...' : 'Aggiorna'}
                        </button>

                        <Link to="/app/timesheet/nuovo" className="action-link-button">
                            Nuovo timesheet
                        </Link>
                    </div>
                }
            />

            <div className="form-card" style={{ marginBottom: '1.5rem' }}>
                <div className="form-grid">
                    <div className="form-group">
                        <label htmlFor="mese">Mese</label>
                        <select
                            id="mese"
                            name="mese"
                            value={filters.mese}
                            onChange={handleFilterChange}
                            className="form-select"
                            disabled={loading}
                        >
                            <option value="">Seleziona mese</option>
                            {MONTH_OPTIONS.map((month) => (
                                <option key={month.value} value={month.value}>
                                    {month.label}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label htmlFor="anno">Anno</label>
                        <select
                            id="anno"
                            name="anno"
                            value={filters.anno}
                            onChange={handleFilterChange}
                            className="form-select"
                            disabled={loading}
                        >
                            <option value="">Seleziona anno</option>
                            {anniDisponibili.map((anno) => (
                                <option key={anno} value={anno}>
                                    {anno}
                                </option>
                            ))}
                        </select>
                    </div>

                    {role === 'ADMIN' ? (
                        <div className="form-group">
                            <label htmlFor="utenteId">Utente</label>
                            <select
                                id="utenteId"
                                name="utenteId"
                                value={filters.utenteId}
                                onChange={handleFilterChange}
                                className="form-select"
                                disabled={loading}
                            >
                                <option value="">Tutti gli utenti</option>
                                {userOptions.map((user) => (
                                    <option key={user.id} value={user.id}>
                                        {user.label}
                                    </option>
                                ))}
                            </select>
                        </div>
                    ) : null}
                </div>

                <div className="form-actions">
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={handleReset}
                        disabled={loading}
                    >
                        Reset
                    </button>

                    <button
                        type="button"
                        className="primary-button form-submit-button"
                        onClick={handleSearch}
                        disabled={loading}
                    >
                        {loading ? 'Ricerca...' : 'Cerca'}
                    </button>
                </div>
            </div>

            {loading ? (
                <div className="module-placeholder">
                    <h2>Caricamento in corso</h2>
                    <p>Sto recuperando i timesheet dal backend.</p>
                </div>
            ) : timesheets.length === 0 ? (
                <div className="module-placeholder">
                    <h2>Nessun timesheet trovato</h2>
                    <p>Non ci sono timesheet da mostrare con i filtri attuali.</p>
                </div>
            ) : (
                <div className="table-card">
                    <div className="table-wrapper">
                        <table className="app-table">
                            <thead>
                                <tr>
                                    <th>Periodo</th>
                                    {role === 'ADMIN' ? <th>Utente</th> : null}
                                    <th>Stato</th>
                                    <th>Data compilazione</th>
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                {timesheets.map((timesheet) => (
                                    <tr key={timesheet.id}>
                                        <td>
                                            {getMonthLabel(timesheet.mese)} {timesheet.anno}
                                        </td>

                                        {role === 'ADMIN' ? <td>{timesheet.utenteNomeCompleto}</td> : null}

                                        <td>
                                            <span className="role-badge">{timesheet.stato}</span>
                                        </td>

                                        <td>{formatCompilazione(timesheet.dataCompilazione)}</td>

                                        <td>
                                            <div className="table-actions">
                                                {canEdit(timesheet) ? (
                                                    <Link
                                                        to={`/app/timesheet/${timesheet.id}/modifica`}
                                                        className="table-action-button edit"
                                                    >
                                                        Modifica
                                                    </Link>
                                                ) : null}

                                                {canConferma(timesheet) ? (
                                                    <button
                                                        type="button"
                                                        className="table-action-button edit"
                                                        onClick={() => openConfirmDialog('conferma', timesheet)}
                                                        disabled={actionLoadingId === timesheet.id}
                                                    >
                                                        Conferma
                                                    </button>
                                                ) : null}

                                                {canRiapri(timesheet) ? (
                                                    <button
                                                        type="button"
                                                        className="table-action-button edit"
                                                        onClick={() => openConfirmDialog('riapri', timesheet)}
                                                        disabled={actionLoadingId === timesheet.id}
                                                    >
                                                        Riapri
                                                    </button>
                                                ) : null}

                                                {canChiudi(timesheet) ? (
                                                    <button
                                                        type="button"
                                                        className="table-action-button edit"
                                                        onClick={() => openConfirmDialog('chiudi', timesheet)}
                                                        disabled={actionLoadingId === timesheet.id}
                                                    >
                                                        Chiudi
                                                    </button>
                                                ) : null}

                                                <button
                                                    type="button"
                                                    className="table-action-button edit"
                                                    onClick={() => loadTotals(timesheet)}
                                                    disabled={totalsLoading}
                                                >
                                                    Totali
                                                </button>

                                                {canExport(timesheet) ? (
                                                    <button
                                                        type="button"
                                                        className="table-action-button edit"
                                                        onClick={() => handleExport(timesheet)}
                                                        disabled={actionLoadingId === timesheet.id}
                                                    >
                                                        Export PDF
                                                    </button>
                                                ) : null}

                                                {canDelete() ? (
                                                    <button
                                                        type="button"
                                                        className="table-action-button delete"
                                                        onClick={() => openConfirmDialog('delete', timesheet)}
                                                        disabled={actionLoadingId === timesheet.id}
                                                    >
                                                        Elimina
                                                    </button>
                                                ) : null}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {selectedTimesheetForTotals ? (
                <div className="table-card" style={{ marginTop: '1.5rem' }}>
                    <PageHeader
                        title={`Totali — ${getMonthLabel(selectedTimesheetForTotals.mese)} ${selectedTimesheetForTotals.anno}`}
                        subtitle={
                            role === 'ADMIN'
                                ? selectedTimesheetForTotals.utenteNomeCompleto
                                : 'Dettaglio riepilogativo del tuo timesheet'
                        }
                        actions={
                            <button
                                type="button"
                                className="secondary-button"
                                onClick={clearTotals}
                                disabled={totalsLoading}
                            >
                                Chiudi dettaglio
                            </button>
                        }
                    />

                    {totalsLoading ? (
                        <div className="module-placeholder">
                            <h2>Caricamento totali in corso</h2>
                            <p>Sto recuperando il riepilogo del timesheet selezionato.</p>
                        </div>
                    ) : (
                        <>
                            <div className="form-card" style={{ marginBottom: '1.5rem' }}>
                                <div className="form-grid">
                                    <div className="form-group">
                                        <label>Totale orario</label>
                                        <input
                                            type="text"
                                            value={totali ? `${totali.totaleOrario.toFixed(2)} ore` : '0.00 ore'}
                                            disabled
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label>Totale costo</label>
                                        <input
                                            type="text"
                                            value={totali ? `€ ${totali.totaleCosto.toFixed(2)}` : '€ 0.00'}
                                            disabled
                                        />
                                    </div>
                                </div>
                            </div>

                            {totaliPerCliente.length === 0 ? (
                                <div className="module-placeholder">
                                    <h2>Nessun totale per cliente</h2>
                                    <p>Non ci sono ancora righe sufficienti per generare il riepilogo per cliente.</p>
                                </div>
                            ) : (
                                <div className="table-wrapper">
                                    <table className="app-table">
                                        <thead>
                                            <tr>
                                                <th>Cliente</th>
                                                <th>Totale orario</th>
                                                <th>Totale costo</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {totaliPerCliente.map((item) => (
                                                <tr key={item.clienteId}>
                                                    <td>{item.clienteNome}</td>
                                                    <td>{item.orario.toFixed(2)} ore</td>
                                                    <td>€ {item.costo.toFixed(2)}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </>
                    )}
                </div>
            ) : null}

            <ConfirmDialog
                open={!!confirmState}
                title={confirmState ? dialogTitleMap[confirmState.action] : ''}
                message={confirmState ? dialogMessageMap[confirmState.action](confirmState.timesheet) : ''}
                confirmText={confirmState ? dialogConfirmTextMap[confirmState.action] : 'Conferma'}
                cancelText="Annulla"
                loading={actionLoadingId !== null}
                onConfirm={handleConfirmAction}
                onCancel={closeConfirmDialog}
            />
        </div>
    );
}

export default TimesheetPage;