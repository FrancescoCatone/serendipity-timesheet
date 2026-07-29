import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { createClienteApi } from '../../api/clientiApi';
import { getErrorMessage } from '../../utils/error';

function NuovoClientePage() {
    const navigate = useNavigate();

    const [form, setForm] = useState({
        nome: '',
        tariffaOraria: '',
    });

    const [loading, setLoading] = useState(false);

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

        const validationError = validateForm();
        if (validationError) {
            toast.error(validationError);
            return;
        }

        try {
            setLoading(true);

            const response = await createClienteApi({
                nome: form.nome.trim(),
                tariffaOraria: parseTariffaOraria(form.tariffaOraria),
            });

            toast.success(response.message || 'Cliente creato con successo');
            navigate('/app/clienti', { replace: true });
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Errore durante la creazione del cliente'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <PageHeader
                title="Nuovo cliente"
                subtitle="Compila i dati per creare una nuova anagrafica cliente"
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
                                disabled={loading}
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
                                disabled={loading}
                                placeholder="Inserisci la tariffa oraria"
                            />
                        </div>
                    </div>

                    <div className="form-actions">
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={() => navigate('/app/clienti')}
                            disabled={loading}
                        >
                            Annulla
                        </button>

                        <button
                            type="submit"
                            className="primary-button form-submit-button"
                            disabled={loading}
                        >
                            {loading ? 'Salvataggio...' : 'Crea cliente'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default NuovoClientePage;
