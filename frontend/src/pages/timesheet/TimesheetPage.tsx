import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import PageHeader from '../../components/common/PageHeader';
import {
    chiudiTimesheetApi,
    confermaTimesheetApi,
    deleteTimesheetApi,
    exportTimesheetPdfApi,
    getAnniTimesheetApi,
    getTimesheetApi,
    getTotaliTimesheetApi,
    riapriTimesheetApi,
} from '../../api/timesheetApi';
import { getUtentiApi } from '../../api/utentiApi';
import type {
    TimesheetDto,
    TimesheetListMeta,
    TimesheetStato,
    TotaleClienteDto,
    TotaliDto,
} from '../../types/timesheet.ts';
import { getCurrentUserRole } from '../../utils/auth';
import { excludeAdminUsers, sortUsersByDisplayName } from '../../utils/entityFilters';
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

type TimesheetFilters = {
    mese: string;
    anno: string;
    utenteId: string;
    stato: '' | TimesheetStato;
};

const PAGE_SIZE = 10;

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

const STATO_OPTIONS: Array<{ value: '' | TimesheetStato; label: string }> = [
    { value: '', label: 'Tutti gli stati' },
    { value: 'APERTO', label: 'Aperto' },
    { value: 'CONFERMATO', label: 'Confermato' },
    { value: 'CHIUSO', label: 'Chiuso' },
];

function getMonthLabel(mese: number): string {
    return MONTH_OPTIONS.find((item) => item.value === mese)?.label ?? '';
}

function formatCompilazione(value: string | null): string {
    if (!value) {
        return '-';
    }

    try {
        return new Intl.DateTimeFormat('it-IT').format(new Date(value));
    } catch {
        return value;
    }
}

function getDefaultFilters(): TimesheetFilters {
    const now = new Date();
    return {
        mese: String(now.getMonth() + 1),
        anno: String(now.getFullYear()),
        utenteId: '',
        stato: '',
    };
}

function getEmptyFilters(): TimesheetFilters {
    return {
        mese: '',
        anno: '',
        utenteId: '',
        stato: '',
    };
}

function createDefaultMeta(): TimesheetListMeta {
    return {
        page: 0,
        size: PAGE_SIZE,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
    };
}

