import { useEffect, useMemo, useState } from 'react';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { getUtentiApi } from '../../api/utentiApi';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { createAccontoMovimentoApi, deleteAccontoMovimentoApi, getAccontiSummaryApi } from '../../api/accontiApi';
import type { AccontiSummaryDto } from '../../types/acconti';
import type { AccontoMovimentoDto } from '../../types/acconti';
import type { UtenteDto } from '../../types/utente';
import { excludeAdminUsers, sortUsersByDisplayName } from '../../utils/entityFilters';
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

function formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    if (!year || !month || !day) {
        return value;
    }
    return `${day}/${month}/${year}`;
}

function AccontiPage() {
    const today = new Date();
    const [filters, setFilters] = useState({
        utenteId: '',
        mese: String(today.getMonth() + 1),
        anno: String(today.getFullYear()),
    });
    const [form, setForm] = useState({
        importo: '',
        note: '',
        dataMovimento: `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`,
    });
    const [utenti, setUtenti] = useState<UserOption[]>([]);
    const [loadingSupportData, setLoadingSupportData] = useState(true);
    const [loadingSummary, setLoadingSummary] = useState(false);
    const [saving, setSaving] = useState(false);
    const [deletingId, setDeletingId] = useState<number | null>(null);
    const [movimentoDaEliminare, setMovimentoDaEliminare] = useState<AccontoMovimentoDto | null>(null);
    const [summary, setSummary] = useState<AccontiSummaryDto | null>(null);

    useEffect(() => {
        const loadData = async () => {
            try {
                setLoadingSupportData(true);
                const utentiResponse = await getUtentiApi();
                const utentiData = sortUsersByDisplayName(excludeAdminUsers((utentiResponse.data ?? []) as UtenteDto[]));
                setUtenti(
                    utentiData.map((utente) => ({
                        id: utente.id,
                        label: `${utente.nome} ${utente.cognome}`.trim(),
                    }))
                );
            } catch (error: unknown) {
                toast.error(getErrorMessage(error, 'Errore durante il caricamento dei dipendenti'));
            } finally {
                setLoadingSupportData(false);
            }
        };

        void loadData();
    }, []);

    const selectedMonthLabel = useMemo(() => {
        return MONTH_OPTIONS.find((month) => String(month.value) === filters.mese)?.label ?? filters.mese;
    }, [filters.mese]);

    const handleFilterChange = (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = event.target;
        setFilters((prev) => ({ ...prev, [name]: value }));
    };

    const handleFormChange = (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const { name, value } = event.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    };

    const loadSummary = async () => {
        if (!filters.utenteId || !filters.mese || !filters.anno) {
            toast.error('Seleziona dipendente, mese e anno');
            return;
        }

        try {
            setLoadingSummary(true);
            const response = await getAccontiSummaryApi({
                utenteId: Number(filters.utenteId),
                mese: Number(filters.mese),
                anno: Number(filters.anno),
            });
            setSummary(response.data ?? null);
            toast.success(response.message || 'Riepilogo acconti generato');
        } catch (error: unknown) {
            setSummary(null);
            toast.error(getErrorMessage(error, 'Errore durante il caricamento del riepilogo acconti'));
        } finally {
            setLoadingSummary(false);
        }
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!filters.utenteId || !filters.mese || !filters.anno) {
            toast.error('Seleziona prima dipendente, mese e anno');
            return;
        }

        if (!form.importo || Number(form.importo) <= 0) {
            toast.error('Inserisci un importo maggiore di zero');
            return;
        }

        try {
            setSaving(true);
            const response = await createAccontoMovimentoApi({
                utenteId: Number(filters.utenteId),
                mese: Number(filters.mese),
                anno: Number(filters.anno),
                importo: Number(form.importo),
                note: form.note.trim() || undefined,
                dataMovimento: form.dataMovimento,
            });
            toast.success(response.message || 'Movimento registrato');
            setForm((prev) => ({
                ...prev,
                importo: '',
                note: '',
            }));
            await loadSummary();
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante la registrazione del movimento'));
        } finally {
            setSaving(false);
        }
    };

    const openDeleteDialog = (movimento: AccontoMovimentoDto) => {
        setMovimentoDaEliminare(movimento);
    };

    const closeDeleteDialog = () => {
        if (deletingId !== null) {
            return;
        }
        setMovimentoDaEliminare(null);
    };

    const confirmDeleteMovimento = async () => {
        if (!movimentoDaEliminare) {
            return;
        }

        try {
            setDeletingId(movimentoDaEliminare.id);
            const response = await deleteAccontoMovimentoApi(movimentoDaEliminare.id);
            toast.success(response.message || 'Movimento eliminato');
            setMovimentoDaEliminare(null);
            await loadSummary();
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’eliminazione del movimento'));
        } finally {
            setDeletingId(null);
        }
    };

    return (
        <div>
            <PageHeader
                title="Acconti"
                subtitle="Gestione admin-only degli anticipi registrati su un mese e del saldo residuo per dipendente"
            />

            <div className="form-card" style={{ marginBottom: '1.5rem' }}>
                <div className="form-grid">
                    <div className="form-group">
                        <label htmlFor="utenteId">Dipendente</label>
                        <select
                            id="utenteId"
                            name="utenteId"
                            value={filters.utenteId}
                            onChange={handleFilterChange}
                            className="form-select"
                            disabled={loadingSupportData || loadingSummary || saving}
                        >
                            <option value="">Seleziona dipendente</option>
                            {utenti.map((utente) => (
                                <option key={utente.id} value={utente.id}>
                                    {utente.label}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label htmlFor="mese">Mese</label>
                        <select
                            id="mese"
                            name="mese"
                            value={filters.mese}
                            onChange={handleFilterChange}
                            className="form-select"
                            disabled={loadingSupportData || loadingSummary || saving}
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
                            value={filters.anno}
                            onChange={handleFilterChange}
                            disabled={loadingSupportData || loadingSummary || saving}
                        />
                    </div>
                </div>

                <div className="form-actions">
                    <button
                        type="button"
                        className="primary-button form-submit-button"
                        onClick={() => void loadSummary()}
                        disabled={loadingSupportData || loadingSummary || saving}
                    >
                        {loadingSummary ? 'Caricamento...' : 'Carica riepilogo'}
                    </button>
                </div>
            </div>

            <div className="form-card" style={{ marginBottom: '1.5rem' }}>
                <h2 style={{ marginTop: 0 }}>Nuovo movimento</h2>
                <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
                    Puoi registrare o eliminare anticipi del mese selezionato; il riepilogo viene ricalcolato subito.
                </p>

                <form onSubmit={handleSubmit} className="entity-form">
                    <div className="form-grid">
                        <div className="form-group">
                            <label htmlFor="importo">Importo</label>
                            <input
                                id="importo"
                                name="importo"
                                type="number"
                                min={0.01}
                                step={0.01}
                                value={form.importo}
                                onChange={handleFormChange}
                                disabled={saving || loadingSummary}
                                placeholder="Es. 300.00"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="dataMovimento">Data movimento</label>
                            <input
                                id="dataMovimento"
                                name="dataMovimento"
                                type="date"
                                value={form.dataMovimento}
                                onChange={handleFormChange}
                                disabled={saving || loadingSummary}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="note">Note</label>
                            <textarea
                                id="note"
                                name="note"
                                value={form.note}
                                onChange={handleFormChange}
                                disabled={saving || loadingSummary}
                                rows={3}
                                placeholder="Motivo o contesto del movimento"
                            />
                        </div>
                    </div>

                    <div className="form-actions">
                        <button
                            type="submit"
                            className="primary-button form-submit-button"
                            disabled={saving || loadingSummary || loadingSupportData}
                        >
                            {saving ? 'Salvataggio...' : 'Registra movimento'}
                        </button>
                    </div>
                </form>
            </div>

            {summary ? (
                <>
                    <div className="table-card" style={{ marginBottom: '1.5rem' }}>
                        <PageHeader
                            title={`Riepilogo acconti - ${summary.utenteNome} ${summary.utenteCognome}`.trim()}
                            subtitle={`${selectedMonthLabel} ${summary.anno}`}
                        />

                        <div className="dashboard-grid" style={{ padding: '0 24px 24px 24px' }}>
                            <div className="dashboard-card">
                                <h2>Maturato</h2>
                                <p>{formatCurrency(summary.maturato)}</p>
                            </div>

                            <div className="dashboard-card">
                                <h2>Acconti</h2>
                                <p>{formatCurrency(summary.totaleAcconti)}</p>
                            </div>

                            <div className="dashboard-card">
                                <h2>Saldo residuo</h2>
                                <p>{formatCurrency(summary.saldoResiduo)}</p>
                            </div>
                        </div>

                        <div style={{ padding: '0 24px 24px 24px' }}>
                            <p className="page-subtitle" style={{ margin: 0 }}>
                                Stato timesheet del periodo: <strong>{summary.timesheetStato ?? 'ASSENTE'}</strong>
                            </p>
                            <p className="page-subtitle" style={{ marginTop: '0.35rem' }}>
                                Totale movimenti registrati: <strong>{formatCurrency(summary.totaleMovimenti)}</strong>
                            </p>
                        </div>
                    </div>

                    <div className="table-card">
                        <PageHeader
                            title="Storico movimenti"
                            subtitle="Ordinato dal più recente al meno recente"
                        />

                        {summary.movimenti.length === 0 ? (
                            <div className="module-placeholder" style={{ margin: '0 24px 24px 24px' }}>
                                <h2>Nessun acconto registrato</h2>
                                <p>Per il periodo selezionato non ci sono ancora anticipi registrati.</p>
                            </div>
                        ) : (
                            <div className="table-wrapper">
                                <table className="app-table">
                                    <thead>
                                        <tr>
                                            <th>Data</th>
                                            <th>Importo</th>
                                            <th>Note</th>
                                            <th>Registrato il</th>
                                            <th>Azioni</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {summary.movimenti.map((movimento) => (
                                            <tr key={movimento.id}>
                                                <td>{formatDate(movimento.dataMovimento)}</td>
                                                <td>{formatCurrency(movimento.importo)}</td>
                                                <td>{movimento.note || '—'}</td>
                                                <td>{movimento.createdAt.replace('T', ' ').slice(0, 16)}</td>
                                                <td>
                                                    <button
                                                        type="button"
                                                        className="table-action-button delete"
                                                        onClick={() => openDeleteDialog(movimento)}
                                                        disabled={deletingId === movimento.id || saving || loadingSummary}
                                                    >
                                                        {deletingId === movimento.id ? 'Eliminazione...' : 'Elimina'}
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </>
            ) : null}

            {!loadingSupportData && !loadingSummary && !summary ? (
                <div className="module-placeholder">
                    <h2>Nessun riepilogo caricato</h2>
                    <p>Seleziona dipendente, mese e anno per vedere maturato, movimenti e saldo residuo.</p>
                </div>
            ) : null}

            <ConfirmDialog
                open={!!movimentoDaEliminare}
                title="Conferma eliminazione"
                message={
                    movimentoDaEliminare
                        ? `Vuoi eliminare l'acconto del ${formatDate(movimentoDaEliminare.dataMovimento)} da ${formatCurrency(movimentoDaEliminare.importo)}?`
                        : ''
                }
                confirmText="Elimina acconto"
                cancelText="Annulla"
                loading={deletingId !== null}
                onConfirm={confirmDeleteMovimento}
                onCancel={closeDeleteDialog}
            />
        </div>
    );
}

export default AccontiPage;
