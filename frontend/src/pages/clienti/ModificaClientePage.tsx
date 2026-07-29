import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { getErrorMessage } from '../../utils/error';
import { getClienteByIdApi, updateClienteApi } from '../../api/clientiApi';

function ModificaClientePage() {
    const navigate = useNavigate();
    const { id } = useParams();

    const [form, setForm] = useState({
        nome: '',
        tariffaOraria: '',
    });

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const loadCliente = async () => {
            if (!id || Number.isNaN(Number(id))) {
                toast.error('ID cliente non valido');
                navigate('/app/clienti', { replace: true });
                return;
            }

            try {
                setLoading(true);

                const response = await getClienteByIdApi(Number(id));
                const cliente = response.data;

                if (!cliente) {
                    toast.error('Cliente non trovato');
                    navigate('/app/clienti', { replace: true });
                    return;
                }

                setForm({
                    nome: cliente.nome ?? '',
                    tariffaOraria:
                        typeof cliente.tariffaOraria === 'number'
                            ? String(cliente.tariffaOraria)
                            : '',
                });
            } catch (error: unknown) {
                toast.error(getErrorMessage(error, 'Errore durante il caricamento del cliente'));
                navigate('/app/clienti', { replace: true });
            } finally {
                setLoading(false);
            }
        };

        loadCliente();
    }, [id, navigate]);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.target;

        setForm((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const parseTariffaOraria = (value: string): number => {
        return Number(value.replace(',', '.').trim());
    };

    const validateForm = (): string | null => {
        const nome = form.nome.trim();
        const tariffa = parseTariffaOraria(form.tariffaOraria);

        if (!nome || !form.tariffaOraria.trim()) {
            return 'Compila tutti i campi obbligatori';
        }

        if (nome.length > 100) {
            return 'Il nome del cliente non può superare 100 caratteri';
        }

        if (Number.isNaN(tariffa) || tariffa <= 0) {
            return 'La tariffa oraria deve essere maggiore di 0';
        }

        return null;
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!id || Number.isNaN(Number(id))) {
            toast.error('ID cliente non valido');
            return;
        }

        const validationError = validateForm();
        if (validationError) {
            toast.error(validationError);
            return;
        }

        try {
            setSaving(true);

            const response = await updateClienteApi(Number(id), {
                nome: form.nome.trim(),
                tariffaOraria: parseTariffaOraria(form.tariffaOraria),
            });

            toast.success(response.message || 'Cliente aggiornato con successo');
            navigate('/app/clienti', { replace: true });
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante l’aggiornamento del cliente'));
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div>
                <PageHeader
                    title="Modifica cliente"
                    subtitle="Caricamento dati cliente in corso"
                />
                <div className="module-placeholder">
                    <h2>Caricamento in corso</h2>
                    <p>Sto recuperando i dati del cliente dal backend.</p>
                </div>
            </div>
        );
    }

    return (
        <div>
            <PageHeader
                title="Modifica cliente"
                subtitle="Aggiorna i dati dell’anagrafica cliente"
            />

            <div className="form-card">
                <form onSubmit={handleSubmit} className="entity-form">
                    <div className="form-grid">
                        <div className="form-group">
                            <label htmlFor="nome">Nome</label>
                            <input
                                id="nome"
                                name="nome"
                                type="text"
                                value={form.nome}
                                onChange={handleChange}
                                disabled={saving}
                                placeholder="Inserisci il nome del cliente"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="tariffaOraria">Tariffa oraria</label>
                            <input
                                id="tariffaOraria"
                                name="tariffaOraria"
                                type="number"
                                min="0"
                                step="0.01"
                                value={form.tariffaOraria}
                                onChange={handleChange}
                                disabled={saving}
                                placeholder="Inserisci la tariffa oraria"
                            />
                        </div>
                    </div>

                    <div className="form-actions">
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={() => navigate('/app/clienti')}
                            disabled={saving}
                        >
                            Annulla
                        </button>

                        <button
                            type="submit"
                            className="primary-button form-submit-button"
                            disabled={saving}
                        >
                            {saving ? 'Salvataggio...' : 'Salva modifiche'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default ModificaClientePage;
