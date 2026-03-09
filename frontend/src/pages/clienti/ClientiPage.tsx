import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { deleteClienteApi, getClientiApi } from '../../api/clientiApi';
import { getErrorMessage } from '../../utils/error';
import type { ClienteDto } from '../../types/cliente';

function formatTariffaOraria(value: number): string {
    return new Intl.NumberFormat('it-IT', {
        style: 'currency',
        currency: 'EUR',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(value);
}

function ClientiPage() {
    const [clienti, setClienti] = useState<ClienteDto[]>([]);
    const [count, setCount] = useState(0);
    const [loading, setLoading] = useState(true);
    const [deletingId, setDeletingId] = useState<number | null>(null);
    const [clienteDaEliminare, setClienteDaEliminare] = useState<ClienteDto | null>(null);

    const loadClienti = useCallback(async () => {
        try {
            setLoading(true);

            const response = await getClientiApi();

            setClienti(response.data ?? []);
            setCount(response.data?.length ?? 0);
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante il caricamento dei clienti'));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadClienti();
    }, [loadClienti]);

    const openDeleteDialog = (cliente: ClienteDto) => {
        setClienteDaEliminare(cliente);
    };

    const closeDeleteDialog = () => {
        if (deletingId !== null) {
            return;
        }

        setClienteDaEliminare(null);
    };

    const confirmDelete = async () => {
        if (!clienteDaEliminare) {
            return;
        }

        try {
            setDeletingId(clienteDaEliminare.id);

            const response = await deleteClienteApi(clienteDaEliminare.id);
            toast.success(response.message || 'Cliente eliminato con successo');

            setClienteDaEliminare(null);
            await loadClienti();
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’eliminazione del cliente'));
        } finally {
            setDeletingId(null);
        }
    };

    return (
        <div>
            <PageHeader
                title="Clienti"
                subtitle={`Totale clienti: ${count}`}
                actions={
                    <div className="page-actions">
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={loadClienti}
                            disabled={loading}
                        >
                            {loading ? 'Aggiornamento...' : 'Aggiorna'}
                        </button>

                        <Link to="/app/clienti/nuovo" className="action-link-button">
                            Nuovo cliente
                        </Link>
                    </div>
                }
            />

            {loading ? (
                <div className="module-placeholder">
                    <h2>Caricamento in corso</h2>
                    <p>Sto recuperando la lista clienti dal backend.</p>
                </div>
            ) : clienti.length === 0 ? (
                <div className="module-placeholder">
                    <h2>Nessun cliente trovato</h2>
                    <p>Non ci sono clienti da mostrare al momento.</p>
                </div>
            ) : (
                <div className="table-card">
                    <div className="table-wrapper">
                        <table className="app-table">
                            <thead>
                                <tr>
                                    <th>Nome</th>
                                    <th>Tariffa oraria</th>
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                {clienti.map((cliente) => (
                                    <tr key={cliente.id}>
                                        <td>{cliente.nome}</td>
                                        <td>{formatTariffaOraria(cliente.tariffaOraria)}</td>
                                        <td>
                                            <div className="table-actions">
                                                <Link
                                                    to={`/app/clienti/${cliente.id}/modifica`}
                                                    className="table-action-button edit"
                                                >
                                                    Modifica
                                                </Link>

                                                <button
                                                    type="button"
                                                    className="table-action-button delete"
                                                    onClick={() => openDeleteDialog(cliente)}
                                                    disabled={deletingId === cliente.id}
                                                >
                                                    {deletingId === cliente.id ? 'Eliminazione...' : 'Elimina'}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            <ConfirmDialog
                open={!!clienteDaEliminare}
                title="Conferma eliminazione"
                message={
                    clienteDaEliminare
                        ? `Sei sicuro di voler eliminare il cliente ${clienteDaEliminare.nome}?`
                        : ''
                }
                confirmText="Elimina cliente"
                cancelText="Annulla"
                loading={deletingId !== null}
                onConfirm={confirmDelete}
                onCancel={closeDeleteDialog}
            />
        </div>
    );
}

export default ClientiPage;