import { useEffect, useMemo, useState } from 'react';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { getClientiApi } from '../../api/clientiApi';
import {
    exportReportClientePdfApi,
    getReportClienteApi,
    getReportClienteGiornoApi,
    getReportDipendenteApi,
} from '../../api/reportApi';
import { getMyProfileApi, getUtentiApi } from '../../api/utentiApi';
import type { ClienteDto } from '../../types/cliente';
import type {
    ReportClienteDto,
    ReportClienteGiornoDto,
    ReportDipendenteDto,
    ReportMode,
} from '../../types/report';
import type { ProfiloUtenteDto, UtenteDto } from '../../types/utente';
import { getCurrentUserRole } from '../../utils/auth';
import { excludeAdminUsers, excludeNonLavoratoClienti, sortClientiByName, sortUsersByDisplayName } from '../../utils/entityFilters';
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

function formatCurrency(value: number): string {
    return `EUR ${value.toFixed(2)}`;
}

function formatDisplayDate(value: string): string {
    if (!value) {
        return 'Data non selezionata';
    }

    const [year, month, day] = value.split('-');
    if (!year || !month || !day) {
        return value;
    }

    return `${day}/${month}/${year}`;
}

function ReportPage() {
    const role = getCurrentUserRole();
    const isAdmin = role === 'ADMIN';

    const currentYear = new Date().getFullYear();

    const [mode, setMode] = useState<ReportMode>(isAdmin ? 'cliente' : 'dipendente');

    const [filters, setFilters] = useState({
        anno: String(currentYear),
        mese: '',
        clienteId: '',
        utenteId: '',
        data: '',
    });

    const [clienti, setClienti] = useState<ClienteDto[]>([]);
    const [utenti, setUtenti] = useState<UserOption[]>([]);

    const [loadingSupportData, setLoadingSupportData] = useState(true);
    const [loadingReport, setLoadingReport] = useState(false);
    const [exportingPdf, setExportingPdf] = useState(false);

    const [reportCliente, setReportCliente] = useState<ReportClienteDto | null>(null);
    const [reportClienteGiorno, setReportClienteGiorno] = useState<ReportClienteGiornoDto | null>(null);
    const [reportDipendente, setReportDipendente] = useState<ReportDipendenteDto | null>(null);

    const [myProfile, setMyProfile] = useState<ProfiloUtenteDto | null>(null);

    useEffect(() => {
        const loadData = async () => {
            try {
                setLoadingSupportData(true);

                const clientiResponse = await getClientiApi();
                setClienti(sortClientiByName(excludeNonLavoratoClienti(clientiResponse.data ?? [])));

                if (isAdmin) {
                    const utentiResponse = await getUtentiApi();
                    const utentiData = sortUsersByDisplayName(
                        excludeAdminUsers((utentiResponse.data ?? []) as UtenteDto[])
                    );

                    setUtenti(
                        utentiData.map((utente) => ({
                            id: utente.id,
                            label: `${utente.nome} ${utente.cognome}`.trim(),
                        }))
                    );
                } else {
                    const profileResponse = await getMyProfileApi();
                    const profile = (profileResponse.data ?? null) as ProfiloUtenteDto | null;

                    setMyProfile(profile);

                    if (profile?.id) {
                        setFilters((prev) => ({
                            ...prev,
                            utenteId: String(profile.id),
                        }));
                    }
                }
            } catch (error: unknown) {
                toast.error(getErrorMessage(error, 'Errore durante il caricamento dei dati del report'));
            } finally {
                setLoadingSupportData(false);
            }
        };

        void loadData();
    }, [isAdmin]);

    const resetResults = () => {
        setReportCliente(null);
        setReportClienteGiorno(null);
        setReportDipendente(null);
    };

    const handleModeChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        const newMode = event.target.value as ReportMode;

        setMode(newMode);
        resetResults();
        setFilters((prev) => ({
            ...prev,
            clienteId: newMode === 'dipendente' ? '' : prev.clienteId,
            utenteId: newMode === 'dipendente' ? prev.utenteId : '',
            data: newMode === 'cliente-giorno' ? prev.data : '',
        }));
    };

    const handleFilterChange = (
        event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
    ) => {
        const { name, value } = event.target;

        setFilters((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const selectedPeriodoLabel = useMemo(() => {
        if (mode === 'cliente-giorno') {
            return formatDisplayDate(filters.data);
        }

        if (!filters.anno) {
            return 'Periodo non selezionato';
        }

        if (!filters.mese) {
            return `Anno ${filters.anno}`;
        }

        const month = MONTH_OPTIONS.find((item) => String(item.value) === filters.mese);
        return month ? `${month.label} ${filters.anno}` : `Periodo ${filters.mese}/${filters.anno}`;
    }, [filters.anno, filters.data, filters.mese, mode]);

    const handleReset = () => {
        setMode(isAdmin ? 'cliente' : 'dipendente');
        setFilters({
            anno: String(currentYear),
            mese: '',
            clienteId: '',
            utenteId: isAdmin ? '' : (myProfile?.id ? String(myProfile.id) : ''),
            data: '',
        });
        resetResults();
    };

    const handleGenerateReport = async () => {
        try {
            resetResults();

            if (mode === 'cliente-giorno') {
                if (!filters.clienteId) {
                    toast.error('Seleziona un cliente');
                    return;
                }

                if (!filters.data) {
                    toast.error('Seleziona una data');
                    return;
                }

                setLoadingReport(true);

                const response = await getReportClienteGiornoApi({
                    clienteId: Number(filters.clienteId),
                    data: filters.data,
                });

                setReportClienteGiorno(response.data ?? null);
                toast.success(response.message || 'Report cliente giornaliero generato con successo');
                return;
            }

            if (!filters.anno) {
                toast.error("Seleziona almeno l'anno del report");
                return;
            }

            const anno = Number(filters.anno);
            const mese = filters.mese ? Number(filters.mese) : undefined;

            setLoadingReport(true);

            if (mode === 'cliente') {
                if (!filters.clienteId) {
                    toast.error('Seleziona un cliente');
                    return;
                }

                const response = await getReportClienteApi({
                    clienteId: Number(filters.clienteId),
                    anno,
                    mese,
                });

                setReportCliente(response.data ?? null);
                toast.success(response.message || 'Report cliente generato con successo');
                return;
            }

            const effectiveUtenteId = isAdmin
                ? Number(filters.utenteId)
                : myProfile?.id;

            if (!effectiveUtenteId) {
                toast.error('Seleziona un dipendente');
                return;
            }

            const response = await getReportDipendenteApi({
                utenteId: effectiveUtenteId,
                anno,
                mese,
            });

            setReportDipendente(response.data ?? null);
            toast.success(response.message || 'Report dipendente generato con successo');
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante la generazione del report'));
        } finally {
            setLoadingReport(false);
        }
    };

    const handleExportPdf = async () => {
        if (mode !== 'cliente') {
            toast.error('L’export PDF è disponibile solo per il report cliente');
            return;
        }
        if (!filters.clienteId || !filters.anno) {
            toast.error('Per esportare il PDF seleziona cliente e anno');
            return;
        }

        try {
            setExportingPdf(true);
            const { blob, filename } = await exportReportClientePdfApi({
                clienteId: Number(filters.clienteId),
                mese: filters.mese ? Number(filters.mese) : undefined,
                anno: Number(filters.anno),
            });

            const url = window.URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = filename;
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            window.URL.revokeObjectURL(url);

            toast.success('PDF report cliente esportato con successo');
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’esportazione del PDF cliente'));
        } finally {
            setExportingPdf(false);
        }
    };

    return (
        <div>
            <PageHeader
                title="Report"
                subtitle="Analisi aggregata per cliente, dipendente o giornata operativa"
            />

            <div className="form-card" style={{ marginBottom: '1.5rem' }}>
                <div className="form-grid">
                    {isAdmin ? (
                        <div className="form-group">
                            <label htmlFor="mode">Modalita report</label>
                            <select
                                id="mode"
                                value={mode}
                                onChange={handleModeChange}
                                className="form-select"
                                disabled={loadingSupportData || loadingReport}
                            >
                                <option value="cliente">Per cliente</option>
                                <option value="cliente-giorno">Per cliente e giorno</option>
                                <option value="dipendente">Per dipendente</option>
                            </select>
                        </div>
                    ) : null}

                    {mode === 'cliente-giorno' ? (
                        <div className="form-group">
                            <label htmlFor="data">Data</label>
                            <input
                                id="data"
                                name="data"
                                type="date"
                                value={filters.data}
                                onChange={handleFilterChange}
                                disabled={loadingSupportData || loadingReport}
                            />
                        </div>
                    ) : (
                        <>
                            <div className="form-group">
                                <label htmlFor="anno">Anno</label>
                                <input
                                    id="anno"
                                    name="anno"
                                    type="number"
                                    min={2000}
                                    value={filters.anno}
                                    onChange={handleFilterChange}
                                    disabled={loadingSupportData || loadingReport}
                                    placeholder="Es. 2026"
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="mese">Mese</label>
                                <select
                                    id="mese"
                                    name="mese"
                                    value={filters.mese}
                                    onChange={handleFilterChange}
                                    className="form-select"
                                    disabled={loadingSupportData || loadingReport}
                                >
                                    <option value="">Tutto l'anno</option>
                                    {MONTH_OPTIONS.map((month) => (
                                        <option key={month.value} value={month.value}>
                                            {month.label}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </>
                    )}

                    {mode === 'cliente' || mode === 'cliente-giorno' ? (
                        <div className="form-group">
                            <label htmlFor="clienteId">Cliente</label>
                            <select
                                id="clienteId"
                                name="clienteId"
                                value={filters.clienteId}
                                onChange={handleFilterChange}
                                className="form-select"
                                disabled={loadingSupportData || loadingReport}
                            >
                                <option value="">Seleziona cliente</option>
                                {clienti.map((cliente) => (
                                    <option key={cliente.id} value={cliente.id}>
                                        {cliente.nome}
                                    </option>
                                ))}
                            </select>
                        </div>
                    ) : (
                        <div className="form-group">
                            <label htmlFor="utenteId">Dipendente</label>
                            {isAdmin ? (
                                <select
                                    id="utenteId"
                                    name="utenteId"
                                    value={filters.utenteId}
                                    onChange={handleFilterChange}
                                    className="form-select"
                                    disabled={loadingSupportData || loadingReport}
                                >
                                    <option value="">Seleziona dipendente</option>
                                    {utenti.map((utente) => (
                                        <option key={utente.id} value={utente.id}>
                                            {utente.label}
                                        </option>
                                    ))}
                                </select>
                            ) : (
                                <input
                                    id="utenteId"
                                    type="text"
                                    value={myProfile ? `${myProfile.nome} ${myProfile.cognome}`.trim() : ''}
                                    disabled
                                />
                            )}
                        </div>
                    )}
                </div>

                <div className="form-actions">
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={handleReset}
                        disabled={loadingSupportData || loadingReport}
                    >
                        Reset
                    </button>

                    <button
                        type="button"
                        className="primary-button form-submit-button"
                        onClick={handleGenerateReport}
                        disabled={loadingSupportData || loadingReport || exportingPdf}
                    >
                        {loadingReport ? 'Generazione...' : 'Genera report'}
                    </button>

                    {isAdmin && mode === 'cliente' ? (
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={handleExportPdf}
                            disabled={loadingSupportData || loadingReport || exportingPdf}
                        >
                            {exportingPdf ? 'Esportazione...' : 'Esporta PDF'}
                        </button>
                    ) : null}
                </div>
            </div>

            {loadingSupportData ? (
                <div className="module-placeholder">
                    <h2>Caricamento in corso</h2>
                    <p>Sto recuperando i dati necessari per la reportistica.</p>
                </div>
            ) : null}

            {reportCliente ? (
                <div className="table-card" style={{ marginTop: '1.5rem' }}>
                    <PageHeader
                        title={`Report cliente - ${reportCliente.clienteNome}`}
                        subtitle={selectedPeriodoLabel}
                    />

                    <div className="dashboard-grid" style={{ padding: '0 24px 24px 24px' }}>
                        <div className="dashboard-card">
                            <h2>Totale ore</h2>
                            <p>{reportCliente.totaleOre.toFixed(2)} ore</p>
                        </div>

                        <div className="dashboard-card">
                            <h2>Totale costo</h2>
                            <p>{formatCurrency(reportCliente.totaleCosto)}</p>
                        </div>
                    </div>

                    {reportCliente.dettaglioDipendenti.length === 0 ? (
                        <div className="module-placeholder" style={{ margin: '0 24px 24px 24px' }}>
                            <h2>Nessun dato disponibile</h2>
                            <p>Non ci sono righe da mostrare per i filtri selezionati.</p>
                        </div>
                    ) : (
                        <div className="table-wrapper">
                            <table className="app-table">
                                <thead>
                                    <tr>
                                        <th>Dipendente</th>
                                        <th>Totale ore</th>
                                        <th>Totale costo</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {reportCliente.dettaglioDipendenti.map((item) => (
                                        <tr key={item.utenteId}>
                                            <td data-label="Dipendente">{`${item.nome} ${item.cognome}`.trim()}</td>
                                            <td data-label="Totale ore">{item.oreTotali.toFixed(2)} ore</td>
                                            <td data-label="Totale costo">{formatCurrency(item.costoTotale)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            ) : null}

            {reportClienteGiorno ? (
                <div className="table-card" style={{ marginTop: '1.5rem' }}>
                    <PageHeader
                        title={`Report cliente giornaliero - ${reportClienteGiorno.clienteNome}`}
                        subtitle={formatDisplayDate(reportClienteGiorno.data)}
                    />

                    <div className="dashboard-grid" style={{ padding: '0 24px 24px 24px' }}>
                        <div className="dashboard-card">
                            <h2>Totale ore</h2>
                            <p>{reportClienteGiorno.totaleOre.toFixed(2)} ore</p>
                        </div>

                        <div className="dashboard-card">
                            <h2>Totale costo</h2>
                            <p>{formatCurrency(reportClienteGiorno.totaleCosto)}</p>
                        </div>
                    </div>

                    {reportClienteGiorno.dettaglioDipendenti.length === 0 ? (
                        <div className="module-placeholder" style={{ margin: '0 24px 24px 24px' }}>
                            <h2>Nessun dipendente trovato</h2>
                            <p>Nessuno ha lavorato per questo cliente nella data selezionata.</p>
                        </div>
                    ) : (
                        <div className="table-wrapper">
                            <table className="app-table">
                                <thead>
                                    <tr>
                                        <th>Dipendente</th>
                                        <th>Totale ore</th>
                                        <th>Totale costo</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {reportClienteGiorno.dettaglioDipendenti.map((item) => (
                                        <tr key={item.utenteId}>
                                            <td data-label="Dipendente">{`${item.nome} ${item.cognome}`.trim()}</td>
                                            <td data-label="Totale ore">{item.oreTotali.toFixed(2)} ore</td>
                                            <td data-label="Totale costo">{formatCurrency(item.costoTotale)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            ) : null}

            {reportDipendente ? (
                <div className="table-card" style={{ marginTop: '1.5rem' }}>
                    <PageHeader
                        title={`Report dipendente - ${reportDipendente.nome} ${reportDipendente.cognome}`.trim()}
                        subtitle={selectedPeriodoLabel}
                    />

                    <div className="dashboard-grid" style={{ padding: '0 24px 24px 24px' }}>
                        <div className="dashboard-card">
                            <h2>Totale ore</h2>
                            <p>{reportDipendente.totaleOre.toFixed(2)} ore</p>
                        </div>

                        <div className="dashboard-card">
                            <h2>Totale costo</h2>
                            <p>{formatCurrency(reportDipendente.totaleCosto)}</p>
                        </div>
                    </div>

                    {reportDipendente.dettaglioClienti.length === 0 ? (
                        <div className="module-placeholder" style={{ margin: '0 24px 24px 24px' }}>
                            <h2>Nessun dato disponibile</h2>
                            <p>Non ci sono righe da mostrare per i filtri selezionati.</p>
                        </div>
                    ) : (
                        <div className="table-wrapper">
                            <table className="app-table">
                                <thead>
                                    <tr>
                                        <th>Cliente</th>
                                        <th>Totale ore</th>
                                        <th>Totale costo</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {reportDipendente.dettaglioClienti.map((item) => (
                                        <tr key={item.clienteId}>
                                            <td data-label="Cliente">{item.clienteNome}</td>
                                            <td data-label="Totale ore">{item.oreTotali.toFixed(2)} ore</td>
                                            <td data-label="Totale costo">{formatCurrency(item.costoTotale)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            ) : null}

            {!loadingSupportData && !loadingReport && !reportCliente && !reportClienteGiorno && !reportDipendente ? (
                <div className="module-placeholder" style={{ marginTop: '1.5rem' }}>
                    <h2>Nessun report generato</h2>
                    <p>Seleziona i filtri desiderati e genera un report per visualizzare i dati aggregati.</p>
                </div>
            ) : null}
        </div>
    );
}

export default ReportPage;
