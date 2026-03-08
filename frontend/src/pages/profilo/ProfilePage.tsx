import { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { changeMyPasswordApi, getMyProfileApi } from '../../api/utentiApi';
import { getErrorMessage } from '../../utils/error';
import type { ProfiloUtenteDto } from '../../types/utente';

function ProfilePage() {
    const [profile, setProfile] = useState<ProfiloUtenteDto | null>(null);
    const [loadingProfile, setLoadingProfile] = useState(true);

    const [form, setForm] = useState({
        oldPassword: '',
        newPassword: '',
        confirmNewPassword: '',
    });

    const [loadingPassword, setLoadingPassword] = useState(false);

    useEffect(() => {
        const loadProfile = async () => {
            try {
                setLoadingProfile(true);
                const response = await getMyProfileApi();
                setProfile(response.data ?? null);
            } catch (error: unknown) {
                toast.error(getErrorMessage(error, 'Errore durante il caricamento del profilo'));
            } finally {
                setLoadingProfile(false);
            }
        };

        loadProfile();
    }, []);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.target;

        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const validateForm = (): string | null => {
        if (!form.oldPassword.trim() || !form.newPassword.trim() || !form.confirmNewPassword.trim()) {
            return 'Compila tutti i campi della password';
        }

        if (form.newPassword.length < 6) {
            return 'La nuova password deve contenere almeno 6 caratteri';
        }

        if (form.newPassword !== form.confirmNewPassword) {
            return 'La conferma della nuova password non coincide';
        }

        if (form.oldPassword === form.newPassword) {
            return 'La nuova password deve essere diversa da quella attuale';
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
            setLoadingPassword(true);

            const response = await changeMyPasswordApi({
                oldPassword: form.oldPassword,
                newPassword: form.newPassword,
            });

            toast.success(response.message || 'Password aggiornata con successo');

            setForm({
                oldPassword: '',
                newPassword: '',
                confirmNewPassword: '',
            });
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’aggiornamento della password'));
        } finally {
            setLoadingPassword(false);
        }
    };

    return (
        <div>
            <PageHeader
                title="Profilo"
                subtitle="Consulta i tuoi dati e aggiorna la password del tuo account"
            />

            <div className="profile-grid">
                <div className="profile-card">
                    <h2 className="section-title">Riepilogo account</h2>

                    {loadingProfile ? (
                        <p className="profile-note">Caricamento profilo in corso...</p>
                    ) : !profile ? (
                        <p className="profile-note">Profilo non disponibile.</p>
                    ) : (
                        <div className="profile-info-list">
                            <div className="profile-info-item">
                                <span className="profile-info-label">Nome</span>
                                <span className="profile-info-value">{profile.nome}</span>
                            </div>

                            <div className="profile-info-item">
                                <span className="profile-info-label">Cognome</span>
                                <span className="profile-info-value">{profile.cognome}</span>
                            </div>

                            <div className="profile-info-item">
                                <span className="profile-info-label">Email</span>
                                <span className="profile-info-value">{profile.email}</span>
                            </div>

                            <div className="profile-info-item">
                                <span className="profile-info-label">Codice fiscale</span>
                                <span className="profile-info-value">{profile.codiceFiscale}</span>
                            </div>

                            <div className="profile-info-item">
                                <span className="profile-info-label">Ruolo</span>
                                <span className="profile-info-value">{profile.ruolo}</span>
                            </div>
                        </div>
                    )}
                </div>

                <div className="profile-card">
                    <h2 className="section-title">Aggiorna password</h2>

                    <form onSubmit={handleSubmit} className="entity-form">
                        <div className="form-group">
                            <label htmlFor="oldPassword">Password attuale</label>
                            <input
                                id="oldPassword"
                                name="oldPassword"
                                type="password"
                                value={form.oldPassword}
                                onChange={handleChange}
                                disabled={loadingPassword}
                                placeholder="Inserisci la password attuale"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="newPassword">Nuova password</label>
                            <input
                                id="newPassword"
                                name="newPassword"
                                type="password"
                                value={form.newPassword}
                                onChange={handleChange}
                                disabled={loadingPassword}
                                placeholder="Inserisci la nuova password"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="confirmNewPassword">Conferma nuova password</label>
                            <input
                                id="confirmNewPassword"
                                name="confirmNewPassword"
                                type="password"
                                value={form.confirmNewPassword}
                                onChange={handleChange}
                                disabled={loadingPassword}
                                placeholder="Conferma la nuova password"
                            />
                        </div>

                        <div className="form-actions">
                            <button
                                type="submit"
                                className="primary-button form-submit-button"
                                disabled={loadingPassword}
                            >
                                {loadingPassword ? 'Aggiornamento...' : 'Aggiorna password'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default ProfilePage;