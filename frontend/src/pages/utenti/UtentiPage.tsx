import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { deleteUtenteApi, getUtentiApi } from '../../api/utentiApi';
import { getCurrentUserProfile } from '../../utils/auth';
import { getErrorMessage } from '../../utils/error';
import { isSystemOperatorEmail } from '../../utils/systemUsers';
import type { UtenteDto } from '../../types/utente';

function sortUtentiWithAdminsFirst(items: UtenteDto[]): UtenteDto[] {
    return [...items].sort((a, b) => {
        const aIsAdmin = a.ruolo === 'ADMIN';
        const bIsAdmin = b.ruolo === 'ADMIN';

        if (aIsAdmin !== bIsAdmin) {
            return aIsAdmin ? -1 : 1;
        }

        const byCognome = a.cognome.localeCompare(b.cognome, 'it', { sensitivity: 'base' });
        if (byCognome !== 0) {
            return byCognome;
        }

        const byNome = a.nome.localeCompare(b.nome, 'it', { sensitivity: 'base' });
        if (byNome !== 0) {
            return byNome;
        }

        return a.email.localeCompare(b.email, 'it', { sensitivity: 'base' });
    });
}

function UtentiPage() {
    const currentUserEmail = getCurrentUserProfile().email;
    const [utenti, setUtenti] = useState<UtenteDto[]>([]);
    const [count, setCount] = useState(0);
    const [loading, setLoading] = useState(true);
    const [deletingId, setDeletingId] = useState<number | null>(null);
    const [utenteDaEliminare, setUtenteDaEliminare] = useState<UtenteDto | null>(null);

    const loadUtenti = useCallback(async () => {
        try {
            setLoading(true);

            const response = await getUtentiApi();
            const utentiOrdinati = sortUtentiWithAdminsFirst(response.data ?? []);

            setUtenti(utentiOrdinati);
            setCount(response.meta?.count ?? utentiOrdinati.length ?? 0);
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

    const canManageUtente = (utente: UtenteDto): boolean => {
        if (!isSystemOperatorEmail(utente.email)) {
            return true;
        }

        return isSystemOperatorEmail(currentUserEmail);
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
                                                {canManageUtente(utente) ? (
                                                    <>
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
                                                    </>
                                                ) : (
                                                    <span className="page-subtitle">Account protetto</span>
                                                )}
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