function TimesheetPage() {
    const role = getCurrentUserRole();

    const [timesheets, setTimesheets] = useState<TimesheetDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

    const [anniDisponibili, setAnniDisponibili] = useState<number[]>([]);
    const [userOptions, setUserOptions] = useState<UserOption[]>([]);
    const [filters, setFilters] = useState<TimesheetFilters>(() => getDefaultFilters());
    const [paginationMeta, setPaginationMeta] = useState<TimesheetListMeta>(() => createDefaultMeta());

    const [confirmState, setConfirmState] = useState<ConfirmState>(null);

    const [totalsLoading, setTotalsLoading] = useState(false);
    const [selectedTimesheetForTotals, setSelectedTimesheetForTotals] = useState<TimesheetDto | null>(null);
    const [totali, setTotali] = useState<TotaliDto | null>(null);
    const [totaliPerCliente, setTotaliPerCliente] = useState<TotaleClienteDto[]>([]);

    const clearTotals = () => {
        setSelectedTimesheetForTotals(null);
        setTotali(null);
        setTotaliPerCliente([]);
    };

    const loadSupportData = useCallback(async () => {
        const anniResponse = await getAnniTimesheetApi();
        setAnniDisponibili(anniResponse.data ?? []);

        if (role === 'ADMIN') {
            const utentiResponse = await getUtentiApi();
            const utenti = sortUsersByDisplayName(excludeAdminUsers(utentiResponse.data ?? []));

            setUserOptions(
                utenti.map((utente) => ({
                    id: utente.id,
                    label: `${utente.nome} ${utente.cognome}`.trim(),
                }))
            );
        }
    }, [role]);

    const loadTimesheets = useCallback(async (activeFilters: TimesheetFilters, page = 0) => {
        try {
            setLoading(true);

            const params: {
                mese?: number;
                anno?: number;
                utenteId?: number;
                stato?: string;
                page: number;
                size: number;
            } = {
                page,
                size: PAGE_SIZE,
            };

            if (activeFilters.mese) {
                params.mese = Number(activeFilters.mese);
            }

            if (activeFilters.anno) {
                params.anno = Number(activeFilters.anno);
            }

            if (role === 'ADMIN' && activeFilters.utenteId) {
                params.utenteId = Number(activeFilters.utenteId);
            }

            if (activeFilters.stato) {
                params.stato = activeFilters.stato;
            }

            const response = await getTimesheetApi(params);
            const data = response.data ?? [];
            const meta = response.meta ?? {
                page,
                size: PAGE_SIZE,
                totalElements: data.length,
                totalPages: data.length === 0 ? 0 : 1,
                hasNext: false,
                hasPrevious: page > 0,
            };

            if (data.length === 0 && meta.totalPages > 0 && page >= meta.totalPages) {
                await loadTimesheets(activeFilters, meta.totalPages - 1);
                return;
            }

            setTimesheets(data);
            setPaginationMeta(meta);
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante il caricamento dei timesheet'));
        } finally {
            setLoading(false);
        }
    }, [role]);

    useEffect(() => {
        const bootstrap = async () => {
            try {
                const defaultFilters = getDefaultFilters();
                await loadSupportData();
                await loadTimesheets(defaultFilters, 0);
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

    const handleSearch = async () => {
        clearTotals();
        await loadTimesheets(filters, 0);
    };

    const handleReset = async () => {
        const defaultFilters = getDefaultFilters();
        setFilters(defaultFilters);
        clearTotals();
        await loadTimesheets(defaultFilters, 0);
    };

    const handleShowAll = async () => {
        const emptyFilters = getEmptyFilters();
        setFilters(emptyFilters);
        clearTotals();
        await loadTimesheets(emptyFilters, 0);
    };

    const handleRefresh = async () => {
        clearTotals();
        await loadTimesheets(filters, paginationMeta.page);
    };

    const handlePreviousPage = async () => {
        if (!paginationMeta.hasPrevious || loading) {
            return;
        }

        clearTotals();
        await loadTimesheets(filters, paginationMeta.page - 1);
    };

    const handleNextPage = async () => {
        if (!paginationMeta.hasNext || loading) {
            return;
        }

        clearTotals();
        await loadTimesheets(filters, paginationMeta.page + 1);
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
            await loadTimesheets(filters, paginationMeta.page);
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’operazione sul timesheet'));
        } finally {
            setActionLoadingId(null);
        }
    };

    const canOpenDetail = (_timesheet: TimesheetDto): boolean => true;

    const canDelete = (): boolean => role === 'ADMIN';
    const canConferma = (timesheet: TimesheetDto): boolean => timesheet.stato === 'APERTO';
    const canRiapri = (timesheet: TimesheetDto): boolean =>
        timesheet.stato === 'CONFERMATO' || (timesheet.stato === 'CHIUSO' && role === 'ADMIN');
    const canChiudi = (timesheet: TimesheetDto): boolean => timesheet.stato === 'CONFERMATO';
    const canExport = (timesheet: TimesheetDto): boolean =>
        role === 'ADMIN' && timesheet.stato === 'CHIUSO';

    const subtitle =
        role === 'ADMIN'
            ? `Totale timesheet: ${paginationMeta.totalElements}`
            : `Totale miei timesheet: ${paginationMeta.totalElements}`;

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
                            onClick={handleRefresh}
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

                    <div className="form-group">
                        <label htmlFor="stato">Stato</label>
                        <select
                            id="stato"
                            name="stato"
                            value={filters.stato}
                            onChange={handleFilterChange}
                            className="form-select"
                            disabled={loading}
                        >
                            {STATO_OPTIONS.map((option) => (
                                <option key={option.label} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                <div className="form-actions">
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={handleShowAll}
                        disabled={loading}
                    >
                        Mostra tutti
                    </button>

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
                                        <td data-label="Periodo">
                                            {getMonthLabel(timesheet.mese)} {timesheet.anno}
                                        </td>

                                        {role === 'ADMIN' ? (
                                            <td data-label="Utente">{timesheet.utenteNomeCompleto}</td>
                                        ) : null}

                                        <td data-label="Stato">
                                            <span className={`stato-badge ${timesheet.stato.toLowerCase()}`}>
                                                {timesheet.stato}
                                            </span>
                                        </td>

                                        <td data-label="Data compilazione">
                                            {formatCompilazione(timesheet.dataCompilazione)}
                                        </td>

                                        <td data-label="Azioni">
                                            <div className="table-actions">
                                                {canOpenDetail(timesheet) ? (
                                                    <Link
                                                        to={`/app/timesheet/${timesheet.id}/modifica`}
                                                        className="table-action-button edit"
                                                    >
                                                        {timesheet.stato === 'APERTO' ? 'Modifica' : 'Visualizza'}
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
                                                        className="table-action-button export"
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

                    <div className="table-pagination">
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={handlePreviousPage}
                            disabled={loading || !paginationMeta.hasPrevious}
                        >
                            Precedente
                        </button>

                        <span className="table-pagination-status">
                            Pagina {paginationMeta.totalPages === 0 ? 0 : paginationMeta.page + 1} di {paginationMeta.totalPages}
                        </span>

                        <button
                            type="button"
                            className="secondary-button"
                            onClick={handleNextPage}
                            disabled={loading || !paginationMeta.hasNext}
                        >
                            Successiva
                        </button>
                    </div>
                </div>
            )}

            {selectedTimesheetForTotals ? (
                <div className="table-card" style={{ marginTop: '1.5rem' }}>
                    <PageHeader
                        title={`Totali - ${getMonthLabel(selectedTimesheetForTotals.mese)} ${selectedTimesheetForTotals.anno}`}
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
                                            value={totali ? `EUR ${totali.totaleCosto.toFixed(2)}` : 'EUR 0.00'}
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
                                                    <td data-label="Cliente">{item.clienteNome}</td>
                                                    <td data-label="Totale orario">{item.orario.toFixed(2)} ore</td>
                                                    <td data-label="Totale costo">EUR {item.costo.toFixed(2)}</td>
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
