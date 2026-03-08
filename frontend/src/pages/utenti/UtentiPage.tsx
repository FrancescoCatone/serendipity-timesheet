import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { deleteUtenteApi, getUtentiApi } from '../../api/utentiApi';
import { getErrorMessage } from '../../utils/error';
import type { UtenteDto } from '../../types/utente';

function UtentiPage() {
    const [utenti, setUtenti] = useState<UtenteDto[]>([]);
    const [count, setCount] = useState(0);
    const [loading, setLoading] = useState(true);
    const [deletingId, setDeletingId] = useState<number | null>(null);
    const [utenteDaEliminare, setUtenteDaEliminare] = useState<UtenteDto | null>(null);

    const loadUtenti = useCallback(async () => {
        try {
            setLoading(true);

            const response = await getUtentiApi();

            setUtenti(response.data ?? []);
            setCount(response.meta?.count ?? response.data?.length ?? 0);
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante il caricamento degli utenti'));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadUtenti();
    }, [loadUtenti]);

    const openDeleteDialog = (utente: UtenteDto) => {
        setUtenteDaEliminare(utente);
    };

    const closeDeleteDialog = () => {
        if (deletingId !== null) {
            return;
        }

        setUtenteDaEliminare(null);
    };

    const confirmDelete = async () => {
        if (!utenteDaEliminare) {
            return;
        }

        try {
            setDeletingId(utenteDaEliminare.id);

            const response = await deleteUtenteApi(utenteDaEliminare.id);
            toast.success(response.message || 'Utente eliminato con successo');

            setUtenteDaEliminare(null);
            await loadUtenti();
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’eliminazione dell’utente'));
        } finally {
            setDeletingId(null);
        }
    };

    return (
        <div>
            <PageHeader
                title="Utenti"
                subtitle={`Totale utenti: ${count}`}
                actions={
                    <div className="page-actions">
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={loadUtenti}
                            disabled={loading}
                        >
                            {loading ? 'Aggiornamento...' : 'Aggiorna'}
                        </button>

                        <Link to="/app/utenti/nuovo" className="action-link-button">
                            Nuovo utente
                        </Link>
                    </div>
                }
            />

            {loading ? (
                <div className="module-placeholder">
                    <h2>Caricamento in corso</h2>
                    <p>Sto recuperando la lista utenti dal backend.</p>
                </div>
            ) : utenti.length === 0 ? (
                <div className="module-placeholder">
                    <h2>Nessun utente trovato</h2>
                    <p>Non ci sono utenti da mostrare al momento.</p>
                </div>
            ) : (
                <div className="table-card">
                    <div className="table-wrapper">
                        <table className="app-table">
                            <thead>
                                <tr>
                                    <th>Codice fiscale</th>
                                    <th>Nome</th>
                                    <th>Cognome</th>
                                    <th>Email</th>
                                    <th>Ruolo</th>
                                    <th>Azioni</th>
                                </tr>
                            </thead>
                            <tbody>
                                {utenti.map((utente) => (
                                    <tr key={utente.id}>
                                        <td>{utente.codiceFiscale}</td>
                                        <td>{utente.nome}</td>
                                        <td>{utente.cognome}</td>
                                        <td>{utente.email}</td>
                                        <td>
                                            <span className="role-badge">{utente.ruolo}</span>
                                        </td>
                                        <td>
                                            <div className="table-actions">
                                                <Link
                                                    to={`/app/utenti/${utente.id}/modifica`}
                                                    className="table-action-button edit"
                                                >
                                                    Modifica
                                                </Link>

                                                <button
                                                    type="button"
                                                    className="table-action-button delete"
                                                    onClick={() => openDeleteDialog(utente)}
                                                    disabled={deletingId === utente.id}
                                                >
                                                    {deletingId === utente.id ? 'Eliminazione...' : 'Elimina'}
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
                open={!!utenteDaEliminare}
                title="Conferma eliminazione"
                message={
                    utenteDaEliminare
                        ? `Sei sicuro di voler eliminare l'utente ${utenteDaEliminare.nome} ${utenteDaEliminare.cognome}?`
                        : ''
                }
                confirmText="Elimina utente"
                cancelText="Annulla"
                loading={deletingId !== null}
                onConfirm={confirmDelete}
                onCancel={closeDeleteDialog}
            />
        </div>
    );
}

export default UtentiPage;